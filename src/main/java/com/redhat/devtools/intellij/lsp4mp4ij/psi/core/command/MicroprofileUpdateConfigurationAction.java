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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.FakePsiElement;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.inspections.MicroProfilePropertiesUnassignedInspection;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.inspections.MicroProfilePropertiesUnknownInspection;
import com.redhat.devtools.lsp4ij.commands.LSPCommandAction;
import com.redhat.devtools.lsp4ij.inspections.AbstractDelegateInspectionWithExclusions;
import org.eclipse.lsp4j.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Action for updating the Microprofile configuration, typically requested by LSP4MP.
 */
public class MicroprofileUpdateConfigurationAction extends LSPCommandAction {
    private final Map<String, ConfigurationUpdater> updaters = new HashMap<>();

    public MicroprofileUpdateConfigurationAction() {
        updaters.put("microprofile.tools.validation.unknown.excluded", new InspectionConfigurationUpdater(MicroProfilePropertiesUnknownInspection.ID));
        updaters.put("microprofile.tools.validation.unassigned.excluded", new InspectionConfigurationUpdater(MicroProfilePropertiesUnassignedInspection.ID));
    }

    @Override
    protected void commandPerformed(@NotNull Command command, @NotNull AnActionEvent e) {
        JsonObject configUpdate = getConfigUpdate(command);
        if (configUpdate != null && e.getProject() != null) {
            String section = configUpdate.get("section").getAsString();
            ConfigurationUpdater updater = updaters.get(section);
            if (updater == null) {
                throw new UnsupportedOperationException("Updating " + section + " is not supported yet!");
            }
            JsonElement value = configUpdate.get("value");
            updater.updateConfiguration(e.getProject(), value);
        }
    }

    private @Nullable JsonObject getConfigUpdate(@NotNull Command command) {
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
        void updateConfiguration(Project project, JsonElement value);
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

        private void updateConfiguration(Project project, @NotNull String value) {
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
}
