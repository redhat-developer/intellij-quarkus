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
package com.redhat.devtools.intellij.qute.settings;

import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.redhat.devtools.lsp4ij.features.diagnostics.SeverityMapping;
import com.redhat.devtools.lsp4ij.inspections.AbstractDelegateInspectionWithExclusions;
import com.redhat.devtools.intellij.qute.psi.core.inspections.QuteGlobalInspection;
import com.redhat.devtools.intellij.qute.psi.core.inspections.QuteUndefinedNamespaceInspection;
import com.redhat.devtools.intellij.qute.psi.core.inspections.QuteUndefinedObjectInspection;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Contains Qute inspection settings relevant to Qute LS configuration
 */
//TODO switch to a record, when Java 17 is required
public class QuteInspectionsInfo {

    private boolean enabled = true;
    private DiagnosticSeverity undefinedObjectSeverity = DiagnosticSeverity.Warning;
    private DiagnosticSeverity undefinedNamespaceSeverity = DiagnosticSeverity.Warning;

    public List<String> getExcludedFiles() {
        return excludedFiles;
    }

    private List<String> excludedFiles;


    private QuteInspectionsInfo() {
    }

    public static QuteInspectionsInfo getQuteInspectionsInfo(Project project) {
        QuteInspectionsInfo wrapper = new QuteInspectionsInfo();
        InspectionProfile profile = InspectionProfileManager.getInstance(project).getCurrentProfile();
        wrapper.enabled = SeverityMapping.getSeverity(QuteGlobalInspection.ID, profile) != null;
        wrapper.undefinedObjectSeverity = SeverityMapping.getSeverity(QuteUndefinedObjectInspection.ID, profile);
        wrapper.undefinedNamespaceSeverity = SeverityMapping.getSeverity(QuteUndefinedNamespaceInspection.ID, profile);
        wrapper.excludedFiles = getExclusions(profile, QuteGlobalInspection.ID, project);
        return wrapper;
    }

    public DiagnosticSeverity undefinedObjectSeverity() {
        return undefinedObjectSeverity;
    }

    public DiagnosticSeverity undefinedNamespaceSeverity() {
        return undefinedNamespaceSeverity;
    }

    public boolean enabled() {
        return enabled;
    }

    private static List<String> getExclusions(InspectionProfile profile, String inspectionId, Project project) {
        List<String> exclusions = new ArrayList<>();
        InspectionToolWrapper<?, ?> toolWrapper = profile.getInspectionTool(inspectionId, project);
        if (toolWrapper != null && toolWrapper.getTool() instanceof AbstractDelegateInspectionWithExclusions) {
            AbstractDelegateInspectionWithExclusions inspection = (AbstractDelegateInspectionWithExclusions) toolWrapper.getTool();
            exclusions.addAll(inspection.excludeList);
        }
        return exclusions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuteInspectionsInfo that = (QuteInspectionsInfo) o;
        return enabled == that.enabled && undefinedObjectSeverity == that.undefinedObjectSeverity && undefinedNamespaceSeverity == that.undefinedNamespaceSeverity && Objects.equals(excludedFiles, that.excludedFiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, undefinedObjectSeverity, undefinedNamespaceSeverity, excludedFiles);
    }
}
