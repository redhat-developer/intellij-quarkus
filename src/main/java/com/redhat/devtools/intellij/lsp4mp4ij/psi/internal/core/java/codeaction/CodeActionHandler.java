/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.java.codeaction;

import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.ExtendedCodeAction;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.java.corrections.DiagnosticsHelper;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Code action handler.
 *
 * @author Angelo ZERR
 *
 */
public class CodeActionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(CodeActionHandler.class);

	/**
	 * Returns all the code actions applicable for the context given by the
	 * parameters.
	 *
	 * The workspace edit will be resolved if code action resolve isn't supported.
	 * Otherwise it will be null.
	 *
	 * @param params  the parameters for code actions
	 * @param utils   the JDT utils
	 * @return all the code actions applicable for the context given by the
	 *         parameters
	 */
	public List<? extends CodeAction> codeAction(MicroProfileJavaCodeActionParams params, IPsiUtils utils) {
		try {
			// Get the compilation unit
			String uri = params.getUri();
			PsiFile unit = utils.resolveCompilationUnit(uri);
			if (unit == null) {
				return Collections.emptyList();
			}

			utils = utils.refine(utils.getModule(uri));

			// Prepare the code action invocation context
			int start = DiagnosticsHelper.getStartOffset(unit, params.getRange(), utils);
			int end = DiagnosticsHelper.getEndOffset(unit, params.getRange(), utils);
			JavaCodeActionContext context = new JavaCodeActionContext(unit, start, end - start, utils,
					params);
			context.setASTRoot(getASTRoot(unit));

			// Collect the available code action kinds
			List<String> codeActionKinds = new ArrayList<>();
			if (params.getContext().getOnly() != null && !params.getContext().getOnly().isEmpty()) {
				codeActionKinds.addAll(params.getContext().getOnly());
			} else {
				List<String> defaultCodeActionKinds = Arrays.asList(//
						CodeActionKind.QuickFix, //
						CodeActionKind.Refactor, //
						// JavaCodeActionKind.QUICK_ASSIST,
						CodeActionKind.Source);
				codeActionKinds.addAll(defaultCodeActionKinds);
			}

			List<CodeAction> codeActions = new ArrayList<>();
			Map<String, List<JavaCodeActionDefinition>> forDiagnostics = new HashMap<>();

			// Loop for each code action kinds to process the proper code actions
			for (String codeActionKind : codeActionKinds) {
				// Get list of code action definition for the given kind
				List<JavaCodeActionDefinition> codeActionDefinitions = JavaCodeActionDefinition.EP.extensions()
						.filter(definition -> definition.isAdaptedForCodeAction(context))
						.filter(definition -> codeActionKind.equals(definition.getKind()))
						.collect(Collectors.toList());
				if (codeActionDefinitions != null) {
					// Loop for each code action definition
					for (JavaCodeActionDefinition definition : codeActionDefinitions) {
						String forDiagnostic = definition.getTargetDiagnostic();
						if (forDiagnostic != null) {
							// The code action definition is for a given diagnostic code (QuickFix), store
							// it
							List<JavaCodeActionDefinition> definitionsFor = forDiagnostics.get(forDiagnostic);
							if (definitionsFor == null) {
								definitionsFor = new ArrayList<>();
								forDiagnostics.put(forDiagnostic, definitionsFor);
							}
							definitionsFor.add(definition);
						} else {
							// Collect the code actions
							codeActions.addAll(definition.getCodeActions(context.copy(), null));
						}
					}
				}
			}

			if (!forDiagnostics.isEmpty()) {
				// It exists code action to fix diagnostics, loop for each diagnostics
				params.getContext().getDiagnostics().forEach(diagnostic -> {
					String code = getCodeString(diagnostic.getCode());
					if (code != null) {
						// Try to get code action definition registered with the "for" source#code
						String key = diagnostic.getSource() + "#" + code;
						List<JavaCodeActionDefinition> definitionsFor = forDiagnostics.get(key);
						if (definitionsFor == null) {
							// Try to get code action definition registered with the "for" code
							definitionsFor = forDiagnostics.get(code);
						}
						if (definitionsFor != null) {
							for (JavaCodeActionDefinition definition : definitionsFor) {
								// Collect the code actions to fix the given diagnostic
								codeActions.addAll(definition.getCodeActions(context.copy(), diagnostic));
							}
						}
					}
				});
			}
			if (!params.isResolveSupported()) {
				IPsiUtils finalUtils = utils;
				List<CodeAction> resolvedCodeActions = codeActions.stream()
						.map(codeAction -> {
							if (codeAction.getEdit() != null || codeAction.getCommand() != null) {
								// CodeAction is already resolved
								// (eg. command to update settings to ignore a property from validation)
								return codeAction;
							}
							return this.resolveCodeAction(codeAction, finalUtils);
						}).collect(Collectors.toList());

				ExtendedCodeAction.sort(resolvedCodeActions);
				return resolvedCodeActions;
			}
			// sort code actions by relevant
			ExtendedCodeAction.sort(codeActions);
			return codeActions;
		} catch (IOException e) {
			LOGGER.error("Failed to compute code actions: "+ e.getMessage());
			return Collections.emptyList();
		}
	}

	/**
	 * Returns the given unresolved CodeAction with the workspace edit resolved.
	 *
	 * @param unresolved the unresolved CodeAction
	 * @param utils      the JDT utils
	 * @return the given unresolved CodeAction with the workspace edit resolved
	 */
	public CodeAction resolveCodeAction(CodeAction unresolved, IPsiUtils utils) {
		try {
			CodeActionResolveData data = (CodeActionResolveData) unresolved.getData();
			String participantId = data.getParticipantId();
			String uri = data.getDocumentUri();

			PsiFile unit = utils.resolveCompilationUnit(uri);
			if (unit == null) {
				return null;
			}

			utils = utils.refine(utils.getModule(uri));

			int start = DiagnosticsHelper.getStartOffset(unit, data.getRange(), utils);
			int end = DiagnosticsHelper.getEndOffset(unit, data.getRange(), utils);

			var params = new MicroProfileJavaCodeActionParams();
			params.setContext(new CodeActionContext(
					unresolved.getDiagnostics() == null ? Collections.emptyList() : unresolved.getDiagnostics()));
			params.setResourceOperationSupported(data.isResourceOperationSupported());
			params.setCommandConfigurationUpdateSupported(data.isCommandConfigurationUpdateSupported());
			params.setRange(data.getRange());
			params.setTextDocument(new VersionedTextDocumentIdentifier(uri, null));

			JavaCodeActionResolveContext context = new JavaCodeActionResolveContext(unit,
					start, end - start, utils, params, unresolved);
			context.setASTRoot(getASTRoot(unit));

			IJavaCodeActionParticipant participant = JavaCodeActionDefinition.EP.extensions()
					.filter(definition -> unresolved.getKind().startsWith(definition.getKind()))
					.filter(definition -> participantId.equals(definition.getParticipantId()))
					.findFirst().orElse(null);
			return participant.resolveCodeAction(context.copy());
		} catch (IOException e) {
			LOGGER.error("Failed to resolve code action: "+ e.getMessage());
			return unresolved;
		}
	}


	private static PsiFile getASTRoot(PsiFile unit) {
		return unit;
	}

	private static String getCodeString(Either<String, Integer> code) {
		if (code == null || code.isRight()) {
			return null;
		}
		return code.getLeft();
	}
}
