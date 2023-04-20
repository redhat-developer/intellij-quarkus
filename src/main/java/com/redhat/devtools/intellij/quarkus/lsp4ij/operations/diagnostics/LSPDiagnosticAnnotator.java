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
package com.redhat.devtools.intellij.quarkus.lsp4ij.operations.diagnostics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPEditorWrapper;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPVirtualFileWrapper;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Intellij {@link ExternalAnnotator} implementation which get the current LSP diagnostics for a given file and translate
 * them into Intellij {@link com.intellij.lang.annotation.Annotation}.
 */
public class LSPDiagnosticAnnotator extends ExternalAnnotator<LSPEditorWrapper, LSPEditorWrapper> {

    @Nullable
    @Override
    public LSPEditorWrapper collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        try {
            return LSPEditorWrapper.getLSPEditorWrapper(editor);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public @Nullable LSPEditorWrapper doAnnotate(LSPEditorWrapper wrapper) {
        return wrapper;
    }

    @Override
    public void apply(@NotNull PsiFile file, LSPEditorWrapper editorWrapper, @NotNull AnnotationHolder holder) {
        LSPVirtualFileWrapper fileWrapper = LSPVirtualFileWrapper.getLSPVirtualFileWrapper(file.getVirtualFile());
        final List<Diagnostic> diagnostics = fileWrapper.getAllDiagnostics();
        Document document = editorWrapper.getEditor().getDocument();
        diagnostics.forEach(diagnostic -> {
            createAnnotation(diagnostic, document, holder);
        });
    }

    @Nullable
    private static void createAnnotation(Diagnostic diagnostic, Document document, AnnotationHolder holder) {
        final int start = LSPIJUtils.toOffset(diagnostic.getRange().getStart(), document);
        final int end = LSPIJUtils.toOffset(diagnostic.getRange().getEnd(), document);
        if (start >= end) {
            // Language server reports invalid diagnostic, ignore it.
            return;
        }
        // Create Intellij Annotation from the given LSP diagnostic
        HighlightSeverity severity = toHighlightSeverity(diagnostic.getSeverity());
        TextRange range = new TextRange(start, end);
        String message = diagnostic.getMessage();
        holder.newAnnotation(severity, message)
                .range(range)
                .create();
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