/*******************************************************************************
* Copyright (c) 2020 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.metrics.java;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.IJavaDiagnosticsParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PositionUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4mp.commons.DocumentFormat;

import java.util.ArrayList;
import java.util.List;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.metrics.MicroProfileMetricsConstants.DEPENDENT_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.metrics.MicroProfileMetricsConstants.DIAGNOSTIC_SOURCE;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.metrics.MicroProfileMetricsConstants.GAUGE_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.metrics.MicroProfileMetricsConstants.METRIC_ID;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.metrics.MicroProfileMetricsConstants.REQUEST_SCOPED_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.metrics.MicroProfileMetricsConstants.SESSION_SCOPED_ANNOTATION;

/**
 * 
 * MicroProfile Metrics Diagnostics
 * <ul>
 * <li>Diagnostic 1: display @Gauge annotation diagnostic message if the
 * underlying bean is annotated with @RequestScoped, @SessionScoped
 * or @Dependent. Suggest that @AnnotationScoped is used instead.</li>
 * </ul>
 * 
 * 
 * @author Kathryn Kodama
 * 
 * @See https://github.com/eclipse/microprofile-metrics
 */
public class MicroProfileMetricsDiagnosticsParticipant implements IJavaDiagnosticsParticipant {

	@Override
	public boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context) {
		// Collection of diagnostics for MicroProfile Metrics is done only if
		// microprofile-metrics is on the classpath
		Module javaProject = context.getJavaProject();
		return PsiTypeUtils.findType(javaProject, METRIC_ID) != null;
	}

	@Override
	public List<Diagnostic> collectDiagnostics(JavaDiagnosticsContext context) {
		PsiFile typeRoot = context.getTypeRoot();
		PsiElement[] elements = typeRoot.getChildren();
		List<Diagnostic> diagnostics = new ArrayList<>();
		collectDiagnostics(elements, diagnostics, context);
		return diagnostics;
	}

	private static void collectDiagnostics(PsiElement[] elements, List<Diagnostic> diagnostics,
			JavaDiagnosticsContext context) {
		for (PsiElement element : elements) {
			if (element instanceof PsiClass) {
				PsiClass type = (PsiClass) element;
				if (!type.isInterface()) {
					validateClassType(type, diagnostics, context);
				}
				continue;
			}
		}
	}

	private static void validateClassType(PsiClass classType, List<Diagnostic> diagnostics, JavaDiagnosticsContext context) {
		String uri = context.getUri();
		IPsiUtils utils = context.getUtils();
		DocumentFormat documentFormat = context.getDocumentFormat();
		boolean hasInvalidScopeAnnotation = AnnotationUtils.hasAnnotation(classType, REQUEST_SCOPED_ANNOTATION)
				|| AnnotationUtils.hasAnnotation(classType, SESSION_SCOPED_ANNOTATION)
				|| AnnotationUtils.hasAnnotation(classType, DEPENDENT_ANNOTATION);
		// check for Gauge annotation for Diagnostic 1 only if the class has an invalid
		// scope annotation
		if (hasInvalidScopeAnnotation) {
			for (PsiElement element : classType.getChildren()) {
				if (element instanceof PsiMethod) {
					PsiMethod method = (PsiMethod) element;
					validateMethod(classType, method, diagnostics, context);
				}
			}
		}
	}

	private static void validateMethod(PsiClass classType, PsiMethod method, List<Diagnostic> diagnostics,
									   JavaDiagnosticsContext context) {
		String uri = context.getUri();
		DocumentFormat documentFormat = context.getDocumentFormat();
		boolean hasGaugeAnnotation = AnnotationUtils.hasAnnotation(method, GAUGE_ANNOTATION);

		// Diagnostic 1: display @Gauge annotation diagnostic message if
		// the underlying bean is annotated with @RequestScoped, @SessionScoped or
		// @Dependent.
		// Suggest that @AnnotationScoped is used instead.</li>
		if (hasGaugeAnnotation) {
			Range cdiBeanRange = PositionUtils.toNameRange(classType, context.getUtils());
			Diagnostic d = context.createDiagnostic(uri, createDiagnostic1Message(classType, documentFormat),
					cdiBeanRange, DIAGNOSTIC_SOURCE, MicroProfileMetricsErrorCode.ApplicationScopedAnnotationMissing);
			diagnostics.add(d);
		}
	}

	private static String createDiagnostic1Message(PsiClass classType, DocumentFormat documentFormat) {
		StringBuilder message = new StringBuilder("The class ");
		if (DocumentFormat.Markdown.equals(documentFormat)) {
			message.append("`");
		}
		message.append(classType.getQualifiedName());
		if (DocumentFormat.Markdown.equals(documentFormat)) {
			message.append("`");
		}
		message.append(
				" using the @Gauge annotation should use the @ApplicationScoped annotation. The @Gauge annotation does not"
						+ " support multiple instances of the underlying bean to be created.");
		return message.toString();
	}

}