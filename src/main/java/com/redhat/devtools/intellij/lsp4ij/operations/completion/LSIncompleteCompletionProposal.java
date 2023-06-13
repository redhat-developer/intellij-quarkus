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

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.util.TextRange;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4ij.LanguageServiceAccessor;
import com.redhat.devtools.intellij.lsp4ij.command.internal.CommandExecutor;
import org.apache.commons.lang.StringUtils;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.redhat.devtools.intellij.lsp4ij.ui.IconMapper.getIcon;

public class LSIncompleteCompletionProposal extends LookupElement {
    private static final Logger LOGGER = LoggerFactory.getLogger(LSIncompleteCompletionProposal.class);

    // Those variables should be defined in LSP4J and reused here whenever done there
    // See https://github.com/eclipse/lsp4j/issues/149
    /** The currently selected text or the empty string */
    private static final String TM_SELECTED_TEXT = "TM_SELECTED_TEXT"; //$NON-NLS-1$
    /** The contents of the current line */
    private static final String TM_CURRENT_LINE = "TM_CURRENT_LINE"; //$NON-NLS-1$
    /** The contents of the word under cursor or the empty string */
    private static final String TM_CURRENT_WORD = "TM_CURRENT_WORD"; //$NON-NLS-1$
    /** The zero-index based line number */
    private static final String TM_LINE_INDEX = "TM_LINE_INDEX"; //$NON-NLS-1$
    /** The one-index based line number */
    private static final String TM_LINE_NUMBER = "TM_LINE_NUMBER"; //$NON-NLS-1$
    /** The filename of the current document */
    private static final String TM_FILENAME = "TM_FILENAME"; //$NON-NLS-1$
    /** The filename of the current document without its extensions */
    private static final String TM_FILENAME_BASE = "TM_FILENAME_BASE"; //$NON-NLS-1$
    /** The directory of the current document */
    private static final String TM_DIRECTORY = "TM_DIRECTORY"; //$NON-NLS-1$
    /** The full file path of the current document */
    private static final String TM_FILEPATH = "TM_FILEPATH"; //$NON-NLS-1$

    protected final CompletionItem item;
    protected final int initialOffset;
    protected       int currentOffset;
    protected       int bestOffset;
    protected final Editor editor;
    private Integer rankCategory;
    private Integer rankScore;
    private String documentFilter;
    private String documentFilterAddition = ""; //$NON-NLS-1$
    protected final LanguageServer languageServer;

    public LSIncompleteCompletionProposal(Editor editor, int offset, CompletionItem item, LanguageServer languageServer) {
        this.item = item;
        this.editor = editor;
        this.languageServer = languageServer;
        this.initialOffset = offset;
        this.currentOffset = offset;
        this.bestOffset = getPrefixCompletionStart(editor.getDocument(), offset);
        //this.bestOffset = offset;
        putUserData(CodeCompletionHandlerBase.DIRECT_INSERTION, true);
    }

    /**
     * See {@link CompletionProposalTools.getFilterFromDocument} for filter
     * generation logic
     *
     * @return The document filter for the given offset
     */
    public String getDocumentFilter(int offset) throws StringIndexOutOfBoundsException {
        if (documentFilter != null) {
            if (offset != currentOffset) {
                documentFilterAddition = editor.getDocument().getText(new TextRange(initialOffset, offset));
                rankScore = null;
                rankCategory = null;
                currentOffset = offset;
            }
            return documentFilter + documentFilterAddition;
        }
        currentOffset = offset;
        return getDocumentFilter();
    }

    /**
     * See {@link CompletionProposalTools.getFilterFromDocument} for filter
     * generation logic
     *
     * @return The document filter for the last given offset
     */
    public String getDocumentFilter() {
        if (documentFilter != null) {
            return documentFilter + documentFilterAddition;
        }
        documentFilter = CompletionProposalTools.getFilterFromDocument(editor.getDocument(), currentOffset,
                getFilterString(), bestOffset);
        documentFilterAddition = ""; //$NON-NLS-1$
        return documentFilter;
    }


    protected String getInsertText() {
        String insertText = this.item.getInsertText();
        Either<TextEdit, InsertReplaceEdit> eitherTextEdit = this.item.getTextEdit();
        if (eitherTextEdit != null) {
            if(eitherTextEdit.isLeft()) {
                insertText = eitherTextEdit.getLeft().getNewText();
            } else {
                insertText = eitherTextEdit.getRight().getNewText();
            }
        }
        if (insertText == null) {
            insertText = this.item.getLabel();
        }
        return insertText;
    }

