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
package com.redhat.devtools.intellij.lsp4ij.operations.diagnostics;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4ij.LSPVirtualFileWrapper;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Intellij {@link ExternalAnnotator} implementation which get the current LSP diagnostics for a given file and translate
 * them into Intellij {@link com.intellij.lang.annotation.Annotation}.
 */
public class LSPDiagnosticAnnotator extends ExternalAnnotator<LSPVirtualFileWrapper, LSPVirtualFileWrapper> {

    @Nullable
    @Override
    public LSPVirtualFileWrapper collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        try {
            return LSPVirtualFileWrapper.getLSPVirtualFileWrapper(file.getVirtualFile());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public @Nullable LSPVirtualFileWrapper doAnnotate(LSPVirtualFileWrapper wrapper) {
        return wrapper;
    }

    @Override
    public void apply(@NotNull PsiFile file, LSPVirtualFileWrapper editorWrapper, @NotNull AnnotationHolder holder) {
        // Get current LSP diagnostics of the current file
        LSPVirtualFileWrapper fileWrapper = LSPVirtualFileWrapper.getLSPVirtualFileWrapper(file.getVirtualFile());
        final Collection<LSPDiagnosticsForServer> diagnosticsPerServer = fileWrapper.getAllDiagnostics();
        Document document = LSPIJUtils.getDocument(file.getVirtualFile());

        // Loop for language server which report diagnostics for the given file
        diagnosticsPerServer.forEach(ds -> {
            boolean codeActionsLoading = false;
            // Loop for LSP diagnostics to transform it to Intellij annotation.
            for (Diagnostic diagnostic : ds.getDiagnostics()) {
                codeActionsLoading = codeActionsLoading | createAnnotation(diagnostic, document, ds, holder);
            }
            if (codeActionsLoading) {
                // QuickFixes are loading, refresh them in a background thread
                ds.refreshQuickFixesIfNeeded();
            }
        });
    }

    private static boolean createAnnotation(Diagnostic diagnostic, Document document, LSPDiagnosticsForServer diagnosticsForServer, AnnotationHolder holder) {
        final int start = LSPIJUtils.toOffset(diagnostic.getRange().getStart(), document);
        final int end = LSPIJUtils.toOffset(diagnostic.getRange().getEnd(), document);
        if (start >= end) {
            // Language server reports invalid diagnostic, ignore it.
            return false;
        }
        // Collect information required to create Intellij Annotations
        HighlightSeverity severity = toHighlightSeverity(diagnostic.getSeverity());
        TextRange range = new TextRange(start, end);
        String message = diagnostic.getMessage();
        List<IntentionAction> fixes = diagnosticsForServer.getQuickFixesFor(diagnostic);
        
        // Create Intellij Annotation from the given LSP diagnostic
        AnnotationBuilder builder = holder
                .newAnnotation(severity, message)
                .range(range);

        // Register quick fixes if there are available
        boolean codeActionsLoading = LSPDiagnosticsForServer.isCodeActionsLoading(fixes);
        if (!codeActionsLoading) {
            for (IntentionAction fix : fixes) {
                builder.withFix(fix);
            }
        }
        builder.create();
        return codeActionsLoading;
    }

    private static HighlightSeverity toHighlightSeverity(DiagnosticSeverity severity) {
        switch (severity) {
            case Warning:
                return HighlightSeverity.WEAK_WARNING;
            case Hint:
            case Information:
                return HighlightSeverity.INFORMATION;
            default:
                return HighlightSeverity.ERROR;
        }
    }

}