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

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.DocumentUtil;
import com.intellij.util.IncorrectOperationException;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageServiceAccessor;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionOptions;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.ExecuteCommandOptions;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;

/**
 * LSP code action for Intellij.
 */
public class LSPCodeActionIntentionAction implements IntentionAction {
    private final String title;
    private final LanguageServer languageServer;
    private CodeAction codeAction;
    private Command command;

    public LSPCodeActionIntentionAction(Either<Command, CodeAction> action, LanguageServer languageServer) {
        this.languageServer = languageServer;
        if (action.isRight()) {
            codeAction = action.getRight();
            title = action.getRight().getTitle();
        } else {
            command = action.getLeft();
            title = action.getRight().getTitle();
        }
    }

    @Override
    public @IntentionName
    @NotNull String getText() {
        return title;
    }

    @Override
    public @NotNull
    @IntentionFamilyName String getFamilyName() {
        return getText();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return true;
    }

    private boolean isCodeActionResolveSupported(@NotNull Project project) {
        return LanguageServiceAccessor.getInstance(project).checkCapability(languageServer,
                capabilities -> {
                    if (capabilities != null) {
                        Either<Boolean, CodeActionOptions> caProvider = capabilities.getCodeActionProvider();
                        if (caProvider.isLeft()) {
                            // It is wrong, but we need to parse the registerCapability
                            return caProvider.getLeft();
                        } else if (caProvider.isRight()) {
                            CodeActionOptions options = caProvider.getRight();
                            return options.getResolveProvider().booleanValue();
                        }
                    }
                    return false;
                });
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (codeAction != null) {
            if (codeAction.getEdit() == null && codeAction.getCommand() == null && isCodeActionResolveSupported(project)) {
                // Unresolved code action "edit" property. Resolve it.
                languageServer.getTextDocumentService().resolveCodeAction(codeAction)
                        .thenAccept(resolved -> {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                DocumentUtil.writeInRunUndoTransparentAction(() -> {
                                    apply(resolved != null ? resolved : codeAction, project);
                                });
                            });
                        });
            } else {
                apply(codeAction, project);
            }
        } else if (command != null) {
            executeCommand(command, project);
        } else {
            // Should never get here
        }
    }

    private void apply(CodeAction codeaction, @NotNull Project project) {
        if (codeaction != null) {
            if (codeaction.getEdit() != null) {
                LSPIJUtils.applyWorkspaceEdit(codeaction.getEdit(), codeaction.getTitle());
            }
            if (codeaction.getCommand() != null) {
                executeCommand(codeaction.getCommand(), project);
            }
        }
    }

    private void executeCommand(Command command, @NotNull Project project) {
        if (!canSupportCommand(command, languageServer, project)) {
            return;
        }
        ExecuteCommandParams params = new ExecuteCommandParams();
        params.setCommand(command.getCommand());
        params.setArguments(command.getArguments());
        languageServer.getWorkspaceService().executeCommand(params);
    }

    private static boolean canSupportCommand(Command command, LanguageServer languageServer, @NotNull Project project) {
        return LanguageServiceAccessor.getInstance(project).checkCapability(languageServer,
                capabilities -> {
                    if (capabilities != null) {
                        ExecuteCommandOptions provider = capabilities.getExecuteCommandProvider();
                        return (provider != null && provider.getCommands().contains(command.getCommand()));
                    }
                    return false;
                });
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
