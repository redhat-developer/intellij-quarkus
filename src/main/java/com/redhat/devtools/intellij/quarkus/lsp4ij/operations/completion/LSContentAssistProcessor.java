/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.lsp4ij.operations.completion;

import com.google.common.base.Strings;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageServiceAccessor;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class LSContentAssistProcessor extends CompletionContributor {
    private static final Logger LOGGER = LoggerFactory.getLogger(LSContentAssistProcessor.class);

    private CompletableFuture<List<LanguageServer>> completionLanguageServersFuture;
    private final Object completionTriggerCharsSemaphore = new Object();
    private char[] completionTriggerChars = new char[0];

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        Document document = parameters.getEditor().getDocument();
        Editor editor = parameters.getEditor();
        Project project = parameters.getOriginalFile().getProject();
        int offset = parameters.getOffset();
        initiateLanguageServers(project, document);
        CompletionParams param;
        try {
            param = LSPIJUtils.toCompletionParams(LSPIJUtils.toUri(document), offset, document);
            List<LookupElement> proposals = Collections.synchronizedList(new ArrayList<>());
            this.completionLanguageServersFuture
                    .thenComposeAsync(languageServers -> CompletableFuture.allOf(languageServers.stream()
                            .map(languageServer -> languageServer.getTextDocumentService().completion(param)
                                    .thenAcceptAsync(completion -> proposals
                                            .addAll(toProposals(project, editor, document, offset, completion, languageServer))))
                            .toArray(CompletableFuture[]::new)))
                    .get();
            result.addAllElements(proposals);
        } catch (RuntimeException | InterruptedException | ExecutionException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
            result.addElement(createErrorProposal(offset, e));
        }
        super.fillCompletionVariants(parameters, result);
    }

    private Collection<? extends LookupElement> toProposals(Project project, Editor editor, Document document, int offset, Either<List<CompletionItem>, CompletionList> completion, LanguageServer languageServer) {
        List<CompletionItem> items = completion.isLeft()?completion.getLeft():completion.getRight().getItems();
        boolean isIncomplete = completion.isLeft()?false:completion.getRight().isIncomplete();
        return items.stream().map(item -> createLookupItem(project, editor, offset, item, isIncomplete, languageServer)).
                filter(item -> item.validate(document, offset, null)).
                collect(Collectors.toList());
    }

    private LSIncompleteCompletionProposal createLookupItem(Project project, Editor editor, int offset,
                                                            CompletionItem item, boolean isIncomplete,
                                                            LanguageServer languageServer) {
        return isIncomplete?new LSIncompleteCompletionProposal(editor, offset, item, languageServer):
                new LSCompletionProposal(editor, offset, item, languageServer);
    }


    private LookupElement createErrorProposal(int offset, Exception ex) {
        return LookupElementBuilder.create("Error while computing completion", "");
    }

    private void initiateLanguageServers(Project project, Document document) {
        if (this.completionLanguageServersFuture != null) {
            try {
                this.completionLanguageServersFuture.cancel(true);
            } catch (CancellationException ex) {
                // nothing
            }
        }
        this.completionTriggerChars = new char[0];

        this.completionLanguageServersFuture = LanguageServiceAccessor.getInstance(project).getLanguageServers(document,
                capabilities -> {
                    CompletionOptions provider = capabilities.getCompletionProvider();
                    if (provider != null) {
                        synchronized (this.completionTriggerCharsSemaphore) {
                            this.completionTriggerChars = mergeTriggers(this.completionTriggerChars,
                                    provider.getTriggerCharacters());
                        }
                        return true;
                    }
                    return false;
                });
    }

    private static char[] mergeTriggers(char[] initialArray, Collection<String> additionalTriggers) {
        if (initialArray == null) {
            initialArray = new char[0];
        }
        if (additionalTriggers == null) {
            additionalTriggers = Collections.emptySet();
        }
        Set<Character> triggers = new HashSet<>();
        for (char c : initialArray) {
            triggers.add(Character.valueOf(c));
        }
        additionalTriggers.stream().filter(s -> !Strings.isNullOrEmpty(s))
                .map(triggerChar -> Character.valueOf(triggerChar.charAt(0))).forEach(triggers::add);
        char[] res = new char[triggers.size()];
        int i = 0;
        for (Character c : triggers) {
            res[i] = c.charValue();
            i++;
        }
        return res;
    }
}
