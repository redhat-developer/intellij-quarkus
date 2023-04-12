package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.lang.annotation.Annotation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.quarkus.lsp4ij.operations.codeactions.LSPCodeActionIntentionAction;
import com.redhat.devtools.intellij.quarkus.lsp4ij.operations.diagnostics.LSPDiagnosticHandler;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class LSPEditorWrapper {

    private static final Key<LSPEditorWrapper> KEY = new Key(LSPEditorWrapper.class.getName());

    public static LSPEditorWrapper getLSPEditorWrapper(Editor editor) {
        LSPEditorWrapper wrapper = editor.getUserData(KEY);
        if (wrapper != null) {
            return wrapper;
        }
        return getLSPEditorWrapperSync(editor);
    }

    private static synchronized LSPEditorWrapper getLSPEditorWrapperSync(Editor editor) {
        LSPEditorWrapper wrapper = editor.getUserData(KEY);
        if (wrapper != null) {
            return wrapper;
        }
        wrapper = new LSPEditorWrapper(editor);
        editor.putUserData(KEY, wrapper);
        return wrapper;
    }

    private final Editor editor;

    private final CaretListener caretListener;

    private LSPEditorWrapper(Editor editor) {
        this.editor = editor;
        this.caretListener = new CaretListener() {
            @Override
            public void caretPositionChanged(@NotNull CaretEvent event) {
                int offset = editor.getCaretModel().getCurrentCaret().getOffset();
                updateCodeActions(offset);
            }
        };
        this.editor.getCaretModel().addCaretListener(caretListener);
        //this.editor.addEditorMouseListener();
    }

    public static boolean hasWrapper(Editor editor) {
        return editor.getUserData(KEY) != null;
    }

    public void dispose() {
        this.editor.getCaretModel().removeCaretListener(caretListener);
        editor.putUserData(KEY, null);
    }

    public void updateCodeActions(int offset) {
        Document document = editor.getDocument();
        URI fileUri = LSPIJUtils.toUri(document);
        VirtualFile file = LSPIJUtils.getFile(document);

        CodeActionParams params = new CodeActionParams();
        params.setTextDocument(LSPIJUtils.toTextDocumentIdentifier(fileUri));
        Position pos = LSPIJUtils.toPosition(offset, document);
        Range range = new Range(pos, pos);
        params.setRange(range);

        LSPVirtualFileWrapper wrapper = LSPVirtualFileWrapper.getCache(file);
        PublishDiagnosticsParams publishDiagnostics = wrapper.getCurrentDiagnostics();
        List<Annotation> annotations = wrapper.getAnnotations() != null ? new ArrayList<>(wrapper.getAnnotations()) : Collections.emptyList();
        List<Diagnostic> diagnosticContext = new ArrayList<>();
        if (publishDiagnostics != null && publishDiagnostics.getDiagnostics() != null && !publishDiagnostics.getDiagnostics().isEmpty()) {
            List<Diagnostic> diagnostics = new ArrayList<>(publishDiagnostics.getDiagnostics());
            diagnostics.forEach(diagnostic -> {
                int startOffset = LSPIJUtils.toOffset(diagnostic.getRange().getStart(), document);
                int endOffset = LSPIJUtils.toOffset(diagnostic.getRange().getEnd(), document);
                if (offset >= startOffset && offset <= endOffset) {
                    diagnosticContext.add(diagnostic);
                }
            });
        }

        CodeActionContext context = new CodeActionContext(diagnosticContext);
        params.setContext(context);

        Project project = LSPIJUtils.getProject(file).getProject();
        CompletableFuture<Void> future = LanguageServiceAccessor.getInstance(project)
                .getLanguageServers(document, capabilities -> capabilities.getCodeActionProvider() != null)
                .thenComposeAsync(languageServers -> CompletableFuture.allOf(languageServers.stream()
                        .map(languageServer -> languageServer.getTextDocumentService().codeAction(params)
                                .thenAcceptAsync(codeActions -> {
                                    // textDocument/codeAction may return null
                                    if (codeActions != null) {
                                        codeActions.stream().filter(Objects::nonNull)
                                                .forEach(commandOrCodeAction -> {
                                                    updateAnnotation(commandOrCodeAction, annotations, offset, languageServer);
                                                });
                                        wrapper.setUpdatingCodeAction(true);
                                        updateErrorAnnotations(document, project);
                                    }
                                }))
                        .toArray(CompletableFuture[]::new)));
    }

    private void updateAnnotation(Either<Command, CodeAction> commandOrCodeAction, List<Annotation> annotations, int carretOffset, LanguageServer server) {
        if (commandOrCodeAction.isLeft()) {
            Command command = commandOrCodeAction.getLeft();
            annotations.forEach(annotation -> {
                int start = annotation.getStartOffset();
                int end = annotation.getEndOffset();
                if (start <= carretOffset && end >= carretOffset) {
                    // annotation.registerFix(new LSPCommandFix(FileUtils.editorToURIString(editor), command),
                    //         new TextRange(start, end));
                }
            });
        } else if (commandOrCodeAction.isRight()) {
            CodeAction codeAction = commandOrCodeAction.getRight();
            List<Diagnostic> diagnosticContext = codeAction.getDiagnostics();
            annotations.forEach(annotation -> {
                int start = annotation.getStartOffset();
                int end = annotation.getEndOffset();
                if (start <= carretOffset && end >= carretOffset) {
                    annotation.registerFix(new LSPCodeActionIntentionAction(commandOrCodeAction,
                            server), new TextRange(start, end));
                }
            });

        }
    }

    private void updateErrorAnnotations(Document document, Project project) {
        ApplicationManager.getApplication().runReadAction(() -> {
            final PsiFile file = PsiDocumentManager.getInstance(project)
                    .getCachedPsiFile(document);
            if (file == null) {
                return;
            }
            DaemonCodeAnalyzer.getInstance(project).restart(file);
            return;
        });
    }
}
