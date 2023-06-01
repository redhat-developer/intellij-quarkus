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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.InsertAnnotationProposal;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;
import org.eclipse.lsp4mp.commons.codeaction.ICodeActionId;

/**
 * QuickFix for inserting annotations.
 *
 * @author Angelo ZERR
 *
 */
public abstract class InsertAnnotationMissingQuickFix implements IJavaCodeActionParticipant {

	private static final Logger LOGGER = Logger.getLogger(InsertAnnotationMissingQuickFix.class.getName());

	private static final String ANNOTATION_KEY = "annotation";

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
		List<CodeAction> codeActions = new ArrayList<>();
		insertAnnotations(diagnostic, context, codeActions);
		return codeActions;
	}

	@Override
	public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {

		CodeAction toResolve = context.getUnresolved();
		CodeActionResolveData data = (CodeActionResolveData) toResolve.getData();
		List<String> resolveAnnotations = (List<String>) data.getExtendedDataEntry(ANNOTATION_KEY);
		String[] resolveAnnotationsArray = resolveAnnotations.toArray(String[]::new);
		String name = getLabel(resolveAnnotationsArray);
		PsiElement node = context.getCoveringNode();
		PsiModifierListOwner parentType = getBinding(node);

		ChangeCorrectionProposal proposal = new InsertAnnotationProposal(name, context.getCompilationUnit(),
				context.getASTRoot(), parentType, 0, context.getSource().getCompilationUnit(),
				resolveAnnotationsArray);
		try {
			WorkspaceEdit we = context.convertToWorkspaceEdit(proposal);
			toResolve.setEdit(we);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Unable to create workspace edit for code action to insert missing annotation", e);
		}

		return toResolve;
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

	protected void insertAnnotations(Diagnostic diagnostic, JavaCodeActionContext context,
			List<CodeAction> codeActions) {
		if (generateOnlyOneCodeAction) {
			insertAnnotation(diagnostic, context, codeActions, annotations);
		} else {
			for (String annotation : annotations) {
				JavaCodeActionContext annotationContext = context.copy();
				insertAnnotation(diagnostic, annotationContext, codeActions, annotation);
			}
		}
	}

	protected void insertAnnotation(Diagnostic diagnostic, JavaCodeActionContext context,
			List<CodeAction> codeActions, String... annotations) {
		String name = getLabel(annotations);
		ExtendedCodeAction codeAction = new ExtendedCodeAction(name);
		codeAction.setRelevance(0);
		codeAction.setDiagnostics(Collections.singletonList(diagnostic));
		codeAction.setKind(CodeActionKind.QuickFix);

		Map<String, Object> extendedData = new HashMap<>();
		extendedData.put(ANNOTATION_KEY, Arrays.asList(annotations));
		codeAction.setData(new CodeActionResolveData(context.getUri(), getParticipantId(),
				context.getParams().getRange(), extendedData,
				context.getParams().isResourceOperationSupported(),
				context.getParams().isCommandConfigurationUpdateSupported(),
				getCodeActionId()));

		codeActions.add(codeAction);
	}

	/**
	 * Returns the id for this code action.
	 *
	 * @return the id for this code action
	 */
	protected abstract ICodeActionId getCodeActionId();

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

	/**
	 * Returns true if all the listed annotations should be added in one code
	 * action, and false if separate code actions should be generated for each
	 * annotation.
	 *
	 * @return true if all the listed annotations should be added in one code
	 *         action, and false if separate code actions should be generated for
	 *         each annotation
	 */
	protected boolean isGenerateOnlyOneCodeAction() {
		return this.generateOnlyOneCodeAction;
	}

}
