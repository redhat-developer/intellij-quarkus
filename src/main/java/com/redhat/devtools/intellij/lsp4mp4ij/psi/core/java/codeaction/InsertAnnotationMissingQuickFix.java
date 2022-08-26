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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction;

import java.util.ArrayList;
import java.util.List;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.InsertAnnotationProposal;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

/**
 * QuickFix for inserting annotations.
 *
 * @author Angelo ZERR
 *
 */
public class InsertAnnotationMissingQuickFix implements IJavaCodeActionParticipant {

	private final String[] annotations;

	private final boolean generateOnlyOneCodeAction;

	/**
	 * Constructor for insert annotation quick fix.
	 *
	 * <p>
	 * The participant will generate a CodeAction per annotation.
	 * </p>
	 *
	 * @param annotations list of annotation to insert.
	 */
	public InsertAnnotationMissingQuickFix(String... annotations) {
		this(false, annotations);
	}

	/**
	 * Constructor for insert annotation quick fix.
	 *
	 * @param generateOnlyOneCodeAction true if the participant must generate a
	 *                                  CodeAction which insert the list of
	 *                                  annotation and false otherwise.
	 * @param annotations               list of annotation to insert.
	 */
	public InsertAnnotationMissingQuickFix(boolean generateOnlyOneCodeAction, String... annotations) {
		this.generateOnlyOneCodeAction = generateOnlyOneCodeAction;
		this.annotations = annotations;
	}

	@Override
	public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
		PsiElement node = context.getCoveringNode();
		PsiModifierListOwner parentType = getBinding(node);
		if (parentType != null) {
			List<CodeAction> codeActions = new ArrayList<>();
			insertAnnotations(diagnostic, context, parentType, codeActions);
			return codeActions;
		}
		return null;
	}

	protected PsiModifierListOwner getBinding(PsiElement node) {
		PsiModifierListOwner binding = PsiTreeUtil.getParentOfType(node, PsiVariable.class);
		if (binding == null) {
			return PsiTreeUtil.getParentOfType(node, PsiClass.class);
		}
		return binding;
	}

	protected String[] getAnnotations() {
		return this.annotations;
	}

	protected void insertAnnotations(Diagnostic diagnostic, JavaCodeActionContext context, PsiModifierListOwner parentType,
			List<CodeAction> codeActions) {
		if (generateOnlyOneCodeAction) {
			insertAnnotation(diagnostic, context, parentType, codeActions, annotations);
		} else {
			for (String annotation : annotations) {
				JavaCodeActionContext annotationContext = context.oopy();
				PsiElement node = annotationContext.getCoveringNode();
				PsiModifierListOwner targetParentType = getBinding(node);
				if (targetParentType != null) {
					insertAnnotation(diagnostic, annotationContext, targetParentType, codeActions, annotation);
				}
			}
		}
	}

	protected static void insertAnnotation(Diagnostic diagnostic, JavaCodeActionContext context, PsiModifierListOwner parentType,
			List<CodeAction> codeActions, String... annotations) {
		// Insert the annotation and the proper import by using JDT Core Manipulation
		// API
		String name = getLabel(annotations);
		ChangeCorrectionProposal proposal = new InsertAnnotationProposal(name, context.getCompilationUnit(),
				context.getASTRoot(), parentType, 0, context.getSource().getCompilationUnit(), annotations);
		// Convert the proposal to LSP4J CodeAction
		CodeAction codeAction = context.convertToCodeAction(proposal, diagnostic);
		if (codeAction != null) {
			codeActions.add(codeAction);
		}
	}

	private static String getLabel(String[] annotations) {
		StringBuilder name = new StringBuilder("Insert ");
		for (int i = 0; i < annotations.length; i++) {
			String annotation = annotations[i];
			String annotationName = annotation.substring(annotation.lastIndexOf('.') + 1, annotation.length());
			if (i > 0) {
				name.append(", ");
			}
			name.append("@");
			name.append(annotationName);
		}
		return name.toString();
	}

}
