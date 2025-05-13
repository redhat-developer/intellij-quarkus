/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.lsp;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.qute.lang.QuteLanguage;
import com.redhat.devtools.intellij.qute.psi.core.inspections.QuteLSPLocalInspectionTool;
import com.redhat.devtools.lsp4ij.client.features.LSPDiagnosticFeature;
import org.jetbrains.annotations.NotNull;

/**
 * Qute diagnostics features.
 */
public class QuteDiagnosticFeature extends LSPDiagnosticFeature {

    @Override
    public boolean isInspectionApplicableFor(@NotNull PsiFile file,
                                             @NotNull LocalInspectionTool inspection) {
        if (file.getLanguage() == QuteLanguage.INSTANCE) {
            // Show Qute templates warnings/errors
            // after clicking on "Inspect Code..." from the problem view.
            return QuteLSPLocalInspectionTool.ID.equals(inspection.getID());
        }
        return super.isInspectionApplicableFor(file, inspection);
    }
}
