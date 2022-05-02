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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.config.java;

import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.IJavaDiagnosticsParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.metrics.MicroProfileMetricsConstants;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.List;

/**
 * Validate "defaultValue" attribute of @ConfigProperty and generate
 * diagnostics if "defaultValue" cannot be represented by the given
 * field type.
 *
 * See https://github.com/eclipse/microprofile-config/blob/master/spec/src/main/asciidoc/converters.asciidoc
 * for more details on default converters.
 */
public class MicroProfileConfigPropertyDiagnosticsParticipant implements IJavaDiagnosticsParticipant {

	@Override
	public List<Diagnostic> collectDiagnostics(JavaDiagnosticsContext context) {
		List<Diagnostic> diagnostics = new ArrayList<>();
		validateClass(diagnostics, context);
		return diagnostics;
	}

	private void validateClass(List<Diagnostic> diagnostics, JavaDiagnosticsContext context) {
		PsiFile ast = context.getTypeRoot();
		ast.accept(new ConfigPropertiesAnnotationValidator(diagnostics, context));
	}

	public class ConfigPropertiesAnnotationValidator extends JavaRecursiveElementVisitor {
		private List<Diagnostic> diagnostics;
		private JavaDiagnosticsContext context;

		public ConfigPropertiesAnnotationValidator(List<Diagnostic> diagnostics, JavaDiagnosticsContext context) {
			this.diagnostics = diagnostics;
			this.context = context;
		}

		@Override
		public void visitAnnotation(PsiAnnotation node) {
			if (AnnotationUtils.isMatchAnnotation(node, MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION)) {
				PsiAnnotationMemberValue defValueExpr = AnnotationUtils.getAnnotationMemberValueExpression(node, MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE);
				PsiField parent = PsiTreeUtil.getParentOfType(node, PsiField.class);
				if (defValueExpr instanceof PsiLiteral && ((PsiLiteral) defValueExpr).getValue() instanceof String && parent != null) {
					String defValue = (String) ((PsiLiteral) defValueExpr).getValue();
					PsiField field = (PsiField) parent;
					PsiType fieldBinding = field.getType();
					if (fieldBinding != null && !isAssignable(fieldBinding, defValue)) {
						Range range = context.getUtils().toRange(context.getTypeRoot(), defValueExpr.getTextOffset(), defValueExpr.getTextLength());
						diagnostics.add(new Diagnostic(range,
								"'" + defValue + "'" + " does not match the expected type of '" + fieldBinding.getPresentableText() + "'.",
								DiagnosticSeverity.Error, MicroProfileMetricsConstants.DIAGNOSTIC_SOURCE));
					}
				}
			}
		}

		private boolean isAssignable(PsiType fieldBinding, String defValue) {
			String fqn = fieldBinding.getCanonicalText();
			try {
				if (fqn.startsWith("java.lang.Class")) {
					return Class.forName(defValue) != null;
				} else {
					switch (fqn) {
						case "boolean":
						case "java.lang.Boolean":
							return Boolean.valueOf(defValue) != null;
						case "byte":
						case "java.lang.Byte":
							return Byte.valueOf(defValue) != null;
						case "short":
						case "java.lang.Short":
							return Short.valueOf(defValue) != null;
						case "int":
						case "java.lang.Integer":
							return Integer.valueOf(defValue) != null;
						case "long":
						case "java.lang.Long":
							return Long.valueOf(defValue) != null;
						case "float":
						case "java.lang.Float":
							return Float.valueOf(defValue) != null;
						case "double":
						case "java.lang.Double":
							return Double.valueOf(defValue) != null;
						case "char":
						case "java.lang.Character":
							return Character.valueOf(defValue.charAt(0)) != null;
						case "java.lang.Class":
							return  Class.forName(defValue) != null;
						case "java.lang.String":
							return true;
						default:
							return false;
					}
				}
			} catch (NumberFormatException | ClassNotFoundException e) {
				return false;
			}
		}
	}
}
