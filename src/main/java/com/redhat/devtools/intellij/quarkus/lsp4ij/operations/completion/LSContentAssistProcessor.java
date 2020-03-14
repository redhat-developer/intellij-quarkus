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
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageServiceAccessor;
import one.util.streamex.IntStreamEx;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.TextEdit;
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
import java.util.stream.IntStream;

public class LSContentAssistProcessor extends CompletionContributor {
    private static final Logger LOGGER = LoggerFactory.getLogger(LSContentAssistProcessor.class);

    private Document currentDocument;
    private CompletableFuture<List<LanguageServer>> completionLanguageServersFuture;
    private final Object completionTriggerCharsSemaphore = new Object();
    private char[] completionTriggerChars = new char[0];

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        Document document = parameters.getEditor().getDocument();
        Editor editor = parameters.getEditor();
        Project project = parameters.getOriginalFile().getProject();
        int offset = parameters.getOffset();
        initiateLanguageServers(document);
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
        return items.stream().map(item -> createLookupItem(project, editor, item)).collect(Collectors.toList());
    }

    private LookupElement createLookupItem(Project project, Editor editor, CompletionItem item) {
        TextEdit textEdit = item.getTextEdit();
        List<TextEdit> addTextEdits = item.getAdditionalTextEdits();
        LookupElementBuilder lookupElementBuilder;
        String label = item.getLabel();
        String insertText = item.getInsertText();
        String presentableText = StringUtils.isNotBlank(label)?label:insertText!=null?insertText:"";
        InsertTextFormat insertFormat = item.getInsertTextFormat();
        Command command = item.getCommand();
        CompletionItemKind kind = item.getKind();
        boolean deprecated = item.getDeprecated()!=null?item.getDeprecated():false;
        String detail = item.getDetail();
        String tailText = detail!=null?"\t" + detail:"";

        if (textEdit != null) {
            if (addTextEdits != null) {
                lookupElementBuilder = LookupElementBuilder.create(presentableText, "")
                        .withInsertHandler((context,lookupElement) -> {
                    context.commitDocument();
                    if (insertFormat == InsertTextFormat.Snippet) {
                        Template template = prepareTemplate(textEdit.getNewText());
                        runSnippet(project, editor, template, command, addTextEdits, label);
                    } else {
                        applyEdits(project, editor, textEdit, addTextEdits, null, true, label);
                    }
                })
                  .withLookupString(presentableText);
            } else {
                lookupElementBuilder = LookupElementBuilder.create(presentableText, "")
                        .withInsertHandler((context, lookupElement) -> {
                    context.commitDocument();
                    if (insertFormat == InsertTextFormat.Snippet) {
                        Template template = prepareTemplate(textEdit.getNewText());
                        runSnippet(project, editor, template, command, addTextEdits, label);
                    } else {
                        applyEdits(project, editor, textEdit, Collections.emptyList(), null, true, label);
                    }
                })
                  .withLookupString(presentableText);
            }
        } else if (addTextEdits != null) {
            lookupElementBuilder = LookupElementBuilder.create(presentableText, "")
                    .withInsertHandler((context, lookupElement) -> {
                context.commitDocument();
                if (insertFormat == InsertTextFormat.Snippet) {
                    Template template = prepareTemplate(StringUtils.isNotBlank(insertText)?insertText:label);
                    runSnippet(project, editor, template, command, addTextEdits, label);
                } else {
                    applyEdits(project, editor, null, addTextEdits, null, false, label);
                }
            })
                .withLookupString(presentableText);
        } else {
            lookupElementBuilder = LookupElementBuilder.create(StringUtils.isNotBlank(insertText)?insertText:label);
            if (command != null) lookupElementBuilder = lookupElementBuilder.withInsertHandler((context, lookupElement) -> {
                context.commitDocument();
                if (insertFormat == InsertTextFormat.Snippet) {
                    Template template = prepareTemplate(StringUtils.isNotBlank(insertText)?insertText:label);
                    runSnippet(project, editor, template, command, addTextEdits, label);
                }
                applyEdits(project, editor, null, Collections.emptyList(), null, false, label);
            });
        }
        if (kind == CompletionItemKind.Keyword) lookupElementBuilder = lookupElementBuilder.withBoldness(true);
        if (deprecated) {
            lookupElementBuilder = lookupElementBuilder.withStrikeoutness(true);
        }
        return lookupElementBuilder.withPresentableText(presentableText).withTailText(tailText, true).withAutoCompletionPolicy(AutoCompletionPolicy.SETTINGS_DEPENDENT);
    }

    //TODO: implement template
    //private Template prepareTemplate(String newText) {
        return null;
    }

    private Template prepareTemplate(String insertText) {
        IntStream startIndexes = IntStream.range(0, insertText.length()).filter(index -> insertText.startsWith("$", index));
        val variables = startIndexes.map(i -> {
                String sub = insertText.substring(i);
        if (sub.length() > 0 && sub.charAt(0) == '{') {
            val num = sub.tail.takeWhile(c => c != ':')
            val placeholder = sub.tail.dropWhile(c => c != ':').tail.takeWhile(c => c != '}')
            val len = num.length + placeholder.length + 4
            (i, i + len, num, placeholder)
        } else {
            val num = sub.takeWhile(c => c.isDigit)
            val placeholder = "..."
            val len = num.length + 1
            (i, i + len, num, placeholder)
        }
              })
        var newInsertText = insertText
        variables.sortBy(t => -t._1).foreach(t => newInsertText = newInsertText.take(t._1) + "$" + t._3 + "$" + newInsertText.drop(t._2))

        val template = TemplateManager.getInstance(project).createTemplate("anon" + (1 to 5).map(_ => Random.nextPrintableChar()).mkString(""), "lsp")

        variables.foreach(t => {
                template.addVariable(t._3, new TextExpression(t._4), new TextExpression(t._4), true, false)
        })
        template.setInline(true)
        template.asInstanceOf[TemplateImpl].setString(newInsertText)
        template
    }


    void runSnippet(Project project, Editor editor, Template template, Command command, List<TextEdit> addTextEdits, String label) {
        ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> CommandProcessor.getInstance().executeCommand(project, () -> editor.getDocument().insertString(editor.getCaretModel().getOffset(), template.getTemplateText()), "snippetInsert", "lsp", editor.getDocument()));
            TemplateManager.getInstance(project).startTemplate(editor, template);
            if (addTextEdits != null) {
                applyEdit(project, editor, Integer.MAX_VALUE, addTextEdits, "Additional Completions : " + label, false);
            }
            execCommand(command);
        });
    }

    void applyEdits(Project project, Editor editor, TextEdit edit, final List<TextEdit> edits, Command command, boolean moveToCaret, String label) {
        final List<TextEdit> fedits;
        if (edit != null) {
            fedits = new ArrayList<>(edits);
            fedits.add(edit);
        } else {
            fedits = edits;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            if (fedits != null && !fedits.isEmpty()) {
                applyEdit(project, editor, Integer.MAX_VALUE, fedits, "Completion : " + label, false);
            }
            execCommand(command);
            if (moveToCaret) {
                editor.getCaretModel().moveCaretRelatively(edit.getNewText().length(), 0, false, false, true);
            }
        });
    }

    void execCommand(Command command) {
        if (command != null) executeCommands(Collections.singletonList(command));
    }

    //TODO: implement commands
    void executeCommands(List<Command> commands) {
    }

    private void saveDocument(Document document) {
        ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> FileDocumentManager.getInstance().saveDocument(document)));
    }

    Runnable getEditsRunnable(Project project, Editor editor, int version, List<TextEdit> edits, String name) {
        if (true) {
            Document document = editor.getDocument();
            if (document.isWritable()) {
                return () -> {
                    edits.stream().map(te -> new ImmutablePair<RangeMarker, String>(
                            document.createRangeMarker(LSPIJUtils.toOffset(te.getRange().getStart(), document),
                                    LSPIJUtils.toOffset(te.getRange().getEnd(), document)),
                            te.getNewText())
          ).forEach(markerText -> {
                            int start = markerText.getLeft().getStartOffset();
                            int end = markerText.getLeft().getEndOffset();
                            String text = markerText.getRight();
                    if (text == "" || text == null) {
                        document.deleteString(start, end);
                    } else if (end - start <= 0) {
                        document.insertString(start, text);
                    } else {
                        document.replaceString(start, end, text);
                    }
                    markerText.getLeft().dispose();
          });
                    saveDocument(document);
                };
            } else {
                LOGGER.warn("Document is not writable");
                return null;
            }
        } else {
            LOGGER.warn("Edit version " + version + " is older than current version ");
            return null;
        }
    }


    boolean applyEdit(Project project, Editor editor, int version, List<TextEdit> edits, String name, boolean closeAfter) {
        Runnable runnable = getEditsRunnable(project, editor, version, edits, name);
        ApplicationManager.getApplication().runWriteAction(() -> {
      /*      holdDCE.synchronized {
              holdDCE = true
            }*/
            if (runnable != null)
                CommandProcessor.getInstance().executeCommand(project, runnable, name, "LSPPlugin", editor.getDocument());
            if (closeAfter) {
                FileEditorManager.getInstance(project)
                        .closeFile(PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument()).getVirtualFile());
            }
      /*      holdDCE.synchronized {
              holdDCE = false
            }*/
        });
        return runnable != null ? true : false;
    }


    private LookupElement createErrorProposal(int offset, Exception ex) {
        return LookupElementBuilder.create("Error while computing completion", "");
    }

    private void initiateLanguageServers(Document document) {
        if (currentDocument != document) {
            this.currentDocument = document;
            if (this.completionLanguageServersFuture != null) {
                try {
                    this.completionLanguageServersFuture.cancel(true);
                } catch (CancellationException ex) {
                    // nothing
                }
            }
            this.completionTriggerChars = new char[0];

            this.completionLanguageServersFuture = LanguageServiceAccessor.getLanguageServers(document,
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



    @Override
    public void beforeCompletion(@NotNull CompletionInitializationContext context) {
        System.out.println("beforeCompletion called");
        super.beforeCompletion(context);
    }

    @Override
    public void duringCompletion(@NotNull CompletionInitializationContext context) {
        System.out.println("duringCompletion called");
        super.duringCompletion(context);
    }
}
