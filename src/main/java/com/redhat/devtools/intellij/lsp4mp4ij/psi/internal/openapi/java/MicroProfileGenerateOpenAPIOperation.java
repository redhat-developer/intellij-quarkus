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
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.ExtendedCodeAction;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.openapi.MicroProfileOpenAPIConstants;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;
import org.eclipse.lsp4mp.commons.codeaction.MicroProfileCodeActionId;

import java.text.MessageFormat;
import java.util.*;

/**
 * Generate OpenAPI annotations by the "Source" kind code action.
 *
 * @author Benson Ning
 *
 */
public class MicroProfileGenerateOpenAPIOperation implements IJavaCodeActionParticipant {

	private final static String MESSAGE = "Generate OpenAPI Annotations for ''{0}''";

	private final static String TYPE_NAME_KEY = "type";

	@Override
	public String getParticipantId() {
		return MicroProfileGenerateOpenAPIOperation.class.getName();
	}

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
				PsiClass typeDeclaration = (PsiClass) type;
				String typeName = typeDeclaration.getQualifiedName();
				if (typeName == null || typeName.isBlank()) {
					continue;
				}

				Map<String, Object> extendedData = new HashMap<>();
				extendedData.put(TYPE_NAME_KEY, typeName);
				CodeActionResolveData data = new CodeActionResolveData(context.getUri(), getParticipantId(),
						context.getParams().getRange(),
						extendedData, context.getParams().isResourceOperationSupported(),
						context.getParams().isCommandConfigurationUpdateSupported(),
						MicroProfileCodeActionId.GenerateOpenApiAnnotations);

				ExtendedCodeAction codeAction = new ExtendedCodeAction(
						MessageFormat.format(MESSAGE, getSimpleName(typeName)));
				codeAction.setData(data);
				codeAction.setRelevance(0);
				codeAction.setKind(CodeActionKind.Source);
				codeActions.add(codeAction);
			}
		}
		return codeActions;
	}

	@Override
	public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {

		CodeAction toResolve = context.getUnresolved();
		CodeActionResolveData data = (CodeActionResolveData) toResolve.getData();
		String typeName = (String) data.getExtendedDataEntry(TYPE_NAME_KEY);

		if (StringUtils.isEmpty(typeName)) {
			return toResolve;
		}

		PsiFile cu = context.getASTRoot();
		@SuppressWarnings("unchecked")
		Optional<PsiClass> typeDeclarationOpt = PsiTreeUtil.findChildrenOfType(cu, PsiClass.class).stream() //
				.filter(type -> type instanceof PsiClass
						&& typeName.equals(((PsiClass) type).getQualifiedName())) //
				.map(type -> (PsiClass) type) //
				.findFirst();

		if (typeDeclarationOpt.isEmpty()) {
			return toResolve;
		}

		PsiClass typeDeclaration = typeDeclarationOpt.get();

		ChangeCorrectionProposal proposal = new OpenAPIAnnotationProposal(
				MessageFormat.format(MESSAGE, getSimpleName(typeName)), context.getCompilationUnit(),
				context.getASTRoot(),
				typeDeclaration, MicroProfileOpenAPIConstants.OPERATION_ANNOTATION, 0,
				context.getSource().getCompilationUnit());

		try {
			toResolve.setEdit(context.convertToWorkspaceEdit(proposal));
		} catch (Exception e) {
		}

		return toResolve;
	}


	private static final String getSimpleName(String fullyQualifiedName) {
		int lastDot = fullyQualifiedName.lastIndexOf('.');
		if (lastDot == -1) {
			// It probably wasn't actually fully qualified :|
			return fullyQualifiedName;
		}
		return fullyQualifiedName.substring(lastDot + 1);
	}

}
