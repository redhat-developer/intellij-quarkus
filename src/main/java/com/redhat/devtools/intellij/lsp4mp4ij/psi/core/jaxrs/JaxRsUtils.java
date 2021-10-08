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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Range;

import java.util.Collections;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotation;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotationMemberValue;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.hasAnnotation;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.jaxrs.JaxRsConstants.JAVAX_WS_RS_GET_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.jaxrs.JaxRsConstants.JAVAX_WS_RS_PATH_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.jaxrs.JaxRsConstants.PATH_VALUE;

/**
 * JAX-RS utilities.
 *
 * @author Angelo ZERR
 *
 */
public class JaxRsUtils {

	private JaxRsUtils() {

	}

	/**
	 * Returns the value of the JAX-RS Path annotation and null otherwise..
	 *
	 * @param annotatable
	 * @return the value of the JAX-RS Path annotation and null otherwise..
	 */
	public static String getJaxRsPathValue(PsiElement annotatable) {
		PsiAnnotation annotationPath = getAnnotation(annotatable, JAVAX_WS_RS_PATH_ANNOTATION);
		return annotationPath != null ? getAnnotationMemberValue(annotationPath, PATH_VALUE) : null;
	}

	/**
	 * Returns true if the given method has @GET annotation and false otherwise.
	 *
	 * @param method the method.
	 * @return true if the given method has @GET annotation and false otherwise.
	 */
	public static boolean isJaxRsRequestMethod(PsiMethod method) {
		return hasAnnotation(method, JAVAX_WS_RS_GET_ANNOTATION);
	}

	/**
	 * Create URL CodeLens.
	 *
	 * @param baseURL          the base URL.
	 * @param rootPath         the JAX-RS path value.
	 * @param openURICommandId the open URI command and null otherwise.
	 * @param method           the method.
	 * @param utils            the JDT utilities.
	 * @return the code lens and null otherwise.
	 */
	public static CodeLens createURLCodeLens(String baseURL, String rootPath, String openURICommandId, PsiMethod method,
			IPsiUtils utils) {
		CodeLens lens = createURLCodeLens(method, utils);
		if (lens != null) {
			String pathValue = getJaxRsPathValue(method);
			String url = buildURL(baseURL, rootPath, pathValue);
			lens.setCommand(
					new Command(url, openURICommandId != null ? openURICommandId : "", Collections.singletonList(url)));
		}
		return lens;
	}

	private static CodeLens createURLCodeLens(PsiMethod method, IPsiUtils utils) {
		TextRange r = method.getTextRange();
		if (r == null) {
			return null;
		}
		CodeLens lens = new CodeLens();
		final Range range = utils.toRange(method, r.getStartOffset(), r.getLength());
		lens.setRange(range);
		return lens;
	}

	public static String buildURL(String... paths) {
		StringBuilder url = new StringBuilder();
		for (String path : paths) {
			if (path != null && !path.isEmpty()) {
				if (url.length() > 0 && path.charAt(0) == '/') {
					path = path.substring(1, path.length());
				}

				if (url.length() > 0 && url.charAt(url.length() - 1) != '/') {
					url.append('/');
				}
				url.append(path);
			}
		}
		return url.toString();
	}
}
