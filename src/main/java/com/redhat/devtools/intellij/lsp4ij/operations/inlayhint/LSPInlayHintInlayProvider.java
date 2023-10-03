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
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4ij.AbstractLSPInlayProvider;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4ij.LanguageServiceAccessor;
import com.redhat.devtools.intellij.lsp4ij.internal.CancellationSupport;
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
import java.util.Objects;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
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
                Project project = psiElement.getProject();
                if (project.isDisposed()) {
                    // The project has been closed, don't collect inlay hints.
                    return false;
                }
                VirtualFile file = LSPIJUtils.getFile(psiFile);
                if (file == null) {
                    return false;
                }
                URI fileUri = LSPIJUtils.toUri(file);
                if (fileUri == null) {
                    return false;
                }
                Document document = editor.getDocument();
                final CancellationSupport cancellationSupport = new CancellationSupport();
                try {
                    Range viewPortRange = getViewPortRange(editor);
                    InlayHintParams param = new InlayHintParams(new TextDocumentIdentifier(fileUri.toASCIIString()), viewPortRange);
                    BlockingDeque<Pair<InlayHint, LanguageServer>> pairs = new LinkedBlockingDeque<>();

                    CompletableFuture<Void> future = collect(psiElement.getProject(), file, param, pairs, cancellationSupport);
                    List<Pair<Integer, Pair<InlayHint, LanguageServer>>> inlayHints = createInlayHints(document, pairs, future);
                    inlayHints.stream()
                            .collect(Collectors.groupingBy(p -> p.first))
                            .forEach((offset, list) ->
                                    inlayHintsSink.addInlineElement(offset, false, toPresentation(editor, list, getFactory()), false));
                } catch (ProcessCanceledException e) {
                    // Cancel all LSP requests
                    cancellationSupport.cancel();
                    return false; //throw e;
                } catch (InterruptedException e) {
                    LOGGER.warn(e.getLocalizedMessage(), e);
                    Thread.currentThread().interrupt();
                }
                return false;
            }

            @NotNull
            private List<Pair<Integer, Pair<InlayHint, LanguageServer>>> createInlayHints(
                    @NotNull Document document,
                    BlockingDeque<Pair<InlayHint, LanguageServer>> pairs,
                    CompletableFuture<Void> future)
                    throws InterruptedException {
                List<Pair<Integer, Pair<InlayHint, LanguageServer>>> inlayHints = new ArrayList<>();
                while (!future.isDone() || !pairs.isEmpty()) {
                    ProgressManager.checkCanceled();
                    Pair<InlayHint, LanguageServer> pair = pairs.poll(25, TimeUnit.MILLISECONDS);
                    if (pair != null) {
                        int offset = LSPIJUtils.toOffset(pair.getFirst().getPosition(), document);
                        inlayHints.add(Pair.create(offset, pair));
                    }
                }
                return inlayHints;
            }

            private CompletableFuture<Void> collect(@NotNull Project project, @NotNull VirtualFile file, InlayHintParams param, BlockingDeque<Pair<InlayHint, LanguageServer>> pairs, CancellationSupport cancellationSupport) {
                return LanguageServiceAccessor.getInstance(project)
                        .getLanguageServers(file, capabilities -> capabilities.getInlayHintProvider() != null)
                        .thenComposeAsync(languageServers -> cancellationSupport.execute(CompletableFuture.allOf(languageServers.stream()
                                .map(languageServer ->
                                        cancellationSupport.execute(languageServer.getServer().getTextDocumentService().inlayHint(param))
                                                .thenAcceptAsync(inlayHints -> {
                                                    // textDocument/codeLens may return null
                                                    if (inlayHints != null) {
                                                        inlayHints.stream().filter(Objects::nonNull)
                                                                .forEach(inlayHint -> pairs.add(new Pair(inlayHint, languageServer.getServer())));
                                                    }
                                                }))
                                .toArray(CompletableFuture[]::new))));
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

    private InlayPresentation toPresentation(Editor editor,
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
                    InlayPresentation text = createInlayPresentation(editor.getProject(), factory, p, index, part);
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

    @NotNull
    private InlayPresentation createInlayPresentation(
            Project project,
            PresentationFactory factory,
            Pair<Integer, Pair<InlayHint, LanguageServer>> p,
            int index,
            InlayHintLabelPart part) {
        InlayPresentation text = factory.smallText(part.getValue());
        if (hasCommand(part)) {
            // InlayHintLabelPart defines a Command, create a clickable inlay hint
            int finalIndex = index;
            text = factory.referenceOnHover(text, (event, translated) ->
                    executeClientCommand(p.second.second, p.second.first, finalIndex, (Component) event.getSource(), project)
            );
        }
        return text;
    }

    private static boolean hasCommand(InlayHintLabelPart part) {
        Command command = part.getCommand();
        return (command != null && command.getCommand() != null && !command.getCommand().isEmpty());
    }

    private void executeClientCommand(
            LanguageServer languageServer,
            InlayHint inlayHint,
            int index,
            Component source,
            Project project
    ) {
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
