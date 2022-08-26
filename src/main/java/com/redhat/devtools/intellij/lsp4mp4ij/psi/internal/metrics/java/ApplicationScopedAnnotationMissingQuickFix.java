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

import java.util.List;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifierListOwner;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.InsertAnnotationMissingQuickFix;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ReplaceAnnotationProposal;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.metrics.MicroProfileMetricsConstants;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

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
public class ApplicationScopedAnnotationMissingQuickFix extends InsertAnnotationMissingQuickFix {

	private static final String[] REMOVE_ANNOTATION_NAMES = new String[] {
			MicroProfileMetricsConstants.REQUEST_SCOPED_ANNOTATION,
			MicroProfileMetricsConstants.SESSION_SCOPED_ANNOTATION,
			MicroProfileMetricsConstants.DEPENDENT_ANNOTATION };

	public ApplicationScopedAnnotationMissingQuickFix() {
		super(MicroProfileMetricsConstants.APPLICATION_SCOPED_ANNOTATION);
	}

	@Override
	protected void insertAnnotations(Diagnostic diagnostic, JavaCodeActionContext context, PsiModifierListOwner parentType,
									 List<CodeAction> codeActions) {
		String[] annotations = getAnnotations();
		for (String annotation : annotations) {
			insertAndReplaceAnnotation(diagnostic, context, parentType, codeActions, annotation);
		}
	}

	private static void insertAndReplaceAnnotation(Diagnostic diagnostic, JavaCodeActionContext context,
												   PsiModifierListOwner parentType, List<CodeAction> codeActions, String annotation) {
		// Insert the annotation and the proper import by using JDT Core Manipulation
		// API
		String name = getLabel(annotation);
		ChangeCorrectionProposal proposal = new ReplaceAnnotationProposal(name, context.getCompilationUnit(),
				context.getASTRoot(), parentType, 0, annotation, context.getSource().getCompilationUnit(),
				REMOVE_ANNOTATION_NAMES);
		// Convert the proposal to LSP4J CodeAction
		CodeAction codeAction = context.convertToCodeAction(proposal, diagnostic);
		if (codeAction != null) {
			codeActions.add(codeAction);
		}
	}

	private static String getLabel(String annotation) {
		StringBuilder name = new StringBuilder("Replace current scope with ");
		String annotationName = annotation.substring(annotation.lastIndexOf('.') + 1, annotation.length());
		name.append("@");
		name.append(annotationName);
		return name.toString();
	}

}
