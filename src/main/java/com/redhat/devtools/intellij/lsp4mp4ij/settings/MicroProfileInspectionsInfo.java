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
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.inspections.*;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Contains Microprofile inspection settings relevant to LSP4MP configuration
 */
//TODO switch to a record, when Java 17 is required
public class MicroProfileInspectionsInfo {

    //See https://github.com/eclipse/lsp4mp/blob/6b483c4d292bfebabd13311d6217291da2d5d169/microprofile.ls/org.eclipse.lsp4mp.ls/src/main/java/org/eclipse/lsp4mp/settings/MicroProfileValidationSettings.java#L36
    private boolean enabled = true;
    private ProblemSeverity syntaxSeverity = ProblemSeverity.error;
    private ProblemSeverity unknownSeverity = ProblemSeverity.warning;
    private ProblemSeverity duplicateSeverity = ProblemSeverity.warning;
    private ProblemSeverity valueSeverity = ProblemSeverity.error;
    private ProblemSeverity requiredSeverity = ProblemSeverity.none;
    private ProblemSeverity expressionSeverity = ProblemSeverity.error;

    private MicroProfileInspectionsInfo() {
    }

    public static MicroProfileInspectionsInfo getMicroProfileInspectionInfo(Project project) {
        MicroProfileInspectionsInfo wrapper = new MicroProfileInspectionsInfo();
        InspectionProfile profile = InspectionProfileManager.getInstance(project).getCurrentProfile();
        boolean syntaxEnabled = isInspectionEnabled(MicroProfilePropertiesSyntaxInspection.ID, profile);
        boolean unknownEnabled = isInspectionEnabled(MicroProfilePropertiesUnknownInspection.ID, profile);
        boolean duplicatedEnabled = isInspectionEnabled(MicroProfilePropertiesDuplicatesInspection.ID, profile);
        boolean valueEnabled = isInspectionEnabled(MicroProfilePropertiesValueInspection.ID, profile);
        boolean requiredEnabled = isInspectionEnabled(MicroProfilePropertiesRequiredInspection.ID, profile);
        boolean expressionsEnabled = isInspectionEnabled(MicroProfilePropertiesExpressionsInspection.ID, profile);
        wrapper.enabled = syntaxEnabled
                || unknownEnabled
                || duplicatedEnabled
                || valueEnabled
                || requiredEnabled
                || expressionsEnabled;

        wrapper.syntaxSeverity = getSeverity(syntaxEnabled, MicroProfilePropertiesSyntaxInspection.ID, profile);
        wrapper.unknownSeverity = getSeverity(unknownEnabled, MicroProfilePropertiesUnknownInspection.ID, profile);
        wrapper.duplicateSeverity = getSeverity(duplicatedEnabled, MicroProfilePropertiesDuplicatesInspection.ID, profile);
        wrapper.valueSeverity = getSeverity(valueEnabled, MicroProfilePropertiesValueInspection.ID, profile);
        wrapper.requiredSeverity = getSeverity(requiredEnabled, MicroProfilePropertiesRequiredInspection.ID, profile);
        wrapper.expressionSeverity = getSeverity(expressionsEnabled, MicroProfilePropertiesExpressionsInspection.ID, profile);
        return wrapper;
    }

    private static ProblemSeverity getSeverity(boolean enabled, String inspectionId, InspectionProfile profile) {
        if (!enabled) {
            return ProblemSeverity.none;
        }
        return ProblemSeverity.getSeverity(getErrorLevel(inspectionId, profile));

    }

    private static @NotNull HighlightSeverity getErrorLevel(String inspectionId, InspectionProfile profile) {
        HighlightDisplayLevel level = profile.getErrorLevel(HighlightDisplayKey.find(inspectionId), null);
        return level.getSeverity();
    }

    private static boolean isInspectionEnabled(@NotNull String inspectionId, @NotNull InspectionProfile profile) {
        return profile.isToolEnabled(HighlightDisplayKey.find(inspectionId));
    }

    public boolean enabled() {
        return enabled;
    }

    public ProblemSeverity unknownSeverity() {
        return unknownSeverity;
    }

    public ProblemSeverity valueSeverity() {
        return valueSeverity;
    }

    public ProblemSeverity expressionSeverity() {
        return expressionSeverity;
    }

    public ProblemSeverity duplicateSeverity() {
        return duplicateSeverity;
    }

    public ProblemSeverity syntaxSeverity() {
        return syntaxSeverity;
    }

    public ProblemSeverity requiredSeverity() {
        return requiredSeverity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MicroProfileInspectionsInfo that = (MicroProfileInspectionsInfo) o;
        return enabled == that.enabled
                && syntaxSeverity == that.syntaxSeverity && unknownSeverity == that.unknownSeverity
                && duplicateSeverity == that.duplicateSeverity && valueSeverity == that.valueSeverity
                && requiredSeverity == that.requiredSeverity && expressionSeverity == that.expressionSeverity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, syntaxSeverity, unknownSeverity, duplicateSeverity, valueSeverity, requiredSeverity,
                expressionSeverity);
    }
}
