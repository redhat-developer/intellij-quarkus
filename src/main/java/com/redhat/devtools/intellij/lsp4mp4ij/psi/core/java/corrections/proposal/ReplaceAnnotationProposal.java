/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copied from /org.eclipse.jdt.ui/src/org/eclipse/jdt/internal/ui/text/correction/proposals/NewAnnotationMemberProposal.java
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal;

import java.util.Arrays;
import java.util.List;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;

/**
 * Similar functionality as NewAnnotationProposal. The main difference is that
 * first removes specified annotations before adding a new annotation.
 * 
 * Note: This class only accepts one new annotation to add.
 * 
 * @author Kathryn Kodama
 */
public class ReplaceAnnotationProposal extends InsertAnnotationProposal {

	private final String[] removeAnnotations;

	public ReplaceAnnotationProposal(String label, PsiFile targetCU, PsiFile invocationNode,
									 PsiModifierListOwner binding, int relevance, String annotation, PsiFile sourceCU,
									 String... removeAnnotations) {
		super(label, targetCU, invocationNode, binding, relevance, sourceCU, annotation);
		this.removeAnnotations = removeAnnotations;
	}

	@Override
	public void performUpdate() {
		super.performUpdate();
		PsiModifierList list = getBinding().getModifierList();
		for(String annotationFQCN : removeAnnotations) {
			PsiAnnotation annotation = list.findAnnotation(annotationFQCN);
			if (annotation != null) {
				if (getBinding().getContainingFile() instanceof PsiJavaFile) {
					PsiImportStatement statement = ((PsiJavaFile) getBinding().getContainingFile()).getImportList().
							findSingleClassImportStatement(annotationFQCN);
					if (statement != null) {
						statement.delete();
					}
				}
				annotation.delete();
			}
		}
	}
}