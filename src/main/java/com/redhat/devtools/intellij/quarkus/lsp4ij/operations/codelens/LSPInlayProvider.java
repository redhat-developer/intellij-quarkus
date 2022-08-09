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
package com.redhat.devtools.intellij.quarkus.lsp4ij.operations.codelens;

import com.intellij.codeInsight.hints.ChangeListener;
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector;
import com.intellij.codeInsight.hints.ImmediateConfigurable;
import com.intellij.codeInsight.hints.InlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsProvider;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.NoSettings;
import com.intellij.codeInsight.hints.SettingsKey;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.MouseButton;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.codeInsight.hints.presentation.SequencePresentation;
import com.intellij.ide.DataManager;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.layout.LCFlags;
import com.intellij.ui.layout.LayoutKt;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageServiceAccessor;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
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

public class LSPInlayProvider implements InlayHintsProvider<NoSettings> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LSPInlayProvider.class);

    public static final DataKey<Command> LSP_COMMAND = DataKey.create("com.redhat.devtools.intellij.quarkus.lsp4ij.command");
    private static final long TIMEOUT = 5L;

    private SettingsKey<NoSettings> key = new SettingsKey<>("LSP.hints");

    @Override
    public boolean isVisibleInSettings() {
        return true;
    }

    @NotNull
    @Override
    public SettingsKey<NoSettings> getKey() {
        return key;
    }

    @NotNull
    @Override
    public String getName() {
        return "LSP";
    }

    @Nullable
    @Override
    public String getPreviewText() {
        return "Preview";
    }

    @NotNull
    @Override
    public ImmediateConfigurable createConfigurable(@NotNull NoSettings o) {
        return new ImmediateConfigurable() {
            @NotNull
            @Override
            public JComponent createComponent(@NotNull ChangeListener changeListener) {
                return LayoutKt.panel(new LCFlags[0], "LSP", builder -> {
                    return null;
                });
            }
        };
    }

    @NotNull
    @Override
    public NoSettings createSettings() {
        return new NoSettings();
    }

    @Nullable
    @Override
    public InlayHintsCollector getCollectorFor(@NotNull PsiFile psiFile, @NotNull Editor editor, @NotNull NoSettings o, @NotNull InlayHintsSink inlayHintsSink) {return new FactoryInlayHintsCollector(editor) {
            @Override
            public boolean collect(@NotNull PsiElement psiElement, @NotNull Editor editor, @NotNull InlayHintsSink inlayHintsSink) {
                try {
                    URI docURI = LSPIJUtils.toUri(editor.getDocument());
                    if (docURI != null) {
                        CodeLensParams param = new CodeLensParams(new TextDocumentIdentifier(docURI.toString()));
                        BlockingDeque<Pair<CodeLens, LanguageServer>> pairs = new LinkedBlockingDeque<>();
                        List<Pair<Integer,Pair<CodeLens, LanguageServer>>> codelenses = new ArrayList<>();
                        CompletableFuture<Void> future = LanguageServiceAccessor.getInstance(psiElement.getProject())
                                .getLanguageServers(editor.getDocument(), capabilities -> capabilities.getCodeLensProvider() != null)
                                .thenComposeAsync(languageServers -> CompletableFuture.allOf(languageServers.stream()
                                        .map(languageServer -> languageServer.getTextDocumentService().codeLens(param)
                                                .thenAcceptAsync(codeLenses -> {
                                                    // textDocument/codeLens may return null
                                                    if (codeLenses != null) {
                                                        codeLenses.stream().filter(Objects::nonNull)
                                                                .forEach(codeLens -> pairs.add(new Pair(codeLens, languageServer)));
                                                    }
                                                }))
                                        .toArray(CompletableFuture[]::new)));
                        while (!future.isDone() || !pairs.isEmpty()) {
                            ProgressManager.checkCanceled();
                            Pair<CodeLens, LanguageServer> pair = pairs.poll(25, TimeUnit.MILLISECONDS);
                            if (pair != null) {
                                int offset = LSPIJUtils.toOffset(pair.getFirst().getRange().getStart(), editor.getDocument());
                                codelenses.add(Pair.create(offset, pair));
                            }
                        }
                        Map<Integer, List<Pair<Integer,Pair<CodeLens, LanguageServer>>>> elements = codelenses.stream().collect(Collectors.groupingBy(p -> p.first));
                        elements.forEach((offset,list) -> inlayHintsSink.addBlockElement(offset, true,
                                true, 0, toPresentation(editor, offset, list, getFactory())));
                        //inlayHintsSink.addBlockElement(offset, true, true, 0, toPresentation(editor, offset, pair.getSecond(), getFactory(), pair.getFirst()));
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
                                             List<Pair<Integer, Pair<CodeLens, LanguageServer>>> elements,
                                             PresentationFactory factory) {
        int line = editor.getDocument().getLineNumber(offset);
        int column = offset - editor.getDocument().getLineStartOffset(line);
        List<InlayPresentation> presentations = new ArrayList<>();
        presentations.add(factory.textSpacePlaceholder(column, true));
        elements.forEach(p -> {
            presentations.add(factory.onClick(factory.text(getCodeLensString(p.second.first)), MouseButton.Left, (event, point) -> {
                executeClientCommand(p.second.second, p.second.first, (Component) event.getSource(), editor.getProject());
                return null;
            }));
            presentations.add(factory.textSpacePlaceholder(1, true));
        });
        return new SequencePresentation(presentations);
    }

    private void executeClientCommand(LanguageServer languageServer, CodeLens codeLens, Component source, Project project) {
        if (LanguageServiceAccessor.getInstance(project).checkCapability(languageServer,
                capabilites -> Boolean.TRUE.equals(capabilites.getCodeLensProvider().getResolveProvider()))) {
            languageServer.getTextDocumentService().resolveCodeLens(codeLens).thenAcceptAsync(resolvedCodeLens -> {
                executeClientCommand(source, resolvedCodeLens);
            });
        } else {
            executeClientCommand(source, codeLens);
        }
    }

    private void executeClientCommand(Component source, CodeLens resolvedCodeLens) {
        if (resolvedCodeLens.getCommand() != null) {
            AnAction action = ActionManager.getInstance().getAction(resolvedCodeLens.getCommand().getCommand());
            if (action != null) {
                DataContext context = SimpleDataContext.getSimpleContext(LSP_COMMAND.getName(), resolvedCodeLens.getCommand(), DataManager.getInstance().getDataContext(source));
                action.actionPerformed(new AnActionEvent(null, context,
                        ActionPlaces.UNKNOWN, new Presentation(),
                        ActionManager.getInstance(), 0));
            }
        }
    }

    private String getCodeLensString(CodeLens codeLens) {
        Command command = codeLens.getCommand();
        if (command == null || command.getTitle().isEmpty()) {
            return null;
        }
        return command.getTitle();
    }

    @Override
    public boolean isLanguageSupported(@NotNull Language language) {
        return true;
    }
}
