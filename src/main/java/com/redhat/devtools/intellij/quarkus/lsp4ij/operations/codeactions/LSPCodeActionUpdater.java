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
package com.redhat.devtools.intellij.quarkus.lsp4ij.operations.codeactions;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPVirtualFileWrapper;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageServiceAccessor;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * LSP code action updater to collect LSP code action only when:
 *
 * <ul>
 *     <li>when an LSP diagnostic is hovered
 *     <li>or when caret cursor changed without document content changes.</li>
 * </ul>
 *
 * <p>We need to implement this code action updater because in Intellij, the standard QuickFix mechanism is to register
 * QuickFixes while validation operation (we create an Annotation and we call registerFix on the created annotation).
 * </p>
 *
 * <p>If we follow this standard mechanism,it means that we need to collect code actions for each reported diagnostics which
 * impact the validation performance and if LSP code action cannot support resolve, it means that we will resolve all code actions
 * for all diagnostics in the validation step although user use just one quick fix (if he click on quick fix).</p>
 *
 * <p>To perform very good performance, validation step done with {@link com.redhat.devtools.intellij.quarkus.lsp4ij.operations.diagnostics.LSPDiagnosticAnnotator} create only {@link com.intellij.lang.annotation.Annotation} without QuickFix.
 * LSP CodeAction are collected only when a diagnostic is hovered or if caret cursor changed by updating Quickfixes directly in the
 * the {@link HighlightInfo} which is used in the Tooltip popup and Bullet Inspection</p>
 */
public class LSPCodeActionUpdater {

    private final Editor editor;

    private final EditorMouseMotionListener editorMouseMotionListener;

    private final CaretListener caretListener;

    private final DocumentListener documentListener;

    private boolean documentChanged;

    private LSPCodeActionResult codeActionResult;

