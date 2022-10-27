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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.openapi.java;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.openapi.MicroProfileOpenAPIConstants;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Generate OpenAPI annotations by the "Source" kind code action.
 *
 * @author Benson Ning
 *
 */
public class MicroProfileGenerateOpenAPIOperation implements IJavaCodeActionParticipant {

	@Override
	public boolean isAdaptedForCodeAction(JavaCodeActionContext context) {
		Module javaProject = context.getJavaProject();
		return PsiTypeUtils.findType(javaProject, MicroProfileOpenAPIConstants.OPERATION_ANNOTATION) != null;
	}

	@Override
	public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
		List<CodeAction> codeActions = new ArrayList<>();
		PsiFile cu = context.getASTRoot();
		Collection<PsiClass> types = PsiTreeUtil.findChildrenOfType(cu, PsiClass.class);
		for (Object type : types) {
			if (type instanceof PsiClass) {
				ChangeCorrectionProposal proposal = new OpenAPIAnnotationProposal(
						"Generate OpenAPI Annotations", context.getCompilationUnit(), context.getASTRoot(),
						(PsiClass) type, MicroProfileOpenAPIConstants.OPERATION_ANNOTATION, 0,
						context.getSource().getCompilationUnit());
				// Convert the proposal to LSP4J CodeAction
				CodeAction codeAction = context.convertToCodeAction(proposal);
				if (codeAction != null) {
					codeActions.add(codeAction);
				}
			}
		}
		return codeActions;
	}
}
