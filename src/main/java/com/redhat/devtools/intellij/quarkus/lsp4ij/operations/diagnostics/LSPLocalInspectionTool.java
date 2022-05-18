/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.lsp4ij.operations.diagnostics;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageServerWrapper;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageServiceAccessor;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LSPLocalInspectionTool extends LocalInspectionTool {
    private static final Logger LOGGER = LoggerFactory.getLogger(LSPLocalInspectionTool.class);

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "LSP";
    }

    @Nls
    @NotNull
    @Override
    public String getGroupDisplayName() {
        return "LSP";
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile != null) {
            Editor editor = LSPIJUtils.editorForFile(virtualFile);
            if (editor != null) {
                List<ProblemDescriptor> problemDescriptors = new ArrayList<>();
                try {
                    for(LanguageServerWrapper wrapper : LanguageServiceAccessor.getInstance(file.getProject()).getLSWrappers(virtualFile, capabilities -> true)) {
                        RangeHighlighter[] highlighters = LSPDiagnosticsToMarkers.getMarkers(editor, wrapper.serverDefinition.id);
                        if (highlighters != null) {
                            for(RangeHighlighter highlighter : highlighters) {
                                PsiElement element;
                                if (highlighter.getEndOffset() - highlighter.getStartOffset() > 0) {
                                    element = new LSPPSiElement(editor.getProject(), file, highlighter.getStartOffset(), highlighter.getEndOffset(), editor.getDocument().getText(new TextRange(highlighter.getStartOffset(), highlighter.getEndOffset())));
                                } else {
                                    element = PsiUtilCore.getElementAtOffset(file, highlighter.getStartOffset());
                                }
                                ProblemHighlightType highlightType = getHighlighType(((Diagnostic)highlighter.getErrorStripeTooltip()).getSeverity());
                                problemDescriptors.add(manager.createProblemDescriptor(element, ((Diagnostic)highlighter.getErrorStripeTooltip()).getMessage(), true, highlightType, isOnTheFly));
                            }
                        }
                    }
                } catch (IOException e) {
                    LOGGER.warn(e.getLocalizedMessage(), e);
                }
                return problemDescriptors.toArray(new ProblemDescriptor[problemDescriptors.size()]);
            }

        }
        return super.checkFile(file, manager, isOnTheFly);
    }

    private ProblemHighlightType getHighlighType(DiagnosticSeverity severity) {
        switch (severity) {
            case Error:
                return ProblemHighlightType.ERROR;
            case Hint:
            case Information:
                return ProblemHighlightType.INFORMATION;
            case Warning:
                return ProblemHighlightType.WARNING;
        }
        return ProblemHighlightType.INFORMATION;
    }
}
