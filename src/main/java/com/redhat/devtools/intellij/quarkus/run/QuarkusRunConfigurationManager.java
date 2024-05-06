/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.quarkus.run;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.dashboard.RunDashboardManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.quarkus.QuarkusPluginDisposable;
import com.redhat.devtools.intellij.quarkus.buildtool.BuildToolDelegate;
import com.redhat.devtools.intellij.quarkus.buildtool.ProjectImportListener;
import com.redhat.devtools.intellij.quarkus.settings.UserDefinedQuarkusSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Quarkus run configuration manager used to:
 *
 * <ul>
 *     <li>auto create Quarkus run configuration when a Quarkus Maven/Gradle is imported</li>
 *     <li>provides methods to find existing/ create Quarkus run configuration from a given module</li>
 * </ul>
 */
public class QuarkusRunConfigurationManager implements Disposable {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusRunConfigurationManager.class);

    public static QuarkusRunConfigurationManager getInstance(Project project) {
        return project.getService(QuarkusRunConfigurationManager.class);
    }

    private final Project project;
    private final MessageBusConnection connection;

    public QuarkusRunConfigurationManager(Project project) {
        this.project = project;
        connection = addProjectImportListener(project);
    }

    public @Nullable RunnerAndConfigurationSettings findExistingConfigurationFor(@NotNull Module module) {
        List<RunnerAndConfigurationSettings> configurations = RunManager.getInstance(project).getConfigurationSettingsList(QuarkusRunConfigurationType.class);
        if (!configurations.isEmpty()) {
            for (RunnerAndConfigurationSettings settings : configurations) {
                QuarkusRunConfiguration configuration = (QuarkusRunConfiguration) settings.getConfiguration();
                if (module.equals(configuration.getModule())) {
                    return settings;
                }
            }
        }
        return null;
    }

    public @NotNull RunnerAndConfigurationSettings createConfiguration(@NotNull Module module, boolean save) {
        var runManager = RunManager.getInstance(module.getProject());
        RunnerAndConfigurationSettings quarkusSettings = runManager.createConfiguration(module.getName(), QuarkusRunConfigurationType.class);
        ((QuarkusRunConfiguration) quarkusSettings.getConfiguration()).setModule(module);
        // Ensure that configuration name is unique
        runManager.setUniqueNameIfNeeded(quarkusSettings.getConfiguration());
        if (save) {
            quarkusSettings.storeInLocalWorkspace();
            // Save the configuration
            runManager.addConfiguration(quarkusSettings);
        }
        if (runManager.getAllSettings().size() == 1) {
            // Select the Quarkus configuration on the top of the Run configuration list
            runManager.setSelectedConfiguration(quarkusSettings);
        }
        return quarkusSettings;
    }

    @NotNull
    private MessageBusConnection addProjectImportListener(Project project) {
        MessageBusConnection connection = project.getMessageBus().connect(QuarkusPluginDisposable.getInstance(project));
        ProjectImportListener listener = new ProjectImportListener() {

            @Override
            public void importFinished(@NotNull List<Module> modules) {
                if (!UserDefinedQuarkusSettings.getInstance(project).isCreateQuarkusRunConfigurationOnProjectImport()) {
                    return;
                }
                tryToCreateRunConfigurations(modules);
            }
        };
        BuildToolDelegate[] delegates = BuildToolDelegate.getDelegates();
        for (BuildToolDelegate delegate : delegates) {
            delegate.addProjectImportListener(project, connection, listener);
        }
        return connection;
    }

    private void tryToCreateRunConfigurations(List<Module> modules) {
        if (modules.isEmpty()) {
            return;
        }

        for (Module module : modules) {
            tryToCreateRunConfiguration(module);
        }
    }

    private boolean tryToCreateRunConfiguration(Module module) {
        if (!QuarkusModuleUtil.isQuarkusModule(module)) {
            return false;
        }
        // Check if it exists a Quarkus run configuration for the given module
        // and if it doesn't exist, create it.
        // This process is "synchronized" to avoid creating several Quarkus run configuration for the same module
        // when tryToCreateRunConfiguration is called in same time.
        return createRunConfigurationIfNeeded(module);
    }

    private synchronized boolean createRunConfigurationIfNeeded(Module module) {
        // Find existing Quarkus run configuration
        RunnerAndConfigurationSettings quarkusSettings = findExistingConfigurationFor(module);
        if (quarkusSettings == null) {
            // No Quarkus run configuration for the module, create it and save it in the .idea/workspace.xml file
            createConfiguration(module, true);
            return true;
        }
        return false;
    }

    /**
     * Add "QuarkusRunConfigurationType" in the Services view settings to show "Quarkus Dev Mode" and their Quarkus run configuration in the Services view.
     *
     * @param logError true if error must be logged while updating Services view settings.
     * @return true if the update is done correctly and false otherwise.
     */
    private boolean addQuarkusRunConfigurationTypeInServicesViewIfNeeded(boolean logError) {
        try {
            RunDashboardManager runDashboardManager = RunDashboardManager.getInstance(project);
            Set<String> types = new HashSet<>(runDashboardManager.getTypes());
            if (!types.contains(QuarkusRunConfigurationType.ID)) {
                types.add(QuarkusRunConfigurationType.ID);
                runDashboardManager.setTypes(types);
            }
            return true;
        } catch (Exception e) {
            // This case comes from when Ultimate is used and Ultimate Quarkus support update in same time their Quarkus Configuration Type.
            // java.util.ConcurrentModificationException
            //	at java.base/java.util.HashMap$HashIterator.nextNode(HashMap.java:1597)
            //	at java.base/java.util.HashMap$KeyIterator.next(HashMap.java:1620)
            //	at com.google.common.collect.Sets$3$1.computeNext(Sets.java:907)
            //	at com.google.common.collect.AbstractIterator.tryToComputeNext(AbstractIterator.java:145)
            //	at com.google.common.collect.AbstractIterator.hasNext(AbstractIterator.java:140)
            //	at java.base/java.util.AbstractCollection.addAll(AbstractCollection.java:335)
            //	at java.base/java.util.HashSet.<init>(HashSet.java:121)
            //	at com.intellij.execution.dashboard.RunDashboardManagerImpl.setTypes(RunDashboardManagerImpl.java:295)
            //	at
            if (logError) {
                LOGGER.error("Error while adding QuarkusRunConfigurationType in Services view settings", e);
            }
            return false;
        }
    }

    @Override
    public void dispose() {
        connection.disconnect();
    }
}
