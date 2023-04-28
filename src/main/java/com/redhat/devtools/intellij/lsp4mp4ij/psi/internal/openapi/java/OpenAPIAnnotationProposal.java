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

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
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

	private final PsiClass fTypeNode;
	private final String fAnnotation;
	
	public OpenAPIAnnotationProposal(String label, PsiFile targetCU, PsiFile invocationNode,
									 PsiClass type, String annotation, int relevance, PsiFile sourceCU) {
		super(label, CodeActionKind.Source, targetCU, relevance, sourceCU);
		this.fTypeNode = type;
		this.fAnnotation = annotation;
	}

	@Override
	public void performUpdate() {
		PsiMethod[] methods = fTypeNode.getMethods();
		List<PsiMethod> responseReturnMethods = new ArrayList<>();
		for(PsiMethod method : methods) {
			if (method.getReturnType() instanceof PsiClassType){
				PsiClass returnType = ((PsiClassType) method.getReturnType()).resolve();
				if (isResponseType(returnType) && !method.getModifierList().hasAnnotation(MicroProfileOpenAPIConstants.OPERATION_ANNOTATION)) {
					responseReturnMethods.add(method);
				}
			}
		}
		if (!responseReturnMethods.isEmpty()) {
			Project project = fTypeNode.getProject();
			PsiClass annotationClass = JavaPsiFacade.getInstance(project).
					findClass(fAnnotation, GlobalSearchScope.allScope(project));
			if (annotationClass != null) {
				if (fTypeNode.getContainingFile() instanceof PsiJavaFile) {
					PsiImportList importList = ((PsiJavaFile) fTypeNode.getContainingFile()).getImportList();
					if (importList != null && importList.findSingleClassImportStatement(annotationClass.getQualifiedName()) == null) {
						importList.add(PsiElementFactory.getInstance(project).createImportStatement(annotationClass));
					}
				}
				for(PsiMethod method : responseReturnMethods) {
					PsiAnnotation annotation = method.getModifierList().addAnnotation(annotationClass.getName());
					PsiExpression expression = PsiElementFactory.getInstance(project).createExpressionFromText("\"\"", annotation);
					annotation.setDeclaredAttributeValue("summary", expression);
					annotation.setDeclaredAttributeValue("description", expression);
				}
			}
		}
	}

	private boolean isResponseType(PsiClass returnType) {
		if (returnType == null) {
			return false;
		}
		String qualifiedMethodName = returnType.getQualifiedName();
		return (JaxRsConstants.JAVAX_WS_RS_RESPONSE_TYPE.equals(qualifiedMethodName)
				|| JaxRsConstants.JAKARTA_WS_RS_RESPONSE_TYPE.equals(qualifiedMethodName));
	}
}
