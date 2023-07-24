/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4ij.operations.codeactions;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.DocumentUtil;
import com.intellij.util.IncorrectOperationException;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4ij.LanguageServerWrapper;
import org.apache.commons.lang.StringUtils;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;

import static com.redhat.devtools.intellij.lsp4ij.operations.codeactions.LSPLazyCodeActions.NO_CODEACTION_AT_INDEX;

/**
 * The lazy IJ Quick fix.
 */
public class LSPLazyCodeActionIntentionAction implements IntentionAction {

	private final LSPLazyCodeActions lazyCodeActions;

	private final int index;
	private Either<Command, CodeAction> action;
	private CodeAction codeAction;

	private String title;
	private Command command;
	private String familyName;

	public LSPLazyCodeActionIntentionAction(LSPLazyCodeActions lazyCodeActions, int index) {
		this.lazyCodeActions = lazyCodeActions;
		this.index = index;
	}

	@Override
	public @IntentionName @NotNull String getText() {
		loadCodeActionIfNeeded();
		return title;
	}

	@Override
	public @NotNull @IntentionFamilyName String getFamilyName() {
		loadCodeActionIfNeeded();
		return familyName;
	}

	@Override
	public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
		loadCodeActionIfNeeded();
		return isValidCodeAction();
	}

	@Override
	public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
		if (codeAction != null) {
			if (codeAction.getEdit() == null && codeAction.getCommand() == null && isCodeActionResolveSupported()) {
				// Unresolved code action "edit" property. Resolve it.
				getLanguageServerWrapper().getInitializedServer()
						.thenApply(ls ->
								ls.getTextDocumentService().resolveCodeAction(codeAction)
										.thenAccept(resolved -> {
											ApplicationManager.getApplication().invokeLater(() -> {
												DocumentUtil.writeInRunUndoTransparentAction(() -> {
													apply(resolved != null ? resolved : codeAction, project);
												});
											});
										})
						);
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
		if (!canSupportCommand(command)) {
			return;
		}
		ExecuteCommandParams params = new ExecuteCommandParams();
		params.setCommand(command.getCommand());
		params.setArguments(command.getArguments());
		getLanguageServerWrapper()
				.getInitializedServer()
				.thenApply(ls -> ls.getWorkspaceService().executeCommand(params)
				);
	}

	private boolean canSupportCommand(Command command) {
		ServerCapabilities capabilities = getLanguageServerWrapper().getServerCapabilities();
		if (capabilities != null) {
			ExecuteCommandOptions provider = capabilities.getExecuteCommandProvider();
			return (provider != null && provider.getCommands().contains(command.getCommand()));
		}
		return false;
	}

	private LanguageServerWrapper getLanguageServerWrapper() {
		return lazyCodeActions.getLanguageServerWrapper();
	}

	private boolean isCodeActionResolveSupported() {
		ServerCapabilities capabilities = getLanguageServerWrapper().getServerCapabilities();
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
	}

	@Override
	public boolean startInWriteAction() {
		return true;
	}

	private void loadCodeActionIfNeeded() {
		if (action != null) {
			// The LSP code action has been already loaded.
			return;
		}
		// Try to get the LSP code action from the given indes
		this.action = lazyCodeActions.getCodeActionAt(index);
		if (isValidCodeAction()) {
			if (action.isRight()) {
				codeAction = action.getRight();
				title = action.getRight().getTitle();
				familyName = StringUtils.isNotBlank(codeAction.getKind()) ? codeAction.getKind() : "LSP QuickFix";
			} else {
				command = action.getLeft();
				title = action.getRight().getTitle();
				familyName = "LSP Command";
			}
		}
	}

	private boolean isValidCodeAction() {
		return action != null && !NO_CODEACTION_AT_INDEX.equals(action);
	}

}
