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
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageServerWrapper;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionOptions;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.ExecuteCommandOptions;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;

public class LSPCodeActionIntentionAction implements IntentionAction {
    private final String title;
    private final LanguageServerWrapper finfo;
    private CodeAction fcodeAction;
    private Command fcommand;

    public LSPCodeActionIntentionAction(Either<Command, CodeAction> action, LanguageServerWrapper finfo) {
        this.finfo = finfo;
        if (action.isRight()) {
            fcodeAction = action.getRight();
            title = action.getRight().getTitle();
        } else {
            fcommand = action.getLeft();
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

    private boolean isCodeActionResolveSupported() {
        ServerCapabilities capabilities = this.finfo.getServerCapabilities();
        if (capabilities != null) {
            Either<Boolean, CodeActionOptions> caProvider = capabilities.getCodeActionProvider();
            if (caProvider.isRight()) {
                CodeActionOptions options = caProvider.getRight();
                return options.getResolveProvider().booleanValue();
            }
        }
        return false;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (fcodeAction != null) {
            if (isCodeActionResolveSupported() && fcodeAction.getEdit() == null) {
                // Unresolved code action "edit" property. Resolve it.
                finfo.getInitializedServer().thenComposeAsync(ls -> ls.getTextDocumentService().resolveCodeAction(fcodeAction)).thenAccept(this::apply);
            } else {
                apply(fcodeAction);
            }
        } else if (fcommand != null) {
            executeCommand(fcommand);
        } else {
            // Should never get here
        }
    }

    private void apply(CodeAction codeaction) {
        if (codeaction != null) {
            if (codeaction.getEdit() != null) {
                LSPIJUtils.applyWorkspaceEdit(codeaction.getEdit(), codeaction.getTitle());
            }
            if (codeaction.getCommand() != null) {
                executeCommand(codeaction.getCommand());
            }
        }
    }

    private void executeCommand(Command command) {
        ServerCapabilities capabilities = this.finfo.getServerCapabilities();
        if (capabilities != null) {
            ExecuteCommandOptions provider = capabilities.getExecuteCommandProvider();
            if (provider != null && provider.getCommands().contains(command.getCommand())) {
                ExecuteCommandParams params = new ExecuteCommandParams();
                params.setCommand(command.getCommand());
                params.setArguments(command.getArguments());
                this.finfo.getInitializedServer()
                        .thenAcceptAsync(ls -> ls.getWorkspaceService().executeCommand(params));
            }
        }
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
