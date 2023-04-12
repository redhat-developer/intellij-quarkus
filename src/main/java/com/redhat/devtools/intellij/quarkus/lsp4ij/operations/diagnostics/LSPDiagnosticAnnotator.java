package com.redhat.devtools.intellij.quarkus.lsp4ij.operations.diagnostics;

import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPEditorWrapper;
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

public class LSPDiagnosticAnnotator extends ExternalAnnotator<LSPVirtualFileWrapper, LSPVirtualFileWrapper> {

    @Nullable
    @Override
    public LSPVirtualFileWrapper collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        try {
            VirtualFile virtualFile = file.getVirtualFile();
            LSPEditorWrapper wrapper  = LSPEditorWrapper.getLSPEditorWrapper(editor);
            return LSPVirtualFileWrapper.getCache(virtualFile);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public @Nullable LSPVirtualFileWrapper doAnnotate(LSPVirtualFileWrapper cache) {
        return cache;
    }

    @Override
    public void apply(@NotNull PsiFile file, LSPVirtualFileWrapper cache, @NotNull AnnotationHolder holder) {
        if (cache.isUpdatingDiagnostic()) {
            createAnnotations(cache, holder);
        } else if (cache.isUpdatingCodeAction()) {
         updateAnnotations(cache, holder);
        }
    }

    private void createAnnotations(LSPVirtualFileWrapper cache, AnnotationHolder holder) {
        final List<Diagnostic> diagnostics = cache.getCurrentDiagnostics().getDiagnostics();
        VirtualFile virtualFile = cache.getFile();
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

        cache.setAnnotations(annotations);
        //eventManager.setAnonHolder(holder);
    }
    private void updateAnnotations(LSPVirtualFileWrapper cache, AnnotationHolder holder) {
        final List<Annotation> annotations = cache.getAnnotations();
        if (annotations == null) {
            return;
        }
        annotations.forEach(annotation -> {
            final TextRange range = new TextRange(annotation.getStartOffset() , annotation.getEndOffset());
            Annotation anon = holder.createAnnotation(annotation.getSeverity(),  range, annotation.getMessage());

            if (annotation.getQuickFixes() == null || annotation.getQuickFixes().isEmpty()) {
                return;
            }
            annotation.getQuickFixes().forEach(quickFixInfo -> anon.registerFix(quickFixInfo.quickFix));
        });
    }


    @Nullable
    protected Annotation createAnnotation(Document document, Diagnostic diagnostic, AnnotationHolder holder) {
        final int start = LSPIJUtils.toOffset(diagnostic.getRange().getStart(), document);
        final int end = LSPIJUtils.toOffset(diagnostic.getRange().getEnd(),document);
        if (start >= end) {
            return null;
        }
        final TextRange range = new TextRange(start, end);
        return holder.createAnnotation(getIJSeverity(diagnostic.getSeverity()), range, diagnostic.getMessage());
    }

    private static HighlightSeverity getIJSeverity(DiagnosticSeverity severity) {
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
