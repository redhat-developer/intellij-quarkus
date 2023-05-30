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
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4ij.LSPVirtualFileWrapper;
import com.redhat.devtools.intellij.lsp4ij.LanguageServiceAccessor;
import org.eclipse.lsp4j.DocumentLinkParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
            DocumentLinkParams params = new DocumentLinkParams(LSPIJUtils.toTextDocumentIdentifier(uri));
            try {
                LanguageServiceAccessor.getInstance(editor.getProject())
                        .getLanguageServers(editor.getDocument(), capabilities -> capabilities.getDocumentLinkProvider() != null)
                        .thenComposeAsync(servers ->
                                CompletableFuture.allOf(
                                        servers.stream()
                                                .map(server ->
                                                        server.getSecond().getTextDocumentService().documentLink(params)
                                                                .thenAcceptAsync(documentLinks -> {
                                                                            LSPVirtualFileWrapper.getLSPVirtualFileWrapper(file.getVirtualFile())
                                                                                    .updateDocumentLink(documentLinks, server.getFirst());
                                                                        }
                                                                )
                                                )
                                                .toArray(CompletableFuture[]::new))
                        ).get(1_000, TimeUnit.MILLISECONDS);
            } catch (ExecutionException | TimeoutException e) {
                LOGGER.warn(e.getLocalizedMessage(), e);
            }
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
