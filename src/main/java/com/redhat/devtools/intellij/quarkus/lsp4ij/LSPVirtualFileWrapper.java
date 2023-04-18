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
 *     Some piece of code has been inspired by https://github.com/ballerina-platform/lsp4intellij/blob/master/src/main/java/org/wso2/lsp4intellij/editor/EditorEventManager.java
 *******************************************************************************/
package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.lang.annotation.Annotation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.quarkus.lsp4ij.operations.codeactions.LSPCodeActionIntentionAction;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * LSP wrapper for VirtualFile which maintains the current diagnostics
 * for all language servers mapped with this file.
 *
 * @author Angelo ZERR
 */
public class LSPVirtualFileWrapper {

    private static final Key<LSPVirtualFileWrapper> KEY = new Key(LSPVirtualFileWrapper.class.getName());

    private final VirtualFile file;

    private final Map<String /* language server id */, List<Diagnostic> /* current diagnostics */> diagnosticsPerServer;

    private List<Annotation> annotations;

    private boolean diagnosticDirty;

    LSPVirtualFileWrapper(VirtualFile file) {
        this.file = file;
        this.diagnosticsPerServer = new HashMap<>();
    }

    public VirtualFile getFile() {
        return file;
    }

    // ------------------------ LSP Diagnostics

    /**
     * Update the new published diagnostics for the given language server id.
     *
     * @param diagnostics      the new diagnostics list
     * @param languageServerId the language server id which has published those diagnostics.
     */
    public void updateDiagnostics(List<Diagnostic> diagnostics, String languageServerId) {
        diagnosticDirty = true;
        diagnosticsPerServer.put(languageServerId, diagnostics);
    }

    /**
     * Returns all current diagnostics reported by all language servers mapped with the file.
     *
     * @return all current diagnostics reported by all language servers mapped with the file.
     */
    public List<Diagnostic> getAllDiagnostics() {
        diagnosticDirty = false;
        if (diagnosticsPerServer.isEmpty()) {
            return Collections.emptyList();
        }
        return diagnosticsPerServer.values()
                .stream()
                .flatMap(diagnostics -> diagnostics.stream())
                .collect(Collectors.toList());
    }


    /**
     * Returns true if there a language server which has published new diagnostics and false otherwise.
     *
     * @return true if there a language server which has published new diagnostics and false otherwise.
     */
    public boolean isDiagnosticDirty() {
        return diagnosticDirty;
    }

    // ------------------------ Intellij Annotations

    /**
     * Set the current Intellij annotations which matches the current LSP diagnostics.
     *
     * @param annotations the Intellij Annotations
     */
    public void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
    }

    /**
     * Returns the current Intellij annotations.
     *
     * @return the current Intellij annotations.
     */
    public List<Annotation> getAnnotations() {
        return annotations;
    }

    /**
     * Update Intellij QuickFixes which should be available at the given offset
     * by collecting LSP code actions at the given offset.
     *
     * @param offset the caret / hover offset
     */
    public void updateQuickFixAt(int offset) {

        Document document = LSPIJUtils.getDocument(file);
        URI fileUri = LSPIJUtils.toUri(document);

        CodeActionParams params = new CodeActionParams();
        params.setTextDocument(LSPIJUtils.toTextDocumentIdentifier(fileUri));
        Position pos = LSPIJUtils.toPosition(offset, document);
        Range range = new Range(pos, pos);
        params.setRange(range);

        List<Diagnostic> currentDiagnostics = getAllDiagnostics();
        List<Annotation> currentAnnotations = getAnnotations() != null ? new ArrayList<>(getAnnotations()) : Collections.emptyList();
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
        CompletableFuture<Void> future = LanguageServiceAccessor.getInstance(project)
                .getLanguageServers(document, capabilities -> capabilities.getCodeActionProvider() != null)
                .thenComposeAsync(languageServers -> CompletableFuture.allOf(languageServers.stream()
                        .map(languageServer -> languageServer.getTextDocumentService().codeAction(params)
                                .thenAcceptAsync(codeActions -> {
                                    // textDocument/codeAction may return null
                                    if (codeActions != null) {
                                        codeActions.stream().filter(Objects::nonNull)
                                                .forEach(commandOrCodeAction -> {
                                                    updateAnnotation(commandOrCodeAction, currentAnnotations, offset, languageServer);
                                                });
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

    // ------------------------ Static accessor

    /**
     * Returns the LSPVirtualFileWrapper for the given file.
     *
     * @param file the virtual file.
     * @return the LSPVirtualFileWrapper for the given file.
     */
    public static LSPVirtualFileWrapper getLSPVirtualFileWrapper(VirtualFile file) {
        LSPVirtualFileWrapper wrapper = file.getUserData(KEY);
        if (wrapper != null) {
            return wrapper;
        }
        return getLSPVirtualFileWrapperSync(file);
    }

    private static synchronized LSPVirtualFileWrapper getLSPVirtualFileWrapperSync(VirtualFile file) {
        LSPVirtualFileWrapper wrapper = file.getUserData(KEY);
        if (wrapper != null) {
            return wrapper;
        }
        wrapper = new LSPVirtualFileWrapper(file);
        file.putUserData(KEY, wrapper);
        return wrapper;
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