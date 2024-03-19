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
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.FakePsiElement;
import com.redhat.devtools.intellij.qute.psi.core.inspections.QuteGlobalInspection;
import com.redhat.devtools.intellij.qute.psi.core.inspections.QuteUndefinedObjectInspection;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.commands.LSPCommand;
import com.redhat.devtools.lsp4ij.commands.LSPCommandAction;
import com.redhat.devtools.lsp4ij.inspections.AbstractDelegateInspectionWithExclusions;
import com.redhat.devtools.lsp4ij.features.diagnostics.SeverityMapping;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuteUpdateConfigurationAction extends LSPCommandAction {
    private final Map<String, ConfigurationUpdater> updaters = new HashMap<>();

    public QuteUpdateConfigurationAction() {
        updaters.put("qute.validation.enabled", new InspectionEnabler(QuteGlobalInspection.ID));
        updaters.put("qute.validation.excluded", new InspectionExclusionsUpdater(QuteGlobalInspection.ID));
        updaters.put("qute.validation.undefinedObject.severity", new InspectionSeverityUpdater(QuteUndefinedObjectInspection.ID));
    }

    @Override
    protected void commandPerformed(@NotNull LSPCommand command, @NotNull AnActionEvent e) {
        JsonObject configUpdate = getConfigUpdate(command);
        if (configUpdate != null && e.getProject() != null) {
            String section = configUpdate.get("section").getAsString();
            ConfigurationUpdater updater = updaters.get(section);
            if (updater == null) {
                throw new UnsupportedOperationException("Updating '" + section + "' is not supported yet!");
            }
            updater.updateConfiguration(e.getProject(), configUpdate);
        }
    }

    private @Nullable JsonObject getConfigUpdate(@NotNull LSPCommand command) {
        Object arg = command.getArgumentAt(0);
        if (arg instanceof JsonObject) {
            return (JsonObject) arg;
        }
        return null;
    }

    interface ConfigurationUpdater {
        void updateConfiguration(Project project, JsonObject value);
    }

    private static class InspectionSeverityUpdater implements ConfigurationUpdater {
        protected final String inspectionId;

        InspectionSeverityUpdater(String inspectionId) {
            this.inspectionId = inspectionId;
        }

        @Override
        public void updateConfiguration(Project project, JsonObject configUpdate) {
            JsonElement value = configUpdate.get("value");
            if (value == null) {
                return;
            }
            String severity = value.getAsString();
            if ("ignore".equals(severity)) {
                disableInspection(project);
            } else {
                setSeverity(project, DiagnosticSeverity.valueOf(severity));
            }
        }

        private void disableInspection(Project project) {
            InspectionProfileImpl profile = InspectionProfileManager.getInstance(project).getCurrentProfile();
            profile.setToolEnabled(inspectionId, false, project);
        }

        private void setSeverity(Project project, DiagnosticSeverity severity) {
            InspectionProfileImpl profile = InspectionProfileManager.getInstance(project).getCurrentProfile();
            HighlightDisplayLevel level = HighlightDisplayLevel.find(SeverityMapping.toHighlightSeverity(severity));
            profile.setErrorLevel(HighlightDisplayKey.find(inspectionId), level, project);
        }
    }

    private static class InspectionExclusionsUpdater extends InspectionSeverityUpdater {

        InspectionExclusionsUpdater(String inspectionId) {
            super(inspectionId);
        }

        @Override
        public void updateConfiguration(Project project, JsonObject configUpdate) {
            JsonElement value = configUpdate.get("value");
            if (value != null) {
                addToExclusions(project, value.getAsString());
            }
        }

        protected void addToExclusions(Project project, @NotNull String value) {
            InspectionProfile profile = InspectionProfileManager.getInstance(project).getCurrentProfile();
            InspectionToolWrapper<?, ?> toolWrapper = profile.getInspectionTool(inspectionId, project);
            if (toolWrapper != null && toolWrapper.getTool() instanceof AbstractDelegateInspectionWithExclusions) {
                Key<AbstractDelegateInspectionWithExclusions> key = new Key<>(inspectionId);
                profile.modifyToolSettings(key, getPsiElement(project), (tool) -> {
                    tool.excludeList.add(sanitize(project, value));
                });
            }
        }

        /**
         * returns file value path relative to the given project
         *
         * @param project the reference project
         * @param value   the file path URI to get a relative path from
         * @return file value path relative to the given project
         */
        private String sanitize(@NotNull Project project, @NotNull String value) {
            if (value.startsWith("file:/")) {
                try {
                    URI projectURI = LSPIJUtils.toUri(project);
                    URI fileURI = new URI(value);
                    return projectURI.relativize(fileURI).toString();
                } catch (URISyntaxException ignore) {
                }
            }
            return value;
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

    /**
     * Simulates a module-wide validation disabling by adding <module-path>/** to exclusions
     */
    private static class InspectionEnabler extends InspectionExclusionsUpdater {

        InspectionEnabler(String inspectionId) {
            super(inspectionId);
        }

        @Override
        public void updateConfiguration(Project project, JsonObject value) {
            String scopeUri = value.get("scopeUri").getAsString();
            VirtualFile resource = LSPIJUtils.findResourceFor(scopeUri);
            if (resource != null) {
                @Nullable Module module = LSPIJUtils.getModule(resource, project);
                if (module != null) {
                    String modulePath = LSPIJUtils.toUri(module).resolve("**").toString();
                    addToExclusions(project, modulePath);
                }
            }
        }

    }
}