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
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.java.corrections.DiagnosticsHelper;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Code action handler.
 *
 * @author Angelo ZERR
 *
 */
public class CodeActionHandler {

	/**
	 *
	 * @param params
	 * @param utils
	 * @return
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
					utils.getModule(), params);
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
							codeActions.addAll(definition.getCodeActions(context.oopy(), null));
						}
					}
				}
			}

			if (!forDiagnostics.isEmpty()) {
				// It exists code action to fix diagnostics, loop for each diagnostics
				params.getContext().getDiagnostics().forEach(diagnostic -> {
					String code = getCode(diagnostic);
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
								codeActions.addAll(definition.getCodeActions(context.oopy(), diagnostic));
							}
						}
					}
				});
			}
			// sort code actions by relevant
			ExtendedCodeAction.sort(codeActions);
			return codeActions;
		} catch (IOException e) {
			return Collections.emptyList();
		}
	}

	private static PsiFile getASTRoot(PsiFile unit) {
		return unit;
	}

	private static String getCode(Diagnostic diagnostic) {
		Object code = null;
		try {
			Field f = diagnostic.getClass().getDeclaredField("code");
			f.setAccessible(true);
			code = f.get(diagnostic);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getCodeString(code);
	}

	private static String getCodeString(Object codeObject) {
		if (codeObject instanceof String) {
			return ((String) codeObject);
		}
		@SuppressWarnings("unchecked")
		Either<String, Number> code = (Either<String, Number>) codeObject;
		if (code == null || code.isRight()) {
			return null;
		}
		return code.getLeft();
	}
}
