/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.lsp4ij.operations.completion;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LSCompletionProposal extends LSIncompleteCompletionProposal {
    private static final Logger LOGGER = LoggerFactory.getLogger(LSCompletionProposal.class);

    public LSCompletionProposal(Editor editor, int offset, CompletionItem item, LanguageServer languageServer) {
        super(editor, offset, item, languageServer);
    }

    @Override
    public boolean validate(Document document, int offset, DocumentEvent event) {
        if (item.getLabel() == null || item.getLabel().isEmpty()) {
            return false;
        }
        if (offset < this.bestOffset) {
            return false;
        }
        try {
            String documentFilter = getDocumentFilter(offset);
            if (!documentFilter.isEmpty()) {
                return CompletionProposalTools.isSubstringFoundOrderedInString(documentFilter, getFilterString());
            } else if (item.getTextEdit() != null) {
                if (item.getTextEdit().isLeft()) {
                    return offset == LSPIJUtils.toOffset(item.getTextEdit().getLeft().getRange().getStart(), document);
                } else {
                    return offset == LSPIJUtils.toOffset(item.getTextEdit().getRight().getInsert().getStart(), document);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }
        return true;
    }
}