    public int getPrefixCompletionStart(Document document, int completionOffset) {
        Either<TextEdit, InsertReplaceEdit> textEdit = this.item.getTextEdit();
        if (textEdit != null) {
            if (textEdit.isLeft()) {
                try {
                    return LSPIJUtils.toOffset(this.item.getTextEdit().getLeft().getRange().getStart(), document);
                } catch (RuntimeException e) {
                    LOGGER.warn(e.getLocalizedMessage(), e);
                }
            } else {
                try {
                    return LSPIJUtils.toOffset(this.item.getTextEdit().getRight().getInsert().getStart(), document);
                } catch (RuntimeException e) {
                    LOGGER.warn(e.getLocalizedMessage(), e);
                }
            }
        }
        String insertText = getInsertText();
        try {
            String subDoc = document.getText(new TextRange(
                    Math.max(0, completionOffset - insertText.length()),
                    Math.min(insertText.length(), completionOffset)));
            for (int i = 0; i < insertText.length() && i < completionOffset; i++) {
                String tentativeCommonString = subDoc.substring(i);
                if (insertText.startsWith(tentativeCommonString)) {
                    return completionOffset - tentativeCommonString.length();
                }
            }
        } catch (RuntimeException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }
        return completionOffset;
    }


    @NotNull
    @Override
    public String getLookupString() {
        String lookup = StringUtils.isNotBlank(item.getFilterText())?item.getFilterText():item.getLabel();
        if (lookup.charAt(0) == '@') {
            return lookup.substring(1);
        }
        return lookup;
    }

    private boolean isDeprecated() {
        return (item.getTags() != null && item.getTags().contains(CompletionItemTag.Deprecated))
        || (item.getDeprecated() != null && item.getDeprecated().booleanValue());
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(item.getLabel());
        presentation.setTypeText(item.getDetail());
        presentation.setIcon(getIcon(item.getKind()));
        if (isDeprecated()) {
            presentation.setStrikeout(true);
        }
    }

