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
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.TemplateImpl;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4ij.LanguageServiceAccessor;
import com.redhat.devtools.intellij.lsp4ij.command.internal.CommandExecutor;
import com.redhat.devtools.intellij.lsp4ij.operations.completion.snippet.LspSnippetIndentOptions;
import org.apache.commons.lang.StringUtils;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.redhat.devtools.intellij.lsp4ij.operations.completion.snippet.LspSnippetVariableConstants.*;
import static com.redhat.devtools.intellij.lsp4ij.ui.IconMapper.getIcon;

public class LSIncompleteCompletionProposal extends LookupElement {
    private static final Logger LOGGER = LoggerFactory.getLogger(LSIncompleteCompletionProposal.class);

    protected final CompletionItem item;
    protected final int initialOffset;
    private final PsiFile file;
    protected int currentOffset;
    protected int bestOffset;
    protected final Editor editor;
    private Integer rankCategory;
    private Integer rankScore;
    private String documentFilter;
    private String documentFilterAddition = ""; //$NON-NLS-1$
    protected final LanguageServer languageServer;

    public LSIncompleteCompletionProposal(PsiFile file, Editor editor, int offset, CompletionItem item, LanguageServer languageServer) {
        this.file = file;
        this.item = item;
        this.editor = editor;
        this.languageServer = languageServer;
        this.initialOffset = offset;
        this.currentOffset = offset;
        this.bestOffset = getPrefixCompletionStart(editor.getDocument(), offset);
        putUserData(CodeCompletionHandlerBase.DIRECT_INSERTION, true);
    }

    @Override
    public void handleInsert(@NotNull InsertionContext context) {
        Template template = null;
        if (item.getInsertTextFormat() == InsertTextFormat.Snippet) {
            // Insert text has snippet syntax, ex : ${1:name}
            String insertText = getInsertText();
            // Get the indentation settings
            LspSnippetIndentOptions indentOptions = createLspIndentOptions(insertText, file);
            // Load the insert text to build:
            // - an IJ Template instance which will take care of replacement of placeholders
            // - the insert text without placeholders
            template = SnippetTemplateFactory.createTemplate(insertText, context.getProject(), name -> getVariableValue(name), indentOptions);
            // Update the TextEdit with the content snippet content without placeholders
            // ex : ${1:name} --> name
            updateInsertText(template.getTemplateText());
        }

        // Apply all text edits
        apply(context.getDocument(), context.getCompletionChar(), 0, context.getOffset(CompletionInitializationContext.SELECTION_END_OFFSET));

        if (template != null && ((TemplateImpl) template).getVariableCount() > 0) {
            // LSP completion with snippet syntax, activate the inline template
            context.setAddCompletionChar(false);
            EditorModificationUtil.moveCaretRelatively(editor, -template.getTemplateText().length());
            TemplateManager.getInstance(context.getProject()).startTemplate(context.getEditor(), template);
        }
    }

    private static LspSnippetIndentOptions createLspIndentOptions(String insertText, PsiFile file) {
        if (LspSnippetIndentOptions.shouldBeFormatted(insertText)) {
            // Get global line separator settings
            CodeStyleSettings settings = CodeStyleSettings.getDefaults();
            String lineSeparator = settings.getLineSeparator();
            CommonCodeStyleSettings.@NotNull IndentOptions indentOptions = getIndentOptions(file, settings);
            boolean insertSpaces = !indentOptions.USE_TAB_CHARACTER;
            int tabSize = indentOptions.TAB_SIZE;
            return new LspSnippetIndentOptions(tabSize, insertSpaces, lineSeparator);
        }
        return null;
    }

    private static CommonCodeStyleSettings.@NotNull IndentOptions getIndentOptions(PsiFile file, CodeStyleSettings settings) {
        Project project = file.getProject();
        FileViewProvider provider = PsiManagerEx.getInstanceEx(project).findViewProvider(file.getVirtualFile());
        if (provider instanceof TemplateLanguageFileViewProvider) {
            // get indent options of the language
            Language language = ((TemplateLanguageFileViewProvider) provider).getTemplateDataLanguage();
            CommonCodeStyleSettings.IndentOptions indentOptions = settings.getLanguageIndentOptions(language);
            if (indentOptions != null) {
                return indentOptions;
            }
        }
        Language language = provider.getBaseLanguage();
        CommonCodeStyleSettings.IndentOptions indentOptions = settings.getLanguageIndentOptions(language);
        if (indentOptions != null) {
            return indentOptions;
        }
        return settings.getIndentOptions();
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
            if (eitherTextEdit.isLeft()) {
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

    private void updateInsertText(String newText) {
        Either<TextEdit, InsertReplaceEdit> eitherTextEdit = this.item.getTextEdit();
        if (eitherTextEdit != null) {
            if (eitherTextEdit.isLeft()) {
                eitherTextEdit.getLeft().setNewText(newText);
            } else {
                eitherTextEdit.getRight().setNewText(newText);
            }
        }
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
        return StringUtils.isNotBlank(item.getFilterText()) ? item.getFilterText() : item.getLabel();
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
            if (eitherTextEdit.isLeft()) {
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

    public boolean validate(Document document, int offset, DocumentEvent event) {
        return true;
    }

    public CompletionItem getItem() {
        return item;
    }

    /**
     * Return the result of the resolved LSP variable and null otherwise.
     *
     * @param variableName the variable name to resolve.
     * @return the result of the resolved LSP variable and null otherwise.
     */
    private @Nullable String getVariableValue(String variableName) {
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

}
