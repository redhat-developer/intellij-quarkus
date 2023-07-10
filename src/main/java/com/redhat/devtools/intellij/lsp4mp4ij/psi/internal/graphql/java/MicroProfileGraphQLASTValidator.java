/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.graphql.java;


import java.util.logging.Logger;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.JavaASTValidator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import org.eclipse.lsp4j.DiagnosticSeverity;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.graphql.MicroProfileGraphQLConstants;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.isMatchAnnotation;

/**
 * Diagnostics for microprofile-graphql.
 *
 * @see https://download.eclipse.org/microprofile/microprofile-graphql-1.0/microprofile-graphql.html
 */
public class MicroProfileGraphQLASTValidator extends JavaASTValidator {

	private static final Logger LOGGER = Logger.getLogger(MicroProfileGraphQLASTValidator.class.getName());

	private static final String NO_VOID_MESSAGE = "Methods annotated with microprofile-graphql's `@Query` cannot have 'void' as a return type.";
	
	@Override
	public boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context) {
		Module javaProject = context.getJavaProject();
		// Check if microprofile-graphql is on the path
		return PsiTypeUtils.findType(javaProject, MicroProfileGraphQLConstants.QUERY_ANNOTATION) != null;
	}

	@Override
	public void visitMethod(PsiMethod node) {
		validateMethod(node);
		super.visitMethod(node);
	}

	private void validateMethod(PsiMethod node) {
		if (node.getReturnTypeElement() == null) {
			//Ignore constructor
			return;
		}
		for (PsiAnnotation annotation : node.getAnnotations()) {
			if (isMatchAnnotation(annotation, MicroProfileGraphQLConstants.QUERY_ANNOTATION) ) {
				PsiType returnType = node.getReturnType();
				if (PsiType.VOID.equals(returnType)) {
					super.addDiagnostic(NO_VOID_MESSAGE, //
							MicroProfileGraphQLConstants.DIAGNOSTIC_SOURCE, //
							node.getReturnTypeElement(), //
							MicroProfileGraphQLErrorCode.NO_VOID_QUERIES, //
							DiagnosticSeverity.Error);
				}
				return;
			}
		}
	}

}