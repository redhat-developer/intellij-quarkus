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
package com.redhat.devtools.intellij.quarkus.lsp4ij.operations.codeactions;

import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageServerWrapper;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageServiceAccessor;
import com.redhat.devtools.intellij.quarkus.lsp4ij.operations.diagnostics.LSPDiagnosticsToMarkers;
import com.redhat.devtools.intellij.quarkus.lsp4ij.operations.diagnostics.LSPLocalInspectionTool;
import com.redhat.devtools.intellij.quarkus.lsp4ij.operations.diagnostics.LSPPSiElement;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LSPCodeActionAnnotator extends ExternalAnnotator<LSPCodeActionAnnotator.Info, LSPCodeActionAnnotator.Info> {
    private static final Logger LOGGER = Logger.getLogger(LSPCodeActionAnnotator.class.getName());

    public class Info {
        VirtualFile file;
        Map<LanguageServerWrapper, Collection<ItemInfo>> itemInfos = new HashMap<>();
    }
    public class ItemInfo {
        private List<Either<Command, CodeAction>> actions;
        private PsiElement element;
        private RangeHighlighter highlighter;

        public ItemInfo(PsiElement element, RangeHighlighter highlighter) {
            this.element = element;
            this.highlighter = highlighter;
        }
    }

    @Override
    public @Nullable Info collectInformation(@NotNull PsiFile file) {
        return doCollectInformation(file, null);
    }

    @Override
    public @Nullable Info collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        return doCollectInformation(file, editor);
    }

    protected Info doCollectInformation(PsiFile file, Editor editor) {
        Info info = null;
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile != null) {
            if (editor == null) {
                editor = LSPIJUtils.editorForFile(virtualFile);
            }
            if (editor != null) {
                info = new Info();
                info.file = virtualFile;
                try {
                    for (LanguageServerWrapper wrapper : LanguageServiceAccessor.getInstance(file.getProject()).getLSWrappers(virtualFile, capabilities -> true)) {
                        RangeHighlighter[] highlighters = LSPDiagnosticsToMarkers.getMarkers(editor, wrapper.serverDefinition.id);
                        if (highlighters != null) {
                            for (RangeHighlighter highlighter : highlighters) {
                                PsiElement element;
                                if (highlighter.getEndOffset() - highlighter.getStartOffset() > 0) {
                                    element = new LSPPSiElement(editor.getProject(), file, highlighter.getStartOffset(), highlighter.getEndOffset(), editor.getDocument().getText(new TextRange(highlighter.getStartOffset(), highlighter.getEndOffset())));
                                } else {
                                    element = PsiUtilCore.getElementAtOffset(file, highlighter.getStartOffset());
                                }
                                Collection<ItemInfo> itemInfos = info.itemInfos.get(wrapper);
                                if (itemInfos == null) {
                                    itemInfos = new ArrayList<>();
                                    info.itemInfos.put(wrapper, itemInfos);
                                }
                                itemInfos.add(new ItemInfo(element, highlighter));
                            }
                        }
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, e, e::getLocalizedMessage);
                }
            }
        }
        return info;
    }

    @Override
    public @Nullable Info doAnnotate(Info collectedInfo) {
        Collection<CompletableFuture<?>> futures = new ArrayList<>();
        for(Map.Entry<LanguageServerWrapper, Collection<ItemInfo>> entry : collectedInfo.itemInfos.entrySet()) {
            if (supportsCodeAction(entry.getKey())) {
                for(ItemInfo itemInfo : entry.getValue()) {
                    Diagnostic diagnostic = (Diagnostic) itemInfo.highlighter.getErrorStripeTooltip();
                    CodeActionContext context = new CodeActionContext(Collections.singletonList(diagnostic));
                    CodeActionParams params = new CodeActionParams();
                    params.setContext(context);
                    params.setTextDocument(new TextDocumentIdentifier(LSPIJUtils.toUri(collectedInfo.file).toString()));
                    params.setRange(diagnostic.getRange());
                    CompletableFuture<List<Either<Command, CodeAction>>> codeAction = entry.getKey().getInitializedServer().thenComposeAsync(server -> server.getTextDocumentService().codeAction(params));
                    futures.add(codeAction);
                    codeAction.thenAcceptAsync(actions -> itemInfo.actions = actions);
                }
            }
        }
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.log(Level.WARNING, e, e::getLocalizedMessage);
        }
        return collectedInfo;
    }

    private boolean supportsCodeAction(LanguageServerWrapper wrapper) {
        ServerCapabilities capabilities = wrapper.getServerCapabilities();
        return capabilities != null && capabilities.getCodeActionProvider() != null &&
                ((capabilities.getCodeActionProvider().isLeft() &&
                        Boolean.TRUE.equals(capabilities.getCodeActionProvider().getLeft())) || capabilities.getCodeActionProvider().isRight());
    }

    @Override
    public void apply(@NotNull PsiFile file, Info annotationResult, @NotNull AnnotationHolder holder) {
        for(Map.Entry<LanguageServerWrapper, Collection<ItemInfo>> entry : annotationResult.itemInfos.entrySet()) {
            for(ItemInfo itemInfo : entry.getValue()) {
                Diagnostic diagnostic = (Diagnostic) itemInfo.highlighter.getErrorStripeTooltip();
                AnnotationBuilder builder = holder.newAnnotation(getHighlighType(diagnostic.getSeverity()),
                        diagnostic.getMessage()).range(itemInfo.element).tooltip(diagnostic.getMessage());
                if (itemInfo.actions != null) {
                    for(Either<Command, CodeAction> action : itemInfo.actions) {
                        builder = builder.withFix(new LSPCodeActionIntentionAction(action, entry.getKey()));
                    }
                }
                builder.create();
            }
        }
    }

    private HighlightSeverity getHighlighType(DiagnosticSeverity severity) {
        switch (severity) {
            case Error:
                return HighlightSeverity.ERROR;
            case Hint:
            case Information:
                return HighlightSeverity.INFORMATION;
            case Warning:
                return HighlightSeverity.WARNING;
        }
        return HighlightSeverity.INFORMATION;
    }

    @Override
    public String getPairedBatchInspectionShortName() {
        return LSPLocalInspectionTool.class.getSimpleName();
    }
}
