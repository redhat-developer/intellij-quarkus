/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
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
package com.redhat.microprofile.psi.internal.quarkus.core.java;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.JavaASTValidator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.text.MessageFormat;
import java.util.logging.Logger;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.isMatchAnnotation;

/**
 * Quarkus @ConfigMapping validator.
 * 
 * @author Angelo ZERR
 *
 */
public class QuarkusConfigMappingASTVisitor extends JavaASTValidator {

	private static final Logger LOGGER = Logger.getLogger(QuarkusConfigMappingASTVisitor.class.getName());

	private static final String EXPECTED_INTERFACE_ERROR = "The @ConfigMapping annotation can only be placed in interfaces, class `{0}` is a class";

	@Override
	public boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context) {
		Module javaProject = context.getJavaProject();
		return PsiTypeUtils.findType(javaProject, QuarkusConstants.CONFIG_MAPPING_ANNOTATION) != null;
	}

	@Override
	public void visitClass(PsiClass node) {
		for (PsiAnnotation annotation : node.getAnnotations()) {
			if (isMatchAnnotation(annotation, QuarkusConstants.CONFIG_MAPPING_ANNOTATION)) {
				validateConfigMappingAnnotation(node, annotation);
			}
		}
	}

	/**
	 * Checks if the given type declaration annotated with @ConfigMapping annotation
	 * is an interface.
	 *
	 * @param node       The type declaration to validate
	 * @param annotation The @ConfigMapping annotation
	 */
	private void validateConfigMappingAnnotation(PsiClass node, PsiAnnotation annotation) {
		if (!node.isInterface()) {
			super.addDiagnostic(MessageFormat.format(EXPECTED_INTERFACE_ERROR, node.getName()),
					QuarkusConstants.QUARKUS_PREFIX, annotation, null, DiagnosticSeverity.Error);
		}
	}

}