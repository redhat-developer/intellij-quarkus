/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.restclient.java;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Query;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.IJavaDiagnosticsParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PositionUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.restclient.MicroProfileRestClientConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.restclient.MicroProfileRestClientErrorCode;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.INJECT_JAKARTA_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.INJECT_JAVAX_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.restclient.MicroProfileRestClientConstants.REGISTER_REST_CLIENT_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.restclient.MicroProfileRestClientConstants.REST_CLIENT_ANNOTATION;

/**
 *
 * MicroProfile RestClient Diagnostics:
 *
 * <ul>
 * <li>Diagnostic 1: Field on current type has Inject and RestClient annotations
 * but corresponding interface does not have RegisterRestClient annotation</li>
 * <li>Diagnostic 2: Current type is an interface, does not have
 * RegisterRestClient annotation but corresponding fields have Inject and
 * RestClient annotation</li>
 * <li>Diagnostic 3: Field on current type has Inject and not RestClient
 * annotations but corresponding interface has RegisterRestClient annotation
 * </li>
 * <li>Diagnostic 4: Field on current type has RestClient and not Inject
 * annotations but corresponding interface has RegisterRestClient annotation
 * </li>
 * <li>Diagnostic 5: Field on current type has not RestClient and not Inject
 * annotations but corresponding interface has RegisterRestClient
 * annotation</li>
 * </ul>
 *
 * <p>
 * Those rules comes from
 * https://github.com/MicroShed/microprofile-language-server/blob/8f3401852d2b82310f49cd41ec043f5b541944a9/src/main/java/com/microprofile/lsp/internal/diagnostic/MicroProfileDiagnostic.java#L75
 * </p>
 *
 * @author Angelo ZERR
 *
 * @see <a href="https://github.com/eclipse/microprofile-rest-client">microprofile-rest-client</a>
 * @see <a ref="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/restclient/java/MicroProfileRestClientDiagnosticsParticipant.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/restclient/java/MicroProfileRestClientDiagnosticsParticipant.java</a>
 *
 */
public class MicroProfileRestClientDiagnosticsParticipant implements IJavaDiagnosticsParticipant {

