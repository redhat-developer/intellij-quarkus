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
package com.redhat.devtools.intellij.lsp4ij.operations.inlayhint;

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
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * LSP textDocument/inlayHint support.
 */
public class LSPInlayHintInlayProvider extends AbstractLSPInlayProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(LSPInlayHintInlayProvider.class);

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
                    URI docURI = LSPIJUtils.toUri(editor.getDocument());
                    if (docURI != null) {
                        Range viewPortRange = getViewPortRange(editor);
                        InlayHintParams param = new InlayHintParams(new TextDocumentIdentifier(docURI.toString()), viewPortRange);
                        BlockingDeque<Pair<InlayHint, LanguageServer>> pairs = new LinkedBlockingDeque<>();
                        List<Pair<Integer, Pair<InlayHint, LanguageServer>>> inlayhints = new ArrayList<>();
                        CompletableFuture<Void> future = LanguageServiceAccessor.getInstance(psiElement.getProject())
                                .getLanguageServers(editor.getDocument(), capabilities -> capabilities.getInlayHintProvider() != null)
                                .thenComposeAsync(languageServers -> CompletableFuture.allOf(languageServers.stream()
                                        .map(languageServer -> languageServer.getSecond().getTextDocumentService().inlayHint(param)
                                                .thenAcceptAsync(inlayHints -> {
                                                    // textDocument/codeLens may return null
                                                    if (inlayHints != null) {
                                                        inlayHints.stream().filter(Objects::nonNull)
                                                                .forEach(inlayHint -> pairs.add(new Pair(inlayHint, languageServer.getSecond())));
                                                    }
                                                }))
                                        .toArray(CompletableFuture[]::new)));
                        while (!future.isDone() || !pairs.isEmpty()) {
                            ProgressManager.checkCanceled();
                            Pair<InlayHint, LanguageServer> pair = pairs.poll(25, TimeUnit.MILLISECONDS);
                            if (pair != null) {
                                int offset = LSPIJUtils.toOffset(pair.getFirst().getPosition(), editor.getDocument());
                                inlayhints.add(Pair.create(offset, pair));
                            }
                        }
                        Map<Integer, List<Pair<Integer, Pair<InlayHint, LanguageServer>>>> elements = inlayhints.stream().collect(Collectors.groupingBy(p -> p.first));
                        elements.forEach((offset, list) -> inlayHintsSink.addInlineElement(offset, false,
                                toPresentation(editor, offset, list, getFactory()), false));
                    }
                } catch (InterruptedException e) {
                    LOGGER.warn(e.getLocalizedMessage(), e);
                    Thread.currentThread().interrupt();
                }
                return false;
            }
        };
    }

    @NotNull
    private static Range getViewPortRange(Editor editor) {
        // LSP textDocument/inlayHnt request parameter expects to fill the visible view port range.
        // As Intellij inlay hint provider is refreshed just only when editor is opened or editor content changed
        // and not when editor is scrolling, the view port range must be created with full text document offsets.
        Position start = new Position(0, 0);
        Document document = editor.getDocument();
        Position end = LSPIJUtils.toPosition(document.getTextLength(), document);
        return new Range(start, end);
    }

    private InlayPresentation toPresentation(Editor editor, int offset,
                                             List<Pair<Integer, Pair<InlayHint, LanguageServer>>> elements,
                                             PresentationFactory factory) {
        List<InlayPresentation> presentations = new ArrayList<>();
        elements.forEach(p -> {
            Either<String, List<InlayHintLabelPart>> label = p.second.first.getLabel();
            if (label.isLeft()) {
                presentations.add(factory.smallText(label.getLeft()));
            } else {
                int index = 0;
                for (InlayHintLabelPart part : label.getRight()) {
                    InlayPresentation text = factory.smallText(part.getValue());
                    if (!hasCommand(part)) {
                        // No command, create a simple text inlay hint
                        presentations.add(text);
                    } else {
                        // InlayHintLabelPart defines a Command, create a clickable inlay hint
                        int finalIndex = index;
                        text = factory.referenceOnHover(text, (event, translated) -> {
                            executeClientCommand(p.second.second, p.second.first, finalIndex, (Component) event.getSource(), editor.getProject());
                        });
                    }
                    if (part.getTooltip() != null && part.getTooltip().isLeft()) {
                        text = factory.withTooltip(part.getTooltip().getLeft(), text);
                    }
                    presentations.add(text);
                    index++;
                }
            }
        });
        return factory.roundWithBackground(new SequencePresentation(presentations));
    }

    private static boolean hasCommand(InlayHintLabelPart part) {
        Command command = part.getCommand();
        return (command != null && command.getCommand() != null && !command.getCommand().isEmpty());
    }

    private void executeClientCommand(LanguageServer languageServer, InlayHint inlayHint, int index, Component source,
                                      Project project) {
        if (LanguageServiceAccessor.getInstance(project)
                .checkCapability(languageServer, capabilites -> isResolveSupported(capabilites.getInlayHintProvider()))) {
            languageServer.getTextDocumentService()
                    .resolveInlayHint(inlayHint)
                    .thenAcceptAsync(resolvedInlayHint -> {
                        executeClientCommand(source, resolvedInlayHint.getLabel().getRight().get(index).getCommand());
                    });
        } else {
            executeClientCommand(source, inlayHint.getLabel().getRight().get(index).getCommand());
        }
    }

    private static boolean isResolveSupported(Either<Boolean, InlayHintRegistrationOptions> provider) {
        return provider.isRight() && provider.getRight().getResolveProvider();
    }

}
