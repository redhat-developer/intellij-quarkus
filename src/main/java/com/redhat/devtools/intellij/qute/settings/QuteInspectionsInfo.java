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

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.project.Project;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.redhat.devtools.intellij.lsp4ij.operations.diagnostics.SeverityMapping;
import com.redhat.devtools.intellij.qute.psi.core.inspections.QuteUndefinedNamespaceInspection;
import com.redhat.devtools.intellij.qute.psi.core.inspections.QuteUndefinedObjectInspection;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.gradle.configurationcache.problems.ProblemSeverity;
import org.jetbrains.annotations.NotNull;

/**
 * Contains Qute inspection settings relevant to Qute LS configuration
 */
//TODO switch to a record, when Java 17 is required
public class QuteInspectionsInfo {

    private DiagnosticSeverity undefinedObjectSeverity = DiagnosticSeverity.Warning;
    private DiagnosticSeverity undefinedNamespaceSeverity = DiagnosticSeverity.Warning;

    private QuteInspectionsInfo() {
    }

    public static QuteInspectionsInfo getQuteInspectionsInfo(Project project) {
        QuteInspectionsInfo wrapper = new QuteInspectionsInfo();
        InspectionProfile profile = InspectionProfileManager.getInstance(project).getCurrentProfile();
        wrapper.undefinedObjectSeverity = SeverityMapping.getSeverity(QuteUndefinedObjectInspection.ID, profile);
        wrapper.undefinedNamespaceSeverity = SeverityMapping.getSeverity(QuteUndefinedNamespaceInspection.ID, profile);
        return wrapper;
    }

    public DiagnosticSeverity undefinedObjectSeverity() {
        return undefinedObjectSeverity;
    }

    public DiagnosticSeverity undefinedNamespaceSeverity() {
        return undefinedNamespaceSeverity;
    }

}
