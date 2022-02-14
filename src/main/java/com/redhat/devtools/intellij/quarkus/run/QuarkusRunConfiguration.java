/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.run;

import com.intellij.execution.DefaultExecutionTarget;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.quarkus.TelemetryService;
import com.redhat.devtools.intellij.quarkus.tool.ToolDelegate;
import com.redhat.devtools.intellij.telemetry.core.service.TelemetryMessageBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

public class QuarkusRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule, QuarkusRunConfigurationOptions> {
    public QuarkusRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(name, getRunConfigurationModule(project), factory);
    }

    @NotNull
    private static RunConfigurationModule getRunConfigurationModule(Project project) {
        RunConfigurationModule module = new RunConfigurationModule(project);
        module.setModuleToAnyFirstIfNotSpecified();
        return module;
    }

    @Override
    public Collection<Module> getValidModules() {
        return getAllModules();
    }

    @NotNull
    @Override
    protected QuarkusRunConfigurationOptions getOptions() {
        return (QuarkusRunConfigurationOptions) super.getOptions();
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        if (getModule() == null) {
            throw new RuntimeConfigurationException("No module selected", "Quarkus configuration");
        }
        Module module = getModule();
        if (!QuarkusModuleUtil.isQuarkusModule(module)) {
            throw new RuntimeConfigurationException("Not a Quarkus module", "Quarkus configuration");
        }
        ToolDelegate delegate = ToolDelegate.getDelegate(module);
        if (delegate == null) {
            throw new RuntimeConfigurationException("Can't find a tool to process the module", "Quarkus configuration");
        }
    }

    public Module getModule() {
        return getConfigurationModule().getModule();
    }

    public void setModule(Module module) {
        getConfigurationModule().setModule(module);
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new QuarkusRunSettingsEditor(getProject());
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        TelemetryMessageBuilder.ActionMessage telemetry = TelemetryService.instance().action(TelemetryService.RUN_PREFIX + "run");
        telemetry.property("kind", executor.getId());
        ToolDelegate toolDelegate = ToolDelegate.getDelegate(getModule());
        if (toolDelegate != null) {
            telemetry.property("tool", toolDelegate.getDisplay());
            RunnerAndConfigurationSettings settings = toolDelegate.getConfigurationDelegate(getModule(), this);
            if (settings != null) {
                long groupId = ExecutionEnvironment.getNextUnusedExecutionId();
                ExecutionUtil.runConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance(), DefaultExecutionTarget.INSTANCE, groupId);
            }
        } else {
            telemetry.property("tool", "not found");
        }
        telemetry.send();
        return null;
    }

    public String getProfile() {
        return getOptions().getProfile();
    }

    public void setProfile(String profile) {
        getOptions().setProfile(profile);
    }

    @Override
    public boolean isBuildBeforeLaunchAddedByDefault() {
        return false;
    }

    public Map<String, String> getEnv() {
        return getOptions().getEnv();
    }

    public void setEnv(Map<String, String> env) {
        getOptions().setEnv(env);
    }
}