    public LSPCodeActionUpdater(Editor editor) {
        this.editor = editor;
        // Add a mouse motion listener to update Intellij QuickFix when an LSP diagnostic is hovered
        this.editorMouseMotionListener = new EditorMouseMotionListener() {
            @Override
            public void mouseMoved(@NotNull EditorMouseEvent e) {
                Document document = editor.getDocument();
                VirtualFile file = LSPIJUtils.getFile(document);
                List<Diagnostic> currentDiagnostics = LSPVirtualFileWrapper.getLSPVirtualFileWrapper(file).getAllDiagnostics();
                if (currentDiagnostics != null && !currentDiagnostics.isEmpty()) {
                    int offset = getTargetOffset(e);
                    if (offset != -1) {
                        for (Diagnostic diagnostic : currentDiagnostics) {
                            int startOffset = LSPIJUtils.toOffset(diagnostic.getRange().getStart(), document);
                            int endOffset = LSPIJUtils.toOffset(diagnostic.getRange().getEnd(), document);
                            if (offset >= startOffset && offset <= endOffset) {
                                try {
                                    updateQuickFixAt(offset,startOffset,endOffset).get();
                                } catch (InterruptedException interruptedException) {
                                    interruptedException.printStackTrace();
                                } catch (ExecutionException executionException) {
                                    executionException.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        };
        this.editor.addEditorMouseMotionListener(editorMouseMotionListener);

        // Add a caret listener to update Intellij QuickFix when caret cursor changed (without document changes)
        this.documentChanged = false;
        this.documentListener = new DocumentListener() {

            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                documentChanged = true;
            }
        };
        this.editor.getDocument().addDocumentListener(documentListener);

        this.caretListener = new CaretListener() {
            @Override
            public void caretPositionChanged(@NotNull CaretEvent event) {
                if (documentChanged) {
                    documentChanged = false;
                    return;
                }
                int offset = editor.getCaretModel().getCurrentCaret().getOffset();
                VirtualFile file = LSPIJUtils.getFile(editor.getDocument());
                if (file != null) {
                    updateQuickFixAt(offset, -1,-1);
                }
            }
        };
        this.editor.getCaretModel().addCaretListener(caretListener);
    }

    private static int getTargetOffset(EditorMouseEvent event) {
        Editor editor = event.getEditor();
        if (editor instanceof EditorEx &&
                editor.getProject() != null &&
                event.getArea() == EditorMouseEventArea.EDITING_AREA &&
                event.getMouseEvent().getModifiers() == 0 &&
                event.isOverText() &&
                event.getCollapsedFoldRegion() == null) {
            return event.getOffset();
        }
        return -1;
    }

    /**
     * Update Intellij QuickFixes which should be available at the given offset
     * by collecting LSP code actions at the given offset.
     *
     * @param offset the caret / hover offset
     * @return
     */
    private CompletableFuture<Void> updateQuickFixAt(int offset, int start,int end) {
        Document document = editor.getDocument();
        VirtualFile file = LSPIJUtils.getFile(document);
        if (file == null) {
            return null;
        }
        URI fileUri = LSPIJUtils.toUri(document);

        CodeActionParams params = new CodeActionParams();
        params.setTextDocument(LSPIJUtils.toTextDocumentIdentifier(fileUri));
        Position pos = LSPIJUtils.toPosition(offset, document);
        Range range = new Range(pos, pos);
        params.setRange(range);

        List<Diagnostic> currentDiagnostics = LSPVirtualFileWrapper.getLSPVirtualFileWrapper(file).getAllDiagnostics();
        List<Diagnostic> diagnosticContext = new ArrayList<>();
        if (!currentDiagnostics.isEmpty()) {
            List<Diagnostic> diagnostics = new ArrayList<>(currentDiagnostics);
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
        return LanguageServiceAccessor.getInstance(project)
                .getLanguageServers(document, capabilities -> capabilities.getCodeActionProvider() != null)
                .thenComposeAsync(languageServers -> CompletableFuture.allOf(languageServers.stream()
                        .map(languageServer -> languageServer.getTextDocumentService().codeAction(params)
                                .thenAcceptAsync(codeActions -> {
                                    // textDocument/codeAction may return null
                                    if (codeActions != null) {
                                        this.codeActionResult = new LSPCodeActionResult(context, codeActions, languageServer);
                                        updateErrorAnnotations(document, project, editor, offset, start, end);
                                    }
                                }))
                        .toArray(CompletableFuture[]::new)));
    }

    public LSPCodeActionResult getCodeActionResult() {
        return codeActionResult;
    }

    private void updateErrorAnnotations(Document document, Project project, Editor editor, int offset, int start,int end) {
        ApplicationManager.getApplication().runReadAction(() -> {
            final PsiFile file = PsiDocumentManager.getInstance(project)
                    .getCachedPsiFile(document);
            if (file == null) {
                return;
            }
            DaemonCodeAnalyzer codeAnalyzer = DaemonCodeAnalyzer.getInstance(project);
            HighlightInfo info = ((DaemonCodeAnalyzerImpl) codeAnalyzer).findHighlightByOffset(editor.getDocument(), offset, false);
            if (info != null) {

                if (codeActionResult != null) {
                    LanguageServer languageServer = codeActionResult.getLanguageServer();
                    List<Either<Command, CodeAction>> codeActions = codeActionResult.getCodeActions();

                    for (Either<Command, CodeAction> commandOrCodeAction : codeActions) {
                        LSPCodeActionIntentionAction quickFix = new LSPCodeActionIntentionAction(commandOrCodeAction,
                                languageServer);
                        TextRange textRange = new TextRange(start != -1 ? start : offset, end != -1 ? end : offset);
                        HighlightDisplayKey k = HighlightDisplayKey.find("Annotator");
                        info.registerFix(quickFix, null, HighlightDisplayKey.getDisplayNameByKey(k), textRange, k);
                    }

                    // See UpdateHighlightersUtil
                    List<Pair<HighlightInfo.IntentionActionDescriptor, RangeMarker>> list =
                            new ArrayList<>(info.quickFixActionRanges.size());
                    for (Pair<HighlightInfo.IntentionActionDescriptor, TextRange> pair : info.quickFixActionRanges) {
                        TextRange textRange = pair.second;
                        RangeMarker marker = document.createRangeMarker(textRange);
                        list.add(Pair.create(pair.first, marker));
                    }
                    info.quickFixActionMarkers = ContainerUtil.createLockFreeCopyOnWriteList(list);
                }
            }
        });
    }

    public void dispose() {
        this.editor.removeEditorMouseMotionListener(editorMouseMotionListener);
        this.editor.getCaretModel().removeCaretListener(caretListener);
        this.editor.getDocument().removeDocumentListener(documentListener);
    }
}