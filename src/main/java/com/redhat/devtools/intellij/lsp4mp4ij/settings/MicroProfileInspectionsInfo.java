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
package com.redhat.devtools.intellij.lsp4mp4ij.settings;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.project.Project;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.redhat.devtools.intellij.lsp4ij.operations.diagnostics.SeverityMapping;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.inspections.*;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Contains Microprofile inspection settings relevant to LSP4MP configuration
 */
//TODO switch to a record, when Java 17 is required
public class MicroProfileInspectionsInfo {

    //See https://github.com/eclipse/lsp4mp/blob/6b483c4d292bfebabd13311d6217291da2d5d169/microprofile.ls/org.eclipse.lsp4mp.ls/src/main/java/org/eclipse/lsp4mp/settings/MicroProfileValidationSettings.java#L36
    private DiagnosticSeverity syntaxSeverity = DiagnosticSeverity.Error;
    private DiagnosticSeverity unknownSeverity = DiagnosticSeverity.Warning;
    private DiagnosticSeverity duplicateSeverity = DiagnosticSeverity.Warning;
    private DiagnosticSeverity valueSeverity = DiagnosticSeverity.Error;
    private DiagnosticSeverity requiredSeverity = null;
    private DiagnosticSeverity expressionSeverity = DiagnosticSeverity.Error;

    private MicroProfileInspectionsInfo() {
    }

    public static MicroProfileInspectionsInfo getMicroProfileInspectionInfo(Project project) {
        MicroProfileInspectionsInfo wrapper = new MicroProfileInspectionsInfo();
        InspectionProfile profile = InspectionProfileManager.getInstance(project).getCurrentProfile();
        wrapper.syntaxSeverity = SeverityMapping.getSeverity(MicroProfilePropertiesSyntaxInspection.ID, profile);
        wrapper.unknownSeverity = SeverityMapping.getSeverity(MicroProfilePropertiesUnknownInspection.ID, profile);
        wrapper.duplicateSeverity = SeverityMapping.getSeverity(MicroProfilePropertiesDuplicatesInspection.ID, profile);
        wrapper.valueSeverity = SeverityMapping.getSeverity(MicroProfilePropertiesValueInspection.ID, profile);
        wrapper.requiredSeverity = SeverityMapping.getSeverity(MicroProfilePropertiesRequiredInspection.ID, profile);
        wrapper.expressionSeverity = SeverityMapping.getSeverity(MicroProfilePropertiesExpressionsInspection.ID, profile);
        return wrapper;
    }

    public DiagnosticSeverity unknownSeverity() {
        return unknownSeverity;
    }

    public DiagnosticSeverity valueSeverity() {
        return valueSeverity;
    }

    public DiagnosticSeverity expressionSeverity() {
        return expressionSeverity;
    }

    public DiagnosticSeverity duplicateSeverity() {
        return duplicateSeverity;
    }

    public DiagnosticSeverity syntaxSeverity() {
        return syntaxSeverity;
    }

    public DiagnosticSeverity requiredSeverity() {
        return requiredSeverity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MicroProfileInspectionsInfo that = (MicroProfileInspectionsInfo) o;
        return  syntaxSeverity == that.syntaxSeverity && unknownSeverity == that.unknownSeverity
                && duplicateSeverity == that.duplicateSeverity && valueSeverity == that.valueSeverity
                && requiredSeverity == that.requiredSeverity && expressionSeverity == that.expressionSeverity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(syntaxSeverity, unknownSeverity, duplicateSeverity, valueSeverity, requiredSeverity,
                expressionSeverity);
    }
}
