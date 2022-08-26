/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copied from /org.eclipse.jdt.ui/src/org/eclipse/jdt/internal/ui/text/correction/proposals/MissingAnnotationAttributesProposal.java
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerExpression;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import org.eclipse.lsp4j.CodeActionKind;

public class InsertAnnotationAttributeProposal extends ASTRewriteCorrectionProposal {

	private final PsiAnnotation fAnnotation;
	private final Set<String> attributes;

	public InsertAnnotationAttributeProposal(String label, PsiFile targetCU, PsiAnnotation annotation,
											 int relevance, PsiFile sourceCU, String... attributes) {
		super(label, CodeActionKind.QuickFix, annotation, relevance, sourceCU);
		this.attributes = new HashSet<>(Arrays.asList(attributes));
		this.fAnnotation = annotation;
	}

	@Override
	public void performUpdate() {
		addDefinedAtributes();
	}

	private void addDefinedAtributes() {
		Set<String> implementedAttribs = new HashSet<String>();
		for(PsiNameValuePair pair : fAnnotation.getParameterList().getAttributes()) {
			implementedAttribs.add(pair.getAttributeName());
		}
		for(PsiMethod method : fAnnotation.resolveAnnotationType().getMethods()) {
			if (!implementedAttribs.contains(method.getName()) && attributes.contains(method.getName())) {
				fAnnotation.setDeclaredAttributeValue(method.getName(), newDefaultExpression(method.getReturnType()));
			}
		}
	}

	private PsiAnnotationMemberValue newDefaultExpression(PsiType type) {
		if (type instanceof PsiPrimitiveType) {
			String name = ((PsiPrimitiveType) type).getName();
			if ("boolean".equals(name)) { //$NON-NLS-1$
				return PsiElementFactory.getInstance(fAnnotation.getProject()).
						createExpressionFromText("true", fAnnotation);
			} else {
				return PsiElementFactory.getInstance(fAnnotation.getProject()).
						createExpressionFromText("0", fAnnotation);
			}
		}
		if (type.equals(PsiType.getJavaLangString(fAnnotation.getManager(),
				GlobalSearchScope.allScope(fAnnotation.getProject())))) { //$NON-NLS-1$
			return PsiElementFactory.getInstance(fAnnotation.getProject()).
					createExpressionFromText("\"\"", fAnnotation);
		}
		if (type instanceof PsiArrayType) {
			PsiArrayInitializerExpression initializer = (PsiArrayInitializerExpression) PsiElementFactory.getInstance(fAnnotation.getProject()).
					createExpressionFromText(type.getCanonicalText(true), fAnnotation);
			initializer.add(newDefaultExpression(((PsiArrayType) type).getComponentType()));
			return initializer;
		}
		if (type instanceof PsiClassType && ((PsiClassType) type).resolve().isAnnotationType()) {
			return PsiElementFactory.getInstance(fAnnotation.getProject()).createExpressionFromText("@" + ((PsiClassType) type).resolve().getQualifiedName(), fAnnotation);
		}
		return PsiElementFactory.getInstance(fAnnotation.getProject()).createExpressionFromText("null", fAnnotation);
	}

}
