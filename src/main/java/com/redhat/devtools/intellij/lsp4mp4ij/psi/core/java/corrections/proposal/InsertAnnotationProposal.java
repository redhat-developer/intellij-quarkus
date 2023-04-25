/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copied from /org.eclipse.jdt.ui/src/org/eclipse/jdt/internal/ui/text/correction/proposals/NewAnnotationMemberProposal.java
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.eclipse.lsp4j.CodeActionKind;

public class InsertAnnotationProposal extends ASTRewriteCorrectionProposal {

	private final PsiFile fInvocationNode;
	private final PsiModifierListOwner fBinding;

	private final String[] annotations;

	public InsertAnnotationProposal(String label, PsiFile targetCU, PsiFile invocationNode,
									PsiModifierListOwner binding, int relevance, PsiFile sourceCU, String... annotations) {
		super(label, CodeActionKind.QuickFix, targetCU, relevance, sourceCU);
		fInvocationNode = invocationNode;
		fBinding = binding;
		this.annotations = annotations;
	}

	@Override
	public void performUpdate() {
		if (annotations == null || annotations.length == 0) {
			return;
		}
		if (!(fBinding.getContainingFile() instanceof PsiJavaFile)) {
			return;
		}
		PsiImportList imports = ((PsiJavaFile) fBinding.getContainingFile()).getImportList();
		if (imports == null) {
			//should we bail here???
			return;
		}
		for(String annotation : annotations) {
			PsiClass annotationClass = JavaPsiFacade.getInstance(fBinding.getProject()).
					findClass(annotation, GlobalSearchScope.allScope(fBinding.getProject()));
			if (annotationClass != null && annotationClass.getName() != null) {
				//Add annotation to binding
				fBinding.getModifierList().addAnnotation(annotationClass.getName());
				//Add import annotation if missing
				if (!importExists(annotation, imports)) {
					imports.add(PsiElementFactory.getInstance(fBinding.getProject()).createImportStatement(annotationClass));
				}
			}
		}
	}

	private boolean importExists(String annotation, PsiImportList imports) {
		for (PsiImportStatement importStatement : imports.getImportStatements()) {
			if (importStatement.isOnDemand() && importStatement.getQualifiedName() != null  && importStatement.getQualifiedName().startsWith(annotation.substring(0, annotation.lastIndexOf('.')))) {
				// eg. check import jakarta.inject.*
				return true;
			} else if (annotation.equals(importStatement.getQualifiedName())) {
				// eg. check import jakarta.inject.Inject
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the Compilation Unit node
	 * 
	 * @return the invocation node for the Compilation Unit
	 */
	protected PsiFile getInvocationNode() {
		return this.fInvocationNode;
	}

	/**
	 * Returns the Binding object associated with the new annotation change
	 * 
	 * @return the binding object
	 */
	protected PsiModifierListOwner getBinding() {
		return this.fBinding;
	}

	/**
	 * Returns the annotations list
	 * 
	 * @return the list of new annotations to add
	 */
	protected String[] getAnnotations() {
		return this.annotations;
	}
}
