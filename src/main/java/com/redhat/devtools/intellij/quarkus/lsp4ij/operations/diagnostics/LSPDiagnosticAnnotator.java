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

import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPVirtualFileWrapper;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPIJUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DiagnosticTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Intellij ExternalAnnotator implementation which get the current LSP diagnostics for a given file and translate t
 * hem into Intellij Annotation.
 */
public class LSPDiagnosticAnnotator extends ExternalAnnotator<LSPVirtualFileWrapper, LSPVirtualFileWrapper> {

    @Nullable
    @Override
    public LSPVirtualFileWrapper collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        try {
            VirtualFile virtualFile = file.getVirtualFile();
            return LSPVirtualFileWrapper.getLSPVirtualFileWrapper(virtualFile);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public @Nullable LSPVirtualFileWrapper doAnnotate(LSPVirtualFileWrapper wrapper) {
        return wrapper;
    }

    @Override
    public void apply(@NotNull PsiFile file, LSPVirtualFileWrapper wrapper, @NotNull AnnotationHolder holder) {
        if (wrapper.isDiagnosticDirty()) {
            // LSP Diagnostics has been published for the given file, create for each diagnostic an Intellij Annotation
            createAnnotations(wrapper, holder);
            // FIXME : remove this following code, code action request should be called when cursor change or when error annotation is hovered!
            // Associate for each annotation some quick fixes coming from the code actions request
            List<Annotation>  annotations = wrapper.getAnnotations();
            if(annotations != null) {
                for (Annotation annotation : annotations) {
                    wrapper.updateQuickFixAt(annotation.getStartOffset());
                }
            }
        } else  {
            // Update annotation holder with existing annotations
            // This case comes from:
            // - when an external component from Intellij Quarkus refresh the validation with DaemonCodeAnalyzer.getInstance(module.getProject()).restart(psiFile);
            // - when quick fix are updated from LSP code actions.
         updateAnnotations(wrapper.getAnnotations(), holder);
        }
    }

    private void createAnnotations(LSPVirtualFileWrapper wrapper, AnnotationHolder holder) {
        final List<Diagnostic> diagnostics = wrapper.getAllDiagnostics();
        VirtualFile virtualFile = wrapper.getFile();
        Document document = LSPIJUtils.getDocument(virtualFile);

        List<Annotation> annotations = new ArrayList<>();
        diagnostics.forEach(diagnostic -> {
            Annotation annotation = createAnnotation(document, diagnostic, holder);
            if (annotation != null) {
                if (diagnostic.getTags() != null && diagnostic.getTags().contains(DiagnosticTag.Deprecated)) {
                    annotation.setHighlightType(ProblemHighlightType.LIKE_DEPRECATED);
                }
                annotations.add(annotation);
            }
        });

        wrapper.setAnnotations(annotations);
    }

    private static void updateAnnotations(List<Annotation> existingAnnotations, AnnotationHolder holder) {
        if (existingAnnotations == null || existingAnnotations.isEmpty()) {
            return;
        }
        // Copy all existing annotations in the given annotation holder.
        existingAnnotations.forEach(currentAnnotation -> {
            // Copy range, severity, message information
            TextRange range = new TextRange(currentAnnotation.getStartOffset() , currentAnnotation.getEndOffset());
            Annotation newAnnotation = holder.createAnnotation(currentAnnotation.getSeverity(),  range, currentAnnotation.getMessage());

            // Copy quick fixes information
            if (currentAnnotation.getQuickFixes() == null || currentAnnotation.getQuickFixes().isEmpty()) {
                return;
            }
            currentAnnotation.getQuickFixes().forEach(quickFixInfo -> newAnnotation.registerFix(quickFixInfo.quickFix));
        });
    }


    @Nullable
    private static Annotation createAnnotation(Document document, Diagnostic diagnostic, AnnotationHolder holder) {
        final int start = LSPIJUtils.toOffset(diagnostic.getRange().getStart(), document);
        final int end = LSPIJUtils.toOffset(diagnostic.getRange().getEnd(),document);
        if (start >= end) {
            // Language server reports invalid diagnostic, ignore it.
            return null;
        }
        HighlightSeverity severity = toHighlightSeverity(diagnostic.getSeverity());
        TextRange range = new TextRange(start, end);
        String message = diagnostic.getMessage();
        return holder.createAnnotation(severity, range, message);
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