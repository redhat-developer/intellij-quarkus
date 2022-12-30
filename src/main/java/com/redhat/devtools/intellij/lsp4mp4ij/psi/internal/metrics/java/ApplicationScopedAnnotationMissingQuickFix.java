/*******************************************************************************
* Copyright (c) 2020 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.metrics.java;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.ExtendedCodeAction;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.InsertAnnotationMissingQuickFix;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ReplaceAnnotationProposal;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.metrics.MicroProfileMetricsConstants;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4mp.commons.CodeActionResolveData;

/**
 * QuickFix for fixing
 * {@link MicroProfileMetricsErrorCode#ApplicationScopedAnnotationMissing} error
 * by providing several code actions:
 * 
 * <ul>
 * <li>Remove @RequestScoped | @SessionScoped | @Dependent annotation</li>
 * <li>Insert @ApplicationScoped annotation and the proper import.</li>
 * </ul>
 * 
 * @author Kathryn Kodama
 *
 */
public class ApplicationScopedAnnotationMissingQuickFix implements IJavaCodeActionParticipant {

	private static final Logger LOGGER = Logger.getLogger(ApplicationScopedAnnotationMissingQuickFix.class.getName());

	private static final String[] REMOVE_ANNOTATION_NAMES = new String[] {
			MicroProfileMetricsConstants.REQUEST_SCOPED_ANNOTATION,
			MicroProfileMetricsConstants.SESSION_SCOPED_ANNOTATION,
			MicroProfileMetricsConstants.DEPENDENT_ANNOTATION };

	private static final String ADD_ANNOTATION = MicroProfileMetricsConstants.APPLICATION_SCOPED_ANNOTATION;

	@Override
	public String getParticipantId() {
		return ApplicationScopedAnnotationMissingQuickFix.class.getName();
	}

	@Override
	public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
		ExtendedCodeAction codeAction = new ExtendedCodeAction(getLabel(ADD_ANNOTATION));
		codeAction.setRelevance(0);
		codeAction.setDiagnostics(Collections.singletonList(diagnostic));
		codeAction.setKind(CodeActionKind.QuickFix);
		codeAction.setData(
				new CodeActionResolveData(context.getUri(), getParticipantId(), context.getParams().getRange(), null,
						context.getParams().isResourceOperationSupported(),
						context.getParams().isCommandConfigurationUpdateSupported()));

		return Collections.singletonList(codeAction);
	}

	@Override
	public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
		CodeAction toResolve = context.getUnresolved();
		String name = getLabel(ADD_ANNOTATION);
		PsiElement node = context.getCoveringNode();
		PsiModifierListOwner parentType = getBinding(node);

		ChangeCorrectionProposal proposal = new ReplaceAnnotationProposal(name, context.getCompilationUnit(),
				context.getASTRoot(), parentType, 0, ADD_ANNOTATION, context.getSource().getCompilationUnit(),
				REMOVE_ANNOTATION_NAMES);
		try {
			WorkspaceEdit we = context.convertToWorkspaceEdit(proposal);
			toResolve.setEdit(we);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to create workspace edit to replace bean scope annotation", e);
		}

		return toResolve;
	}

	private static PsiModifierListOwner getBinding(PsiElement node) {
		PsiModifierListOwner binding = PsiTreeUtil.getParentOfType(node, PsiVariable.class);
		if (binding == null) {
			return PsiTreeUtil.getParentOfType(node, PsiClass.class);
		}
		return binding;
	}

	private static String getLabel(String annotation) {
		StringBuilder name = new StringBuilder("Replace current scope with ");
		String annotationName = annotation.substring(annotation.lastIndexOf('.') + 1, annotation.length());
		name.append("@");
		name.append(annotationName);
		return name.toString();
	}

}
