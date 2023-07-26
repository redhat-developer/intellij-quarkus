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
package com.redhat.devtools.intellij.lsp4ij.operations.documentLink;

import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4ij.LSPVirtualFileWrapper;
import com.redhat.devtools.intellij.lsp4ij.LanguageServerWrapper;
import com.redhat.devtools.intellij.lsp4ij.LanguageServiceAccessor;
import com.redhat.devtools.intellij.lsp4ij.operations.highlight.LSPHighlightPsiElement;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentLinkParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * Intellij {@link ExternalAnnotator} implementation which collect LSP document links and display them with underline style.
 */
public class LSPDocumentLinkAnnotator extends ExternalAnnotator<LSPVirtualFileWrapper, LSPVirtualFileWrapper> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LSPDocumentLinkAnnotator.class);

    @Nullable
    @Override
    public LSPVirtualFileWrapper collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        URI uri = LSPIJUtils.toUri(editor.getDocument());
        if (uri == null) {
            return null;
        }
        try {
            ProgressManager.checkCanceled();
            DocumentLinkParams params = new DocumentLinkParams(LSPIJUtils.toTextDocumentIdentifier(uri));
            Document document = editor.getDocument();

            BlockingDeque<Pair<List<DocumentLink>, LanguageServerWrapper>> documentLinks = new LinkedBlockingDeque<>();
            CompletableFuture<Void> future = LanguageServiceAccessor.getInstance(editor.getProject()).getLanguageServers(document,
                    capabilities -> capabilities.getDocumentLinkProvider() != null)
                    .thenAcceptAsync(languageServers ->
                            CompletableFuture.allOf(languageServers.stream()
                                    .map(languageServer -> Pair.pair(languageServer.getSecond().getTextDocumentService().documentLink(params), languageServer.getFirst()))
                                    .map(request -> request.getFirst().thenAcceptAsync(result -> {
                                        if (result != null) {
                                            documentLinks.add(Pair.pair(result, request.getSecond()));
                                        }
                                    })).toArray(CompletableFuture[]::new)));
            while (!future.isDone() || !documentLinks.isEmpty()) {
                ProgressManager.checkCanceled();
                Pair<List<DocumentLink>, LanguageServerWrapper> links = documentLinks.poll(25, TimeUnit.MILLISECONDS);
                if (links != null) {
                    LSPVirtualFileWrapper.getLSPVirtualFileWrapper(file.getVirtualFile())
                            .updateDocumentLink(links.getFirst(), links.getSecond());
                }
            }

        } catch (ProcessCanceledException cancellation) {
            throw cancellation;
        } catch (InterruptedException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
            Thread.currentThread().interrupt();
        }
        return LSPVirtualFileWrapper.getLSPVirtualFileWrapper(file.getVirtualFile());
    }

    @Override
    public @Nullable LSPVirtualFileWrapper doAnnotate(LSPVirtualFileWrapper wrapper) {
        return wrapper;
    }

    @Override
    public void apply(@NotNull PsiFile file, LSPVirtualFileWrapper wrapper, @NotNull AnnotationHolder holder) {
        if (wrapper == null) {
            return;
        }
        Document document = LSPIJUtils.getDocument(wrapper.getFile());
        for (var documentLinkPerServer : wrapper.getAllDocumentLink()) {
            for (var documentLink : documentLinkPerServer.getDocumentLinks()) {
                TextRange range = LSPIJUtils.toTextRange(documentLink.getRange(), document);
                holder.newSilentAnnotation(HighlightInfoType.HIGHLIGHTED_REFERENCE_SEVERITY)
                        .range(range)
                        .textAttributes(DefaultLanguageHighlighterColors.HIGHLIGHTED_REFERENCE)
                        .create();
            }
        }
    }
}
