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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs;

import com.intellij.psi.PsiMethod;
/**
 * Represents a JAX-RS method.
 */
public class JaxRsMethodInfo {

	private final String url;
	private final HttpMethod httpMethod;
	private final PsiMethod javaMethod;
	private final String documentUri;

	public JaxRsMethodInfo(String url, HttpMethod httpMethod, PsiMethod javaMethod, String documentUri) {
		this.url = url;
		this.javaMethod = javaMethod;
		this.httpMethod = httpMethod;
		this.documentUri = documentUri;
	}

	/**
	 * Returns the URL of the JAX-RS method.
	 *
	 * @return the URL of the JAX-RS method
	 */
	public String getUrl() {
		return this.url;
	}

	/**
	 * Returns the HTTP method of the JAX-RS method.
	 *
	 * @return the HTTP method of the JAX-RS method
	 */
	public HttpMethod getHttpMethod() {
		return this.httpMethod;
	}

	/**
	 * Returns the Java method associated with this JAX-RS method.
	 *
	 * @return the Java method associated with this JAX-RS method
	 */
	public PsiMethod getJavaMethod() {
		return this.javaMethod;
	}

	/**
	 * Returns the URI of the Java file where this JAX-RS method is defined.
	 *
	 * @return the URI of the Java file where this JAX-RS method is defined
	 */
	public String getDocumentUri() {
		return this.documentUri;
	}

}