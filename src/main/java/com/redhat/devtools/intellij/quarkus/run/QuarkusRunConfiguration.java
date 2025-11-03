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

import com.intellij.execution.*;
import com.intellij.execution.configurations.*;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.util.net.NetUtils;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.quarkus.buildtool.BuildToolDelegate;
import com.redhat.devtools.intellij.quarkus.telemetry.TelemetryEventName;
import com.redhat.devtools.intellij.quarkus.telemetry.TelemetryManager;
import com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.intellij.execution.runners.ExecutionUtil.createEnvironment;

/**
 * Quarkus run configration which wraps Maven / Gradle configuration.
 */
public class QuarkusRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule, QuarkusRunConfigurationOptions>
        implements RunConfigurationWithSuppressedDefaultRunAction, RunConfigurationWithSuppressedDefaultDebugAction {

    private final static Logger LOGGER = LoggerFactory.getLogger(QuarkusRunConfiguration.class);

    static final String QUARKUS_CONFIGURATION = "Quarkus Configuration";

    private static final int DEFAULT_PORT = 5005;

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
            throw new RuntimeConfigurationException("No module selected", QUARKUS_CONFIGURATION);
        }
        Module module = getModule();
        if (!QuarkusModuleUtil.isQuarkusModule(module)) {
            throw new RuntimeConfigurationException("Not a Quarkus module", QUARKUS_CONFIGURATION);
        }
        BuildToolDelegate delegate = BuildToolDelegate.getDelegate(module);
        if (delegate == null) {
            throw new RuntimeConfigurationException("Can't find a tool to process the module", QUARKUS_CONFIGURATION);
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

    private int allocateLocalPort() {
        try {
            return NetUtils.findAvailableSocketPort();
        } catch (IOException e) {
            LOGGER.warn("Unexpected I/O exception occurred on attempt to find a free port to use for external system task debugging", e);
        }
        return DEFAULT_PORT;
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        Module module = getModule();

        Map<String, String> telemetryData = new HashMap<>();
        telemetryData.put("kind", executor.getId());

        BuildToolDelegate toolDelegate = BuildToolDelegate.getDelegate(module);
        RunProfileState state = null;
        if (toolDelegate != null) {
            telemetryData.put("tool", toolDelegate.getDisplay());
            boolean debug = DefaultDebugExecutor.EXECUTOR_ID.equals(executor.getId());
            Integer debugPort = debug ? allocateLocalPort() : null;
            Integer quteDebugPort = debug && isQuteDebuggerInstalled(module) ? allocateLocalPort() : null;
            // The parameter (run/debug) executor is filled according the run/debug action
            // but in the case of Gradle, the executor must be only the run executor
            // otherwise for some reason, the stop button will stop the task without stopping the Quarkus application process.
            // Here we need to override the executor if Gradle is started in debug mode.
            Executor overridedExecutor = toolDelegate.getOverridedExecutor();
            executor = overridedExecutor != null ? overridedExecutor : executor;
            // Create a Gradle or Maven run configuration in memory
            RunnerAndConfigurationSettings settings = toolDelegate.getConfigurationDelegate(module, this, debugPort, quteDebugPort);
            if (settings != null) {
                QuarkusRunConfigurationManager.getInstance(module.getProject()); // to be sure that Quarkus execution listener is registered
                long groupId = ExecutionEnvironment.getNextUnusedExecutionId();
                state = doRunConfiguration(settings, executor, DefaultExecutionTarget.INSTANCE, groupId);
            }
        } else {
            telemetryData.put("tool", "not found");
        }
        // Send "run-run" telemetry event
        TelemetryManager.instance().send(TelemetryEventName.RUN_RUN, telemetryData);
        return state;
    }


    private boolean isQuteDebuggerInstalled(@NotNull Module module) {
        // Qute debugger is available since Quarkus 3.29.
        // We check if "io.quarkus.qute.runtime.debug.DebugQuteEngineObserver" is in the classpath
        // Note: we cannot check if "io.quarkus.qute.debug.adapter.RegisterDebugServerAdapter" is in classpath
        // because "io.quarkus.qute.debug.adapter.RegisterDebugServerAdapter" is not available in the standard IJ classpath
        return PsiTypeUtils.findType("io.quarkus.qute.runtime.debug.DebugQuteEngineObserver", module, null) != null;
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

    private static RunProfileState doRunConfiguration(@NotNull RunnerAndConfigurationSettings configuration,
                                                      @NotNull Executor executor,
                                                      @Nullable ExecutionTarget targetOrNullForDefault,
                                                      @Nullable Long executionId) throws ExecutionException {
        ExecutionEnvironmentBuilder builder = createEnvironment(executor, configuration);
        if (builder == null) {
            return null;
        }
        if (targetOrNullForDefault != null) {
            builder.target(targetOrNullForDefault);
        } else {
            builder.activeTarget();
        }
        if (executionId != null) {
            builder.executionId(executionId);
        }
        return configuration.getConfiguration().getState(executor, builder.build());
    }

}
