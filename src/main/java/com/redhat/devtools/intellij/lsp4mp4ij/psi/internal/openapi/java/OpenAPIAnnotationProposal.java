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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.openapi.java;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ASTRewriteCorrectionProposal;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.jaxrs.JaxRsConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.openapi.MicroProfileOpenAPIConstants;
import org.eclipse.lsp4j.CodeActionKind;

import java.util.ArrayList;
import java.util.List;

/**
 * A proposal for generating OpenAPI annotations that works on an AST rewrite.  
 *  
 * @author Benson Ning
 *
 */
public class OpenAPIAnnotationProposal extends ASTRewriteCorrectionProposal {

	private final PsiFile fInvocationNode;
	private final PsiClass fTypeNode;
	private final String fAnnotation;
	
	public OpenAPIAnnotationProposal(String label, PsiFile targetCU, PsiFile invocationNode,
									 PsiClass type, String annotation, int relevance, PsiFile sourceCU) {
		super(label, CodeActionKind.Source, targetCU, relevance, sourceCU);
		this.fInvocationNode = invocationNode;
		this.fTypeNode = type;
		this.fAnnotation = annotation;
	}

	@Override
	public void performUpdate() {
		PsiMethod[] methods = fTypeNode.getMethods();
		List<PsiMethod> responseReturnMethods = new ArrayList<>();
		for(PsiMethod method : methods) {
			boolean operationFlag = false;
			if (method.getReturnType() instanceof PsiClassType && JaxRsConstants.JAVAX_WS_RS_RESPONSE_TYPE.
					equals(((PsiClassType) method.getReturnType()).resolve().getQualifiedName())) {
				if (method.getModifierList().hasAnnotation(MicroProfileOpenAPIConstants.OPERATION_ANNOTATION)) {
					operationFlag = true;
					break;
				}
			}
			if (!operationFlag) {
				responseReturnMethods.add(method);
			}
		}
		if (!responseReturnMethods.isEmpty()) {
			PsiClass annotationClass = JavaPsiFacade.getInstance(fTypeNode.getProject()).
					findClass(fAnnotation, GlobalSearchScope.allScope(fTypeNode.getProject()));
			if (annotationClass != null) {
				if (fTypeNode.getContainingFile() instanceof PsiJavaFile) {
					if (((PsiJavaFile) fTypeNode.getContainingFile()).getImportList().
							findSingleClassImportStatement(annotationClass.getQualifiedName()) == null) {
						((PsiJavaFile) fTypeNode.getContainingFile()).getImportList().
								add(PsiElementFactory.getInstance(fTypeNode.getProject()).
										createImportStatement(annotationClass));
					}
				}
				for(PsiMethod method : responseReturnMethods) {
					PsiAnnotation annotation = method.getModifierList().addAnnotation(annotationClass.getName());
					annotation.setDeclaredAttributeValue("summary", PsiElementFactory.getInstance(fTypeNode.getProject()).createExpressionFromText("\"\"", annotation));
					annotation.setDeclaredAttributeValue("description", PsiElementFactory.getInstance(fTypeNode.getProject()).createExpressionFromText("\"\"", annotation));
				}
			}
		}
	}
}
