/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.java;

import com.intellij.openapi.module.Module;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.IJavaDiagnosticsParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.COMPLETION_STAGE_TYPE_UTILITY;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.FUTURE_TYPE_UTILITY;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.isMatchAnnotation;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.MicroProfileFaultToleranceConstants.ASYNCHRONOUS_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.MicroProfileFaultToleranceConstants.FALLBACK_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.MicroProfileFaultToleranceConstants.FALLBACK_METHOD_FALLBACK_ANNOTATION_MEMBER;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.java.MicroProfileFaultToleranceErrorCode.FALLBACK_METHOD_DOES_NOT_EXIST;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.java.MicroProfileFaultToleranceErrorCode.FAULT_TOLERANCE_DEFINITION_EXCEPTION;

/**
 * Validates that the Fallback annotation's fallback method exists
 *
 */
public class MicroProfileFaultToleranceDiagnosticsParticipant implements IJavaDiagnosticsParticipant {

	@Override
	public boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context) {
		Module javaProject = context.getJavaProject();
		return PsiTypeUtils.findType(javaProject, FALLBACK_ANNOTATION) != null || PsiTypeUtils.findType(javaProject, ASYNCHRONOUS_ANNOTATION) != null;
	}

	@Override
	public List<Diagnostic> collectDiagnostics(JavaDiagnosticsContext context) {
		List<Diagnostic> diagnostics = new ArrayList<>();
		validateClass(diagnostics, context);
		return diagnostics;
	}

	private static void validateClass(List<Diagnostic> diagnostics, JavaDiagnosticsContext context) {
		PsiFile ast = context.getTypeRoot();
		ast.accept(new FaultToleranceAnnotationValidator(diagnostics, context));
	}

	private static class FaultToleranceAnnotationValidator extends JavaElementVisitor {

		private final Map<PsiClass, Set<String>> methodsCache;
		private List<Diagnostic> diagnostics;
		private JavaDiagnosticsContext context;

		private static Logger LOGGER = Logger.getLogger(FaultToleranceAnnotationValidator.class.getName());

		public FaultToleranceAnnotationValidator(List<Diagnostic> diagnostics, JavaDiagnosticsContext context) {
			super();
			this.methodsCache = new HashMap<>();
			this.diagnostics = diagnostics;
			this.context = context;
		}

		@Override
		public void visitMethod(PsiMethod node) {
			validateMethod(node, diagnostics, context);
			super.visitMethod(node);
		}

		@Override
		public void visitJavaFile(PsiJavaFile file) {
			for(PsiClass clazz : file.getClasses()) {
				visitClass(clazz);
			}
		}

		@Override
		public void visitClass(PsiClass aClass) {
			for(PsiMethod method : aClass.getMethods()) {
				visitMethod(method);
			}
		}

		/**
		 * Checks if the given method declaration has a fallback annotation, and if so,
		 * provides diagnostics for the fallbackMethod
		 * 
		 * @param node        The method declaration to validate
		 * @param diagnostics A list where the diagnostics will be added
		 * @param context     The context, used to create the diagnostics
		 */
		private void validateMethod(PsiMethod node, List<Diagnostic> diagnostics,
				JavaDiagnosticsContext context) {
			@SuppressWarnings("rawtypes")
			PsiAnnotation[] annotations = node.getAnnotations();
			for (PsiAnnotation annotation : annotations) {
					if (isMatchAnnotation(annotation, FALLBACK_ANNOTATION)) {
						validateFallbackAnnotation(node, diagnostics, context, annotation);
					} else if (isMatchAnnotation(annotation, ASYNCHRONOUS_ANNOTATION)) {
						validateAsynchronousAnnotation(node, diagnostics, context, annotation);
					}
			}
		}

		/**
		 * Checks if the given method declaration has a fallback annotation, and if so,
		 * provides diagnostics for the fallbackMethod
		 *
		 * @param node        The method declaration to validate
		 * @param diagnostics A list where the diagnostics will be added
		 * @param context     The context, used to create the diagnostics
		 * @param annotation  The @Fallback annotation
		 */
		private void validateFallbackAnnotation(PsiMethod node, List<Diagnostic> diagnostics, JavaDiagnosticsContext context, PsiAnnotation annotation) {
			PsiAnnotationMemberValue fallbackMethodExpr = AnnotationUtils.getAnnotationMemberValueExpression(annotation,
					FALLBACK_METHOD_FALLBACK_ANNOTATION_MEMBER);
			if (fallbackMethodExpr != null) {
				String fallbackMethodName = AnnotationUtils.getAnnotationMemberValue(annotation, FALLBACK_METHOD_FALLBACK_ANNOTATION_MEMBER);
				//fallbackMethodName = fallbackMethodName.substring(1, fallbackMethodName.length() - 1);
				if (!getExistingMethods(node).contains(fallbackMethodName)) {
					PsiFile openable = context.getTypeRoot();
					Diagnostic d = context.createDiagnostic(context.getUri(),
							"The referenced fallback method '" + fallbackMethodName + "' does not exist",
							context.getUtils().toRange(openable, fallbackMethodExpr.getTextOffset(),
									fallbackMethodExpr.getTextLength()),
							DIAGNOSTIC_SOURCE, FALLBACK_METHOD_DOES_NOT_EXIST);
					d.setSeverity(DiagnosticSeverity.Error);
					diagnostics.add(d);
				}
			}
		}

		/**
		 * Checks if the given method declaration has an asynchronous annotation, and if so,
		 * provides diagnostics for the method return type
		 *
		 * @param node        The method declaration to validate
		 * @param diagnostics A list where the diagnostics will be added
		 * @param context     The context, used to create the diagnostics
		 * @param annotation  The @Asynchronous annotation
		 */
		private void validateAsynchronousAnnotation(PsiMethod node, List<Diagnostic> diagnostics,
													JavaDiagnosticsContext context, PsiAnnotation annotation) {
			PsiType methodReturnType = node.getReturnType();
			String methodReturnTypeString;
			try {
				methodReturnTypeString = methodReturnType.getCanonicalText();
			} catch (Exception e) {
				throw e;
			}
			if ((!(methodReturnTypeString.startsWith(FUTURE_TYPE_UTILITY)) && !(methodReturnTypeString.startsWith(COMPLETION_STAGE_TYPE_UTILITY)))) {
				PsiFile openable = context.getTypeRoot();
				Diagnostic d = context.createDiagnostic(context.getUri(),
						"The annotated method does not return an object of type Future or CompletionStage",
						context.getUtils().toRange(openable, node.getReturnTypeElement().getTextOffset(), node.getReturnTypeElement().getTextLength()),
						DIAGNOSTIC_SOURCE, FAULT_TOLERANCE_DEFINITION_EXCEPTION);
				d.setSeverity(DiagnosticSeverity.Error);
				diagnostics.add(d);
			}
		}


		private Set<String> getExistingMethods(PsiMethod node) {
			PsiClass type = getOwnerType(node);
			if (type == null) {
				return Collections.emptySet();
			}
			return getExistingMethods(type);
		}

		private PsiClass getOwnerType(PsiElement node) {
			return PsiTreeUtil.getParentOfType(node, PsiClass.class);
		}

		private Set<String> getExistingMethods(PsiClass type) {
			Set<String> methods = methodsCache.get(type);
			if (methods == null) {
				methods = Stream.of(type.getMethods()) //
						.map(m -> {
							return m.getName();
						}).collect(Collectors.toSet());
				methodsCache.put(type, methods);
			}
			return methods;
		};
	}

}