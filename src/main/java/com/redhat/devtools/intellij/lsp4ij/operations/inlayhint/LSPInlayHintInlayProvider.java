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
import com.intellij.codeInsight.hints.presentation.MouseButton;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.codeInsight.hints.presentation.SequencePresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4ij.AbstractLSPInlayProvider;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4ij.LanguageServiceAccessor;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintLabelPart;
import org.eclipse.lsp4j.InlayHintParams;
import org.eclipse.lsp4j.InlayHintRegistrationOptions;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
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
import java.util.function.Function;
import java.util.stream.Collectors;

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
                        Range viewPortRange = new Range(new Position(0, 0), new Position(0,0));
                        InlayHintParams param = new InlayHintParams(new TextDocumentIdentifier(docURI.toString()), viewPortRange);
                        BlockingDeque<Pair<InlayHint, LanguageServer>> pairs = new LinkedBlockingDeque<>();
                        List<Pair<Integer,Pair<InlayHint, LanguageServer>>> inlayhints = new ArrayList<>();
                        CompletableFuture<Void> future = LanguageServiceAccessor.getInstance(psiElement.getProject())
                                .getLanguageServers(editor.getDocument(), capabilities -> capabilities.getInlayHintProvider() != null)
                                .thenComposeAsync(languageServers -> CompletableFuture.allOf(languageServers.stream()
                                        .map(languageServer -> languageServer.getTextDocumentService().inlayHint(param)
                                                .thenAcceptAsync(inlayHints -> {
                                                    // textDocument/codeLens may return null
                                                    if (inlayHints != null) {
                                                        inlayHints.stream().filter(Objects::nonNull)
                                                                .forEach(inlayHint -> pairs.add(new Pair(inlayHint, languageServer)));
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
                        Map<Integer, List<Pair<Integer,Pair<InlayHint, LanguageServer>>>> elements = inlayhints.stream().collect(Collectors.groupingBy(p -> p.first));
                        elements.forEach((offset,list) -> inlayHintsSink.addInlineElement(offset, false,
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
                for(InlayHintLabelPart part : label.getRight()) {
                    InlayPresentation presentation = factory.smallText(part.getValue());
                    if (part.getCommand() != null) {
                        int finalIndex = index;
                        presentation = factory.onClick(presentation, MouseButton.Left, (event, point) -> {
                            executeClientCommand(p.second.second, p.second.first, finalIndex, (Component) event.getSource(), editor.getProject());
                            return null;
                        });
                        if (part.getTooltip() != null && part.getTooltip().isLeft()) {
                            presentation = factory.withTooltip(part.getTooltip().getLeft(), presentation);
                        }
                    }
                    presentations.add(presentation);
                    index++;
                }
            }
        });
        return factory.roundWithBackground(new SequencePresentation(presentations));
    }

    private void executeClientCommand(LanguageServer languageServer, InlayHint inlayHint, int index, Component source,
                                      Project project) {
        if (LanguageServiceAccessor.getInstance(project).checkCapability(languageServer,
                capabilites -> isResolveSupported(capabilites.getInlayHintProvider()))) {
            languageServer.getTextDocumentService().resolveInlayHint(inlayHint).thenAcceptAsync(resolvedInlayHint -> {
                executeClientCommand(source, resolvedInlayHint.getLabel().getRight().get(index).getCommand());
            });
        } else {
            executeClientCommand(source, inlayHint.getLabel().getRight().get(index).getCommand());
        }
    }

    private boolean isResolveSupported(Either<Boolean, InlayHintRegistrationOptions> provider) {
        return provider.isRight() && provider.getRight().getResolveProvider();
    }


    private String getInlayHintString(InlayHint inlayHint) {
        Either<String, List<InlayHintLabelPart>> label = inlayHint.getLabel();
        return label.map(Function.identity(), parts -> {
           return parts==null?null:parts.stream().map(InlayHintLabelPart::getValue).collect(Collectors.joining());
        });
    }
}
