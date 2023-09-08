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
package com.redhat.devtools.intellij.qute.psi.core.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.FakePsiElement;
import com.redhat.devtools.intellij.lsp4ij.commands.CommandExecutor;
import com.redhat.devtools.intellij.lsp4ij.inspections.AbstractDelegateInspectionWithExclusions;
import com.redhat.devtools.intellij.qute.psi.core.inspections.QuteGlobalInspection;
import org.eclipse.lsp4j.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuteUpdateConfigurationAction extends AnAction {
    private final Map<String, ConfigurationUpdater> updaters = new HashMap<>();

    public QuteUpdateConfigurationAction() {
        //TODO potentially load those from an extension point?
        updaters.put("qute.validation.enabled", new InspectionConfigurationEnabler(QuteGlobalInspection.ID));
        updaters.put("qute.validation.excluded", new InspectionConfigurationUpdater(QuteGlobalInspection.ID));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        JsonObject configUpdate = getConfigUpdate(e);
        if (configUpdate != null && e.getProject() != null) {
            String section = configUpdate.get("section").getAsString();
            ConfigurationUpdater updater = updaters.get(section);
            if (updater == null) {
                throw new UnsupportedOperationException("Updating "+section+" is not supported yet!");
            }
            JsonElement value = configUpdate.get("value");
            updater.updateConfiguration(e.getProject(), value);
        }
    }

    private @Nullable JsonObject getConfigUpdate(@NotNull AnActionEvent e) {
        @Nullable Command command = e.getData(CommandExecutor.LSP_COMMAND);
        if (command == null) {
            return null;
        }
        List<Object> arguments = command.getArguments();
        if (arguments != null && !arguments.isEmpty()) {
            Object arg = arguments.get(0);
            if (arg instanceof JsonObject) {
                return (JsonObject) arg;
            }
        }
        return null;
    }

    interface ConfigurationUpdater {
        void updateConfiguration(Project project,  JsonElement value);
    }

    private static class InspectionConfigurationUpdater implements ConfigurationUpdater {

        private final String inspectionId;

        InspectionConfigurationUpdater(String inspectionId) {
            this.inspectionId = inspectionId;
        }

        @Override
        public void updateConfiguration(Project project, JsonElement value) {
            if (value != null) {
                updateConfiguration(project, value.getAsString());
            }
        }

        private void updateConfiguration(Project project,  @NotNull String value) {
            InspectionProfile profile = InspectionProfileManager.getInstance(project).getCurrentProfile();
            InspectionToolWrapper<?, ?> toolWrapper = profile.getInspectionTool(inspectionId, project);
            if (toolWrapper != null && toolWrapper.getTool() instanceof AbstractDelegateInspectionWithExclusions) {
                Key<AbstractDelegateInspectionWithExclusions> key = new Key<>(inspectionId);
                profile.modifyToolSettings(key, getPsiElement(project), (tool) -> {
                    tool.excludeList.add(value);
                });
            }
        }

        private PsiElement getPsiElement(Project project) {
            return new FakePsiElement() {
                @Override
                public PsiElement getParent() {
                    return null;
                }

                @Override
                public @NotNull Project getProject() {
                    return project;
                }
            };
        }
    }

    private static class InspectionConfigurationEnabler implements ConfigurationUpdater {

        private final String inspectionId;

        InspectionConfigurationEnabler(String inspectionId) {
            this.inspectionId = inspectionId;
        }

        @Override
        public void updateConfiguration(Project project, JsonElement value) {
            if (value != null) {
                toggleInspection(project, Boolean.valueOf(value.getAsString()));//safer than value.getAsBoolean()
            }
        }

        private void toggleInspection(Project project,  @NotNull boolean enabled) {
            InspectionProfile profile = InspectionProfileManager.getInstance(project).getCurrentProfile();
            if (profile instanceof InspectionProfileImpl) {
                InspectionProfileImpl profileImpl = (InspectionProfileImpl) profile;
                profileImpl.setToolEnabled(inspectionId, enabled);
            }
        }
    }
}
