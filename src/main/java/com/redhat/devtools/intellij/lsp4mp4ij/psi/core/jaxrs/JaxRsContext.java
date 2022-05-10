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

import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codelens.JavaCodeLensContext;

/**
 * JAX-RS context.
 *
 * @author Angelo ZERR
 *
 */
public class JaxRsContext {

	public static final int DEFAULT_PORT = 8080;

	private static final String CONTEXT_KEY = JaxRsContext.class.getName();

	private int serverPort;

	// The quarkus.http.root-path property in application.properties
	private String rootPath;

	// The value of the @ApplicationPath annotation
	private String applicationPath;

	public JaxRsContext() {
		setServerPort(DEFAULT_PORT);
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	/**
	 * Get the quarkus.http.root-path property
	 *
	 * @return the rootPath
	 */
	public String getRootPath() {
		return rootPath;
	}

	/**
	 * Set the quarkus.http.root-path property
	 *
	 * @param rootPath the rootPath to set
	 */
	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

	/**
	 * Get the @ApplicationPath annotation value
	 *
	 * @return the @ApplicationPath annotation value
	 */
	public String getApplicationPath() {
		return applicationPath;
	}

	/**
	 * Set the quarkus.http.root-path property
	 *
	 * @param applicationPath as the @ApplicationPath annotation value
	 */
	public void setApplicationPath(String applicationPath) {
		this.applicationPath = applicationPath;
	}

	public static JaxRsContext getJaxRsContext(JavaCodeLensContext context) {
		JaxRsContext jaxRsContext = (JaxRsContext) context.get(CONTEXT_KEY);
		if (jaxRsContext == null) {
			jaxRsContext = new JaxRsContext();
			context.put(CONTEXT_KEY, jaxRsContext);
		}
		return jaxRsContext;
	}

	/**
	 * Create the local base URL
	 *
	 * @return the String representation of the project base URL
	 */
	public String getLocalBaseURL() {
		StringBuilder localBaseURL = new StringBuilder("http://localhost:");
		localBaseURL.append(getServerPort());
		if (rootPath != null) {
			localBaseURL.append(getRootPath());
		}
		if (applicationPath != null) {
			if (!applicationPath.startsWith("/")) {
				localBaseURL.append('/');
			}
			localBaseURL.append(applicationPath);
		}
		return localBaseURL.toString();
	}
}
