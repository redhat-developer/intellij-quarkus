/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.lsp4ij.operations.codelens;

import com.intellij.codeInsight.hints.FactoryInlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.NoSettings;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.codeInsight.hints.presentation.SequencePresentation;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4ij.AbstractLSPInlayProvider;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4ij.LanguageServiceAccessor;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * LSP textDocument/codeLens support.
 */
public class LSPCodelensInlayProvider extends AbstractLSPInlayProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(LSPCodelensInlayProvider.class);

    @Nullable
    @Override
    public InlayHintsCollector getCollectorFor(@NotNull PsiFile psiFile,
                                               @NotNull Editor editor,
                                               @NotNull NoSettings o,
                                               @NotNull InlayHintsSink inlayHintsSink) {
        return new FactoryInlayHintsCollector(editor) {
            @Override
            public boolean collect(@NotNull PsiElement psiElement, @NotNull Editor editor, @NotNull InlayHintsSink inlayHintsSink) {
                try {
                    Document document = editor.getDocument();
                    Project project = psiElement.getProject();
                    if (project.isDisposed()) {
                        // The project has been closed, don't collect code lenses.
                        return false;
                    }
                    URI docURI = LSPIJUtils.toUri(document);
                    if (docURI != null) {
                        CodeLensParams param = new CodeLensParams(new TextDocumentIdentifier(docURI.toString()));
                        BlockingDeque<Pair<CodeLens, LanguageServer>> pairs = new LinkedBlockingDeque<>();
                        CompletableFuture<Void> future = collect(document, project, param, pairs);
                        List<Pair<Integer, Pair<CodeLens, LanguageServer>>> codeLenses = createCodeLenses(document, pairs, future);
                        Map<Integer, List<Pair<Integer, Pair<CodeLens, LanguageServer>>>> elements = codeLenses.stream().collect(Collectors.groupingBy(p -> p.first));
                        elements.forEach((offset, list) ->
                                inlayHintsSink.addBlockElement(
                                        offset,
                                        true,
                                        true,
                                        0,
                                        toPresentation(editor, offset, list, getFactory()))
                        );
                    }
                } catch (InterruptedException e) {
                    LOGGER.warn(e.getLocalizedMessage(), e);
                    Thread.currentThread().interrupt();
                }
                return false;
            }

            @NotNull
            private List<Pair<Integer, Pair<CodeLens, LanguageServer>>> createCodeLenses(Document document, BlockingDeque<Pair<CodeLens, LanguageServer>> pairs, CompletableFuture<Void> future) throws InterruptedException {
                List<Pair<Integer, Pair<CodeLens, LanguageServer>>> codelenses = new ArrayList<>();
                while (!future.isDone() || !pairs.isEmpty()) {
                    ProgressManager.checkCanceled();
                    Pair<CodeLens, LanguageServer> pair = pairs.poll(25, TimeUnit.MILLISECONDS);
                    if (pair != null) {
                        int offset = LSPIJUtils.toOffset(pair.getFirst().getRange().getStart(), document);
                        codelenses.add(Pair.create(offset, pair));
                    }
                }
                return codelenses;
            }

            private CompletableFuture<Void> collect(Document document, Project project, CodeLensParams param, BlockingDeque<Pair<CodeLens, LanguageServer>> pairs) {
                return LanguageServiceAccessor.getInstance(project)
                        .getLanguageServers(document, capabilities -> capabilities.getCodeLensProvider() != null)
                        .thenComposeAsync(languageServers -> CompletableFuture.allOf(languageServers.stream()
                                .map(languageServer -> languageServer.getSecond().getTextDocumentService().codeLens(param)
                                        .thenAcceptAsync(codeLenses -> {
                                            // textDocument/codeLens may return null
                                            if (codeLenses != null) {
                                                codeLenses.stream().filter(Objects::nonNull)
                                                        .forEach(codeLens -> pairs.add(new Pair(codeLens, languageServer.getSecond())));
                                            }
                                        }))
                                .toArray(CompletableFuture[]::new)));
            }
        };
    }

    private InlayPresentation toPresentation(
            Editor editor,
            int offset,
            List<Pair<Integer, Pair<CodeLens, LanguageServer>>> elements,
            PresentationFactory factory
    ) {
        int line = editor.getDocument().getLineNumber(offset);
        int column = offset - editor.getDocument().getLineStartOffset(line);
        List<InlayPresentation> presentations = new ArrayList<>();
        presentations.add(factory.textSpacePlaceholder(column, true));
        elements.forEach(p -> {
            CodeLens codeLens = p.second.first;
            LanguageServer languageServer = p.second.second;
            InlayPresentation text = factory.smallText(getCodeLensString(codeLens));
            if (!hasCommand(codeLens)) {
                // No command, create a simple text inlay hint
                presentations.add(text);
            } else {
                // Codelens defines a Command, create a clickable inlay hint
                InlayPresentation clickableText = factory.referenceOnHover(text, (event, translated) ->
                    executeClientCommand(p.second.second, p.second.first, (Component) event.getSource(), editor.getProject())
                );
                presentations.add(clickableText);
            }
            presentations.add(factory.textSpacePlaceholder(1, true));
        });
        return new SequencePresentation(presentations);
    }

    private void executeClientCommand(LanguageServer languageServer, CodeLens codeLens, Component source, Project project) {
        if (LanguageServiceAccessor.getInstance(project).checkCapability(languageServer, capabilities ->
                        Boolean.TRUE.equals(capabilities.getCodeLensProvider().getResolveProvider()))
        ) {
            languageServer.getTextDocumentService().resolveCodeLens(codeLens).thenAcceptAsync(resolvedCodeLens ->
                executeClientCommand(source, resolvedCodeLens.getCommand())
            );
        } else {
            executeClientCommand(source, codeLens.getCommand());
        }
    }

    private static boolean hasCommand(CodeLens codeLens) {
        Command command = codeLens.getCommand();
        return (command != null && command.getCommand() != null && !command.getCommand().isEmpty());
    }

    private static String getCodeLensString(CodeLens codeLens) {
        Command command = codeLens.getCommand();
        if (command == null || command.getTitle().isEmpty()) {
            return null;
        }
        return command.getTitle();
    }
}
