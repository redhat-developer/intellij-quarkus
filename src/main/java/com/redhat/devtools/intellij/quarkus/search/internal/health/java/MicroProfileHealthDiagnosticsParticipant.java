/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search.internal.health.java;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.quarkus.search.core.utils.AnnotationUtils;
import com.redhat.devtools.intellij.quarkus.search.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.quarkus.search.core.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.quarkus.search.core.java.diagnostics.IJavaDiagnosticsParticipant;
import com.redhat.devtools.intellij.quarkus.search.core.java.diagnostics.JavaDiagnosticsContext;
import com.redhat.devtools.intellij.quarkus.search.core.utils.PositionUtils;
import com.redhat.devtools.intellij.quarkus.search.internal.health.MicroProfileHealthConstants;
import com.redhat.microprofile.commons.DocumentFormat;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.redhat.devtools.intellij.quarkus.search.internal.health.MicroProfileHealthConstants.HEALTH_ANNOTATION;
import static com.redhat.devtools.intellij.quarkus.search.internal.health.MicroProfileHealthConstants.HEALTH_CHECK_INTERFACE_NAME;
import static com.redhat.devtools.intellij.quarkus.search.internal.health.MicroProfileHealthConstants.LIVENESS_ANNOTATION;
import static com.redhat.devtools.intellij.quarkus.search.internal.health.MicroProfileHealthConstants.READINESS_ANNOTATION;

/**
 *
 * MicroProfile Health Diagnostics:
 * 
 * <ul>
 * <li>Diagnostic 1:display Health annotation diagnostic message if
 * Health/Liveness/Readiness annotation exists but HealthCheck interface is not
 * implemented</li>
 * <li>Diagnostic 2: display HealthCheck diagnostic message if HealthCheck
 * interface is implemented but Health/Liveness/Readiness annotation does not
 * exist</li>
 * 
 * </ul>
 * 
 * <p>
 * Those rules comes from
 * https://github.com/MicroShed/microprofile-language-server/blob/8f3401852d2b82310f49cd41ec043f5b541944a9/src/main/java/com/microprofile/lsp/internal/diagnostic/MicroProfileDiagnostic.java#L250
 * </p>
 * 
 * @author Angelo ZERR
 * 
 * @see <a href="https://github.com/eclipse/microprofile-health">https://github.com/eclipse/microprofile-health</a>
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/health/java/MicroProfileHealthDiagnosticsParticipant.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/health/java/MicroProfileHealthDiagnosticsParticipant.java</a>
 *
 */
public class MicroProfileHealthDiagnosticsParticipant implements IJavaDiagnosticsParticipant {

	@Override
	public boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context) {
		// Collection of diagnostics for MicroProfile Health is done only if
		// microprofile-health is on the classpath
		Module javaProject = context.getJavaProject();
		return PsiTypeUtils.findType(javaProject, HEALTH_ANNOTATION) != null;
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
		PsiClass[] interfaces = findImplementedInterfaces(classType);
		boolean implementsHealthCheck = Stream.of(interfaces)
				.anyMatch(interfaceType -> HEALTH_CHECK_INTERFACE_NAME.equals(interfaceType.getName()));
		boolean hasOneOfHealthAnnotation = AnnotationUtils.hasAnnotation(classType, LIVENESS_ANNOTATION)
				|| AnnotationUtils.hasAnnotation(classType, READINESS_ANNOTATION)
				|| AnnotationUtils.hasAnnotation(classType, HEALTH_ANNOTATION);
		// Diagnostic 1:display Health annotation diagnostic message if
		// Health/Liveness/Readiness annotation exists but HealthCheck interface is not
		// implemented
		if (hasOneOfHealthAnnotation && !implementsHealthCheck) {
			Range healthCheckInterfaceRange = PositionUtils.toNameRange(classType, utils);
			Diagnostic d = context.createDiagnostic(uri, createDiagnostic1Message(classType, documentFormat),
					healthCheckInterfaceRange, MicroProfileHealthConstants.DIAGNOSTIC_SOURCE,
					MicroProfileHealthErrorCode.ImplementHealthCheck);
			diagnostics.add(d);
		}

		// Diagnostic 2: display HealthCheck diagnostic message if HealthCheck interface
		// is implemented but Health/Liveness/Readiness annotation does not exist
		if (implementsHealthCheck && !hasOneOfHealthAnnotation) {
			Range healthCheckInterfaceRange = PositionUtils.toNameRange(classType, utils);
			Diagnostic d = context.createDiagnostic(uri, createDiagnostic2Message(classType, documentFormat),
					healthCheckInterfaceRange, MicroProfileHealthConstants.DIAGNOSTIC_SOURCE,
					MicroProfileHealthErrorCode.HealthAnnotationMissing);
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
				" using the @Liveness, @Readiness, or @Health annotation should implement the HealthCheck interface.");
		return message.toString();
	}

	private static String createDiagnostic2Message(PsiClass classType, DocumentFormat documentFormat) {
		StringBuilder message = new StringBuilder("The class ");
		if (DocumentFormat.Markdown.equals(documentFormat)) {
			message.append("`");
		}
		message.append(classType.getQualifiedName());
		if (DocumentFormat.Markdown.equals(documentFormat)) {
			message.append("`");
		}
		message.append(
				" implementing the HealthCheck interface should use the @Liveness, @Readiness, or @Health annotation.");
		return message.toString();
	}

	private static PsiClass[] findImplementedInterfaces(PsiClass type) {
		return type.getInterfaces();
	}
}
