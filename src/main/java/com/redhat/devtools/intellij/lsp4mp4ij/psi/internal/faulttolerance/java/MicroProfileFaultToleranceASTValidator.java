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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.java;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.validators.JavaASTValidator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.UNI_TYPE_UTILITY;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotationMemberValueExpression;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.isMatchAnnotation;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.MicroProfileFaultToleranceConstants.ASYNCHRONOUS_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.MicroProfileFaultToleranceConstants.FALLBACK_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.MicroProfileFaultToleranceConstants.FALLBACK_METHOD_FALLBACK_ANNOTATION_MEMBER;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.java.MicroProfileFaultToleranceErrorCode.FALLBACK_METHOD_DOES_NOT_EXIST;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.java.MicroProfileFaultToleranceErrorCode.FAULT_TOLERANCE_DEFINITION_EXCEPTION;

/**
 * Collects diagnostics related to the <code>@Fallback</code> and
 * <code>@Asynchronous</code> annotations.
 */
public class MicroProfileFaultToleranceASTValidator extends JavaASTValidator {

	private static final String FALLBACK_ERROR_MESSAGE = "The referenced fallback method ''{0}'' does not exist.";

	private static final String ASYNCHRONOUS_ERROR_MESSAGE = "The annotated method ''{0}'' with @Asynchronous should return an object of type {1}.";

	private final Map<PsiClass, Set<String>> methodsCache;

	private final List<String> allowedReturnTypesForAsynchronousAnnotation;

	private static Logger LOGGER = Logger.getLogger(MicroProfileFaultToleranceASTValidator.class.getName());

	public MicroProfileFaultToleranceASTValidator() {
		super();
		this.methodsCache = new HashMap<>();
		this.allowedReturnTypesForAsynchronousAnnotation = new ArrayList<>(
				Arrays.asList(FUTURE_TYPE_UTILITY, COMPLETION_STAGE_TYPE_UTILITY));
	}

	@Override
	public boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context) {
		Module javaProject = context.getJavaProject();
		boolean adapted = PsiTypeUtils.findType(javaProject, FALLBACK_ANNOTATION) != null
				|| PsiTypeUtils.findType(javaProject, ASYNCHRONOUS_ANNOTATION) != null;
		if (adapted) {
			addAllowedReturnTypeForAsynchronousAnnotation(javaProject, UNI_TYPE_UTILITY);
		}
		return adapted;
	}

	private void addAllowedReturnTypeForAsynchronousAnnotation(Module javaProject, String returnType) {
		if (PsiTypeUtils.findType(javaProject, returnType) != null) {
			allowedReturnTypesForAsynchronousAnnotation.add(returnType);
		}
	}

	@Override
	public void visitMethod(PsiMethod node) {
			validateMethod(node);
	}

	@Override
	public void visitClass(PsiClass type) {
		for (PsiAnnotation annotation : type.getAnnotations()) {
			if (isMatchAnnotation(annotation, ASYNCHRONOUS_ANNOTATION)) {
				PsiMethod[] methods = type.getMethods();
				for (PsiMethod node : methods) {
					validateAsynchronousAnnotation(node, annotation);
				}
				break;
			}
		}
		type.acceptChildren(this);
	}

	/**
	 * Checks if the given method declaration has a supported annotation, and if so,
	 * provides diagnostics if necessary
	 *
	 * @param node The method declaration to validate
	 */
	private void validateMethod(PsiMethod node) {
		for (PsiAnnotation annotation : node.getAnnotations()) {
			if (isMatchAnnotation(annotation, FALLBACK_ANNOTATION)) {
				validateFallbackAnnotation(node, annotation);
			} else if (isMatchAnnotation(annotation, ASYNCHRONOUS_ANNOTATION)) {
				validateAsynchronousAnnotation(node, annotation);
			}
		}
	}

	/**
	 * Checks if the given method declaration has a fallback annotation, and if so,
	 * provides diagnostics for the fallbackMethod
	 *
	 * @param node       The method declaration to validate
	 * @param annotation The @Fallback annotation
	 */
	private void validateFallbackAnnotation(PsiMethod node, PsiAnnotation annotation) {
		PsiAnnotationMemberValue fallbackMethodExpr = getAnnotationMemberValueExpression(annotation,
				FALLBACK_METHOD_FALLBACK_ANNOTATION_MEMBER);
		if (fallbackMethodExpr != null) {
			String fallbackMethodName = AnnotationUtils.getAnnotationMemberValue(annotation, FALLBACK_METHOD_FALLBACK_ANNOTATION_MEMBER);
			//fallbackMethodName = fallbackMethodName.substring(1, fallbackMethodName.length() - 1);
			if (!getExistingMethods(node).contains(fallbackMethodName)) {
				String message = MessageFormat.format(FALLBACK_ERROR_MESSAGE, fallbackMethodName);
				super.addDiagnostic(message, DIAGNOSTIC_SOURCE, fallbackMethodExpr, FALLBACK_METHOD_DOES_NOT_EXIST,
						DiagnosticSeverity.Error);
			}
		}
	}

	/**
	 * Checks if the given method declaration has an asynchronous annotation, and if
	 * so, provides diagnostics for the method return type
	 *
	 * @param node       The method declaration to validate
	 * @param annotation The @Asynchronous annotation
	 */
	private void validateAsynchronousAnnotation(PsiMethod node, PsiAnnotation annotation) {
		PsiType methodReturnType = node.getReturnType();
		String methodReturnTypeString;
		try {
			methodReturnTypeString = methodReturnType.getCanonicalText();
		} catch (Exception e) {
			throw e;
		}
		if ((!isAllowedReturnTypeForAsynchronousAnnotation(methodReturnTypeString))) {
			String allowedTypes = allowedReturnTypesForAsynchronousAnnotation.stream()
					.collect(Collectors.joining("', '", "'", "'"));
			String message = MessageFormat.format(ASYNCHRONOUS_ERROR_MESSAGE, node.getName(), allowedTypes);
			super.addDiagnostic(message, DIAGNOSTIC_SOURCE, node.getReturnTypeElement(), FAULT_TOLERANCE_DEFINITION_EXCEPTION,
					DiagnosticSeverity.Error);
		}
	}

	private boolean isAllowedReturnTypeForAsynchronousAnnotation(String returnType) {
		return allowedReturnTypesForAsynchronousAnnotation.stream().filter(s -> returnType.startsWith(s)).findFirst().isPresent();
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