    protected void apply(Document document, char trigger, int stateMask, int offset) {
        String insertText = null;
        Either<TextEdit, InsertReplaceEdit> eitherTextEdit = item.getTextEdit();
        TextEdit textEdit = null;
        if (eitherTextEdit != null) {
            if(eitherTextEdit.isLeft()) {
                textEdit = eitherTextEdit.getLeft();
            } else {
                // trick to partially support the new InsertReplaceEdit from LSP 3.16. Reuse previously code for TextEdit.
                InsertReplaceEdit insertReplaceEdit = eitherTextEdit.getRight();
                textEdit = new TextEdit(insertReplaceEdit.getInsert(), insertReplaceEdit.getNewText());
            }
        }
        try {
            if (textEdit == null) {
                insertText = getInsertText();
                Position start = LSPIJUtils.toPosition(this.bestOffset, document);
                Position end = LSPIJUtils.toPosition(offset, document); // need 2 distinct objects
                textEdit = new TextEdit(new Range(start, end), insertText);
            } else if (offset > this.initialOffset) {
                // characters were added after completion was activated
                int shift = offset - this.initialOffset;
                textEdit.getRange().getEnd().setCharacter(textEdit.getRange().getEnd().getCharacter() + shift);
            }
            { // workaround https://github.com/Microsoft/vscode/issues/17036
                Position start = textEdit.getRange().getStart();
                Position end = textEdit.getRange().getEnd();
                if (start.getLine() > end.getLine() || (start.getLine() == end.getLine() && start.getCharacter() > end.getCharacter())) {
                    textEdit.getRange().setEnd(start);
                    textEdit.getRange().setStart(end);
                }
            }
            { // allow completion items to be wrong with a too wide range
                Position documentEnd = LSPIJUtils.toPosition(document.getTextLength(), document);
                Position textEditEnd = textEdit.getRange().getEnd();
                if (documentEnd.getLine() < textEditEnd.getLine()
                        || (documentEnd.getLine() == textEditEnd.getLine() && documentEnd.getCharacter() < textEditEnd.getCharacter())) {
                    textEdit.getRange().setEnd(documentEnd);
                }
            }

            if (insertText != null) {
                // try to reuse existing characters after completion location
                int shift = offset - this.bestOffset;
                int commonSize = 0;
                while (commonSize < insertText.length() - shift
                        && document.getTextLength() > offset + commonSize
                        && document.getText().charAt(this.bestOffset + shift + commonSize) == insertText.charAt(commonSize + shift)) {
                    commonSize++;
                }
                textEdit.getRange().getEnd().setCharacter(textEdit.getRange().getEnd().getCharacter() + commonSize);
            }
            insertText = textEdit.getNewText();
            int insertionOffset = LSPIJUtils.toOffset(textEdit.getRange().getStart(), document);
            insertionOffset = computeNewOffset(item.getAdditionalTextEdits(), insertionOffset, document);
            if (item.getInsertTextFormat() == InsertTextFormat.Snippet) {
                int currentSnippetOffsetInInsertText = 0;
                while ((currentSnippetOffsetInInsertText = insertText.indexOf('$', currentSnippetOffsetInInsertText)) != -1) {
                    StringBuilder keyBuilder = new StringBuilder();
                    boolean isChoice = false;
                    List<String> snippetProposals = new ArrayList<>();
                    int offsetInSnippet = 1;
                    while (currentSnippetOffsetInInsertText + offsetInSnippet < insertText.length() && Character.isDigit(insertText.charAt(currentSnippetOffsetInInsertText + offsetInSnippet))) {
                        keyBuilder.append(insertText.charAt(currentSnippetOffsetInInsertText + offsetInSnippet));
                        offsetInSnippet++;
                    }
                    if (keyBuilder.length() == 0 && insertText.substring(currentSnippetOffsetInInsertText).startsWith("${")) { //$NON-NLS-1$
                        offsetInSnippet = 2;
                        while (currentSnippetOffsetInInsertText + offsetInSnippet < insertText.length() && Character.isDigit(insertText.charAt(currentSnippetOffsetInInsertText + offsetInSnippet))) {
                            keyBuilder.append(insertText.charAt(currentSnippetOffsetInInsertText + offsetInSnippet));
                            offsetInSnippet++;
                        }
                        if (currentSnippetOffsetInInsertText + offsetInSnippet < insertText.length()) {
                            char currentChar = insertText.charAt(currentSnippetOffsetInInsertText + offsetInSnippet);
                            if (currentChar == ':' || currentChar == '|') {
                                isChoice |= currentChar == '|';
                                offsetInSnippet++;
                            }
                        }
                        boolean close = false;
                        StringBuilder valueBuilder = new StringBuilder();
                        while (currentSnippetOffsetInInsertText + offsetInSnippet < insertText.length() && !close) {
                            char currentChar = insertText.charAt(currentSnippetOffsetInInsertText + offsetInSnippet);
                            if (valueBuilder.length() > 0 &&
                                    ((isChoice && (currentChar == ',' || currentChar == '|') || currentChar == '}'))) {
                                String value = valueBuilder.toString();
                                if (value.startsWith("$")) { //$NON-NLS-1$
                                    String varValue = getVariableValue(value.substring(1));
                                    if (varValue != null) {
                                        value = varValue;
                                    }
                                }
                                snippetProposals.add(value);
                                valueBuilder = new StringBuilder();
                            } else if (currentChar != '}') {
                                valueBuilder.append(currentChar);
                            }
                            close = currentChar == '}';
                            offsetInSnippet++;
                        }
                    }
                    String defaultProposal = snippetProposals.isEmpty() ? "" : snippetProposals.get(0); //$NON-NLS-1$
                    if (keyBuilder.length() > 0) {
                        String key = keyBuilder.toString();
                        insertText = insertText.substring(0, currentSnippetOffsetInInsertText) + defaultProposal + insertText.substring(currentSnippetOffsetInInsertText + offsetInSnippet);
                        currentSnippetOffsetInInsertText += defaultProposal.length();
                    } else {
                        currentSnippetOffsetInInsertText++;
                    }
                }
            }
            textEdit.setNewText(insertText); // insertText now has placeholder removed
            List<TextEdit> additionalEdits = item.getAdditionalTextEdits();
            if (additionalEdits != null && !additionalEdits.isEmpty()) {
                List<TextEdit> allEdits = new ArrayList<>();
                allEdits.add(textEdit);
                allEdits.addAll(additionalEdits);
                LSPIJUtils.applyEdits(editor, document, allEdits);
            } else {
                LSPIJUtils.applyEdits(editor, document, Collections.singletonList(textEdit));
            }

            LanguageServiceAccessor.getInstance(editor.getProject()).resolveServerDefinition(languageServer).map(definition -> definition.id)
                    .ifPresent(id -> {
                        Command command = item.getCommand();
                        if (command == null) {
                            return;
                        }
                        CommandExecutor.executeCommand(editor.getProject(), command, document, id);
                    });
        } catch (RuntimeException ex) {
            LOGGER.warn(ex.getLocalizedMessage(), ex);
        }
    }