	@Override
	public boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context) {
		// Collection of diagnostics for MicroProfile RestClient is done only if
		// microprofile-rest-client is on the classpath
		Module javaProject = context.getJavaProject();
		return PsiTypeUtils.findType(javaProject, REST_CLIENT_ANNOTATION) != null;
	}

	@Override
	public void collectDiagnostics(JavaDiagnosticsContext context) {
		PsiFile typeRoot = context.getTypeRoot();
		PsiElement[] elements = typeRoot.getChildren();
		collectDiagnostics(elements, context);
	}

	private static void collectDiagnostics(PsiElement[] elements,
			JavaDiagnosticsContext context) {
		for (PsiElement element : elements) {
			if (element instanceof PsiClass) {
				PsiClass type = (PsiClass) element;
				if (type.isInterface()) {
					validateInterfaceType(type, context);
				} else {
					validateClassType(type, context);
				}
				continue;
			}
		}
	}

	private static void validateClassType(PsiClass classType, JavaDiagnosticsContext context) {
		for (PsiElement element : classType.getChildren()) {
			if (element instanceof PsiField) {
				PsiField field = (PsiField) element;
				validateField(field, context);
			}
		}
	}

	private static void validateField(PsiField field, JavaDiagnosticsContext context) {
		DocumentFormat documentFormat = context.getDocumentFormat();
		boolean hasInjectAnnotation = AnnotationUtils.hasAnyAnnotation(field, INJECT_JAVAX_ANNOTATION, INJECT_JAKARTA_ANNOTATION);
		boolean hasRestClientAnnotation = AnnotationUtils.hasAnnotation(field, REST_CLIENT_ANNOTATION);
		String fieldTypeName = PsiTypeUtils.getResolvedTypeName(field);
		PsiClass fieldType = PsiTypeUtils.findType(field.getManager(), fieldTypeName);
		if (fieldType == null) {
			// document is in invalid state? better bail now.
			// See https://github.com/redhat-developer/intellij-quarkus/issues/823
			return;
		}
		boolean hasRegisterRestClient = AnnotationUtils.hasAnnotation(fieldType, REGISTER_REST_CLIENT_ANNOTATION)
				&& fieldType.isInterface();

		if (!hasRegisterRestClient) {
			if (hasInjectAnnotation && hasRestClientAnnotation) {
				// Diagnostic 1: Field on current type has Inject and RestClient annotations but
				// corresponding interface does not have RegisterRestClient annotation
				Range restClientRange = PositionUtils.toNameRange(field, context.getUtils());
				context.addDiagnostic(createDiagnostic1Message(field, fieldTypeName, documentFormat), restClientRange,
						MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE, null);
			}
		} else {
			if (hasInjectAnnotation && !hasRestClientAnnotation) {
				// Diagnostic 3: Field on current type has Inject and not RestClient annotations
				// but corresponding interface has RegisterRestClient annotation
				Range restClientRange = PositionUtils.toNameRange(field, context.getUtils());
				context.addDiagnostic("The Rest Client object should have the @RestClient annotation to be injected as a CDI bean.",
						restClientRange, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
						MicroProfileRestClientErrorCode.RestClientAnnotationMissing);
			} else if (!hasInjectAnnotation && hasRestClientAnnotation) {
				// Diagnostic 4: Field on current type has RestClient and not Inject
				// annotations but corresponding interface has RegisterRestClient annotation
				Range restClientRange = PositionUtils.toNameRange(field, context.getUtils());
				context.addDiagnostic("The Rest Client object should have the @Inject annotation to be injected as a CDI bean.",
						restClientRange, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
						MicroProfileRestClientErrorCode.InjectAnnotationMissing);
			} else if (!hasInjectAnnotation && !hasRestClientAnnotation) {
				// Diagnostic 5: Field on current type has not RestClient and not Inject
				// annotations
				// but corresponding interface has RegisterRestClient annotation
				Range restClientRange = PositionUtils.toNameRange(field, context.getUtils());
				context.addDiagnostic("The Rest Client object should have the @Inject and @RestClient annotations to be injected as a CDI bean.",
						restClientRange, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
						MicroProfileRestClientErrorCode.InjectAndRestClientAnnotationMissing);
			}
		}
	}

	private static String createDiagnostic1Message(PsiField field, String fieldTypeName, DocumentFormat documentFormat) {
		StringBuilder message = new StringBuilder("The corresponding ");
		if (DocumentFormat.Markdown.equals(documentFormat)) {
			message.append("`");
		}
		message.append(fieldTypeName);
		if (DocumentFormat.Markdown.equals(documentFormat)) {
			message.append("`");
		}
		message.append(" interface does not have the @RegisterRestClient annotation. The field ");
		if (DocumentFormat.Markdown.equals(documentFormat)) {
			message.append("`");
		}
		message.append(field.getName());
		if (DocumentFormat.Markdown.equals(documentFormat)) {
			message.append("`");
		}
		message.append(" will not be injected as a CDI bean.");
		return message.toString();
	}

	private static void validateInterfaceType(PsiClass interfaceType, JavaDiagnosticsContext context) {
		boolean hasRegisterRestClient = AnnotationUtils.hasAnnotation(interfaceType, REGISTER_REST_CLIENT_ANNOTATION);
		if (hasRegisterRestClient) {
			return;
		}

		final AtomicInteger nbReferences = new AtomicInteger(0);
		Query<PsiReference> query = ReferencesSearch.search(interfaceType, createSearchScope(context.getJavaProject()));
		query.forEach(match -> {
			PsiField field = PsiTreeUtil.getParentOfType(match.getElement(), PsiField.class);
			if (field != null) {
				boolean hasInjectAnnotation = AnnotationUtils.hasAnyAnnotation(field, INJECT_JAVAX_ANNOTATION, INJECT_JAKARTA_ANNOTATION);
				boolean hasRestClientAnnotation = AnnotationUtils.hasAnnotation(field, REST_CLIENT_ANNOTATION);
				if (hasInjectAnnotation && hasRestClientAnnotation) {
					nbReferences.incrementAndGet();
				}
			}
		});

		if (nbReferences.get() > 0) {
			Range restInterfaceRange = PositionUtils.toNameRange(interfaceType, context.getUtils());
			context.addDiagnostic("The interface `" + interfaceType.getName()
							+ "` does not have the @RegisterRestClient annotation. The " + nbReferences.get()
							+ " fields references will not be injected as CDI beans.",
					restInterfaceRange, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
					MicroProfileRestClientErrorCode.RegisterRestClientAnnotationMissing);
		}
	}

	private static GlobalSearchScope createSearchScope(Module javaProject) {
		return javaProject.getModuleContentScope();
	}
}
