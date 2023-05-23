/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.lsp4ij.operations.highlight;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerBase;
import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerFactory;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4ij.LanguageServiceAccessor;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LSPHighlightUsagesHandlerFactory implements HighlightUsagesHandlerFactory {
    private static final Logger LOGGER = Logger.getLogger(LSPHighlightUsagesHandlerFactory.class.getName());

    @Override
    public @Nullable HighlightUsagesHandlerBase createHighlightUsagesHandler(@NotNull Editor editor, @NotNull PsiFile file) {
        List<PsiElement> targets = getTargets(editor, file);
        return targets.isEmpty()?null:new LSPHighlightUsagesHandler(editor, file, targets);
    }

    private List<PsiElement> getTargets(Editor editor, PsiFile file) {
        List<PsiElement> elements = new ArrayList<>();
        try {
            int offset = TargetElementUtil.adjustOffset(file, editor.getDocument(), editor.getCaretModel().getOffset());
            Document document = editor.getDocument();
            Position position;
            position = LSPIJUtils.toPosition(offset, document);
            URI uri = LSPIJUtils.toUri(document);
            if(uri == null) {
                return Collections.emptyList();
            }
            ProgressManager.checkCanceled();
            TextDocumentIdentifier identifier = new TextDocumentIdentifier(uri.toString());
            DocumentHighlightParams params = new DocumentHighlightParams(identifier, position);
            BlockingDeque<DocumentHighlight> highlights = new LinkedBlockingDeque<>();
            CompletableFuture<Void> future = LanguageServiceAccessor.getInstance(editor.getProject()).getLanguageServers(document,
                            capabilities -> LSPIJUtils.hasCapability(capabilities.getDocumentHighlightProvider()))
                    .thenAcceptAsync(languageServers ->
                            CompletableFuture.allOf(languageServers.stream()
                                    .map(languageServer -> languageServer.getTextDocumentService().documentHighlight(params))
                                    .map(request -> request.thenAcceptAsync(result -> {
                                        if (result != null) {
                                            result.forEach(hightlight -> highlights.add(hightlight));
                                        }
                                    })).toArray(CompletableFuture[]::new)));
            while (!future.isDone() || !highlights.isEmpty()) {
                ProgressManager.checkCanceled();
                DocumentHighlight highlight = highlights.poll(25, TimeUnit.MILLISECONDS);
                if (highlight != null) {
                    int highlightOffset = LSPIJUtils.toOffset(highlight.getRange().getStart(), document);
                    PsiElement element = file.findElementAt(highlightOffset);
                    if (element != null) {
                        elements.add(element);
                    }
                }
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, e, e::getLocalizedMessage);
        }
        return elements;

    }
}