    private int computeNewOffset(List<TextEdit> additionalTextEdits, int insertionOffset, Document doc) {
        if (additionalTextEdits != null && !additionalTextEdits.isEmpty()) {
            int adjustment = 0;
            for (TextEdit edit : additionalTextEdits) {
                try {
                    Range rng = edit.getRange();
                    int start = LSPIJUtils.toOffset(rng.getStart(), doc);
                    if (start <= insertionOffset) {
                        int end = LSPIJUtils.toOffset(rng.getEnd(), doc);
                        int orgLen = end - start;
                        int newLeng = edit.getNewText().length();
                        int editChange = newLeng - orgLen;
                        adjustment += editChange;
                    }
                } catch (RuntimeException e) {
                    LOGGER.warn(e.getLocalizedMessage(), e);
                }
            }
            return insertionOffset + adjustment;
        }
        return insertionOffset;
    }

    private String getVariableValue(String variableName) {
        Document document = editor.getDocument();
        switch (variableName) {
            case TM_FILENAME_BASE:
                String fileName = LSPIJUtils.getFile(document).getNameWithoutExtension();
                return fileName != null ? fileName : ""; //$NON-NLS-1$
            case TM_FILENAME:
                return LSPIJUtils.getFile(document).getName();
            case TM_FILEPATH:
                return LSPIJUtils.getFile(document).getPath();
            case TM_DIRECTORY:
                return LSPIJUtils.getFile(document).getParent().getPath();
            case TM_LINE_INDEX:
                int lineIndex = getTextEditRange().getStart().getLine();
                return Integer.toString(lineIndex);
            case TM_LINE_NUMBER:
                int lineNumber = getTextEditRange().getStart().getLine();
                return Integer.toString(lineNumber + 1);
            case TM_CURRENT_LINE:
                int currentLineIndex = getTextEditRange().getStart().getLine();
                try {
                    int lineOffsetStart = document.getLineStartOffset(currentLineIndex);
                    int lineOffsetEnd = document.getLineEndOffset(currentLineIndex);
                    String line = document.getText(new TextRange(lineOffsetStart, lineOffsetEnd));
                    return line;
                } catch (RuntimeException e) {
                    LOGGER.warn(e.getMessage(), e);
                    return ""; //$NON-NLS-1$
                }
            case TM_SELECTED_TEXT:
                Range selectedRange = getTextEditRange();
                try {
                    int startOffset = LSPIJUtils.toOffset(selectedRange.getStart(), document);
                    int endOffset = LSPIJUtils.toOffset(selectedRange.getEnd(), document);
                    String selectedText = document.getText(new TextRange(startOffset, endOffset));
                    return selectedText;
                } catch (RuntimeException e) {
                    LOGGER.warn(e.getMessage(), e);
                    return ""; //$NON-NLS-1$
                }
            case TM_CURRENT_WORD:
                return ""; //$NON-NLS-1$
            default:
                return null;
        }
    }

    private Range getTextEditRange() {
        if (item.getTextEdit().isLeft()) {
            return item.getTextEdit().getLeft().getRange();
        } else {
            // here providing insert range, currently do not know if insert or replace is requested
            return item.getTextEdit().getRight().getInsert();
        }
    }

    public String getFilterString() {
        if (item.getFilterText() != null && !item.getFilterText().isEmpty()) {
            return item.getFilterText();
        }
        return item.getLabel();
    }

    @Override
    public void handleInsert(@NotNull InsertionContext context) {
        apply(context.getDocument(), context.getCompletionChar(), 0, context.getOffset(CompletionInitializationContext.SELECTION_END_OFFSET));
    }

    public boolean validate(Document document, int offset, DocumentEvent event) {
        return true;
    }

    public CompletionItem getItem() {
        return item;
    }
}
