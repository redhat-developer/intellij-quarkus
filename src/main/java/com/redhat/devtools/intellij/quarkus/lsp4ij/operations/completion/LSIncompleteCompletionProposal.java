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

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.editor.Document;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;

public class LSIncompleteCompletionProposal extends LookupElement {
    private final CompletionItem item;
    private final Document document;
    private final LanguageServer languageServer;
    private final int initialOffset;
    private final int currentOffset;

    public LSIncompleteCompletionProposal(Document document, int offset, CompletionItem item, LanguageServer languageServer) {
        this.item = item;
        this.document = document;
        this.languageServer = languageServer;
        this.initialOffset = offset;
        this.currentOffset = offset;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return item.getLabel();
    }

    private boolean isDeprecated() {
        return item.getDeprecated() != null && item.getDeprecated().booleanValue();
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(item.getLabel());
        if (isDeprecated()) {
            presentation.setStrikeout(true);
        }
    }

    @Override
    public void handleInsert(@NotNull InsertionContext context) {
    }
}
