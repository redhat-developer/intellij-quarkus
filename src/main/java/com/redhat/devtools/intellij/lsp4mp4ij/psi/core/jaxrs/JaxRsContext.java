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

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.util.Query;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codelens.JavaCodeLensContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsUtils.getJaxRsApplicationPathValue;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsConstants.JAKARTA_WS_RS_APPLICATIONPATH_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsConstants.JAVAX_WS_RS_APPLICATIONPATH_ANNOTATION;

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

	private final Module javaProject;

	private boolean applicationPathLoaded = false;

	public JaxRsContext(Module javaProject) {
		setServerPort(DEFAULT_PORT);
		this.javaProject = javaProject;
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
		if (applicationPathLoaded) {
			return applicationPath;
		}
		PsiClass applicationPathType = PsiTypeUtils.findType(javaProject,
				JAVAX_WS_RS_APPLICATIONPATH_ANNOTATION);
		if (applicationPathType == null) {
			applicationPathType = PsiTypeUtils.findType(javaProject,
					JAKARTA_WS_RS_APPLICATIONPATH_ANNOTATION);
		}
		if (applicationPathType != null) {
			applicationPath = findApplicationPath(applicationPathType, javaProject);
		}
		applicationPathLoaded = true;
		return applicationPath;
	}

	/**
	 * Set the @ApplicationPath annotation value
	 *
	 * @param applicationPath as the @ApplicationPath annotation value
	 */
	public void setApplicationPath(String applicationPath) {
		this.applicationPath = applicationPath;
		this.applicationPathLoaded = applicationPath != null;
	}

	public static JaxRsContext getJaxRsContext(JavaCodeLensContext context) {
		JaxRsContext jaxRsContext = (JaxRsContext) context.get(CONTEXT_KEY);
		if (jaxRsContext == null) {
			jaxRsContext = new JaxRsContext(context.getJavaProject());
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
		// application path is lazy loaded, but we need it now
		getApplicationPath();
		if (applicationPath != null) {
			if (!applicationPath.startsWith("/")) {
				localBaseURL.append('/');
			}
			localBaseURL.append(applicationPath);
		}
		return localBaseURL.toString();
	}

	/**
	 * Use the java search engine to search the java project for the location and
	 * value of the @ApplicationPath annotation, or null if not found
	 *
	 * @param annotationType the type representing the @ApplicationPath annotation
	 * @param javaProject        the java project the code lens applies to
	 * @return the value of the @ApplicationPath annotation, or null if not found
	 */
	private static String findApplicationPath(PsiClass annotationType, Module javaProject) {
		AtomicReference<String> applicationPathRef = new AtomicReference<String>();

		Query<PsiClass> pattern = AnnotatedElementsSearch.searchElements(annotationType, javaProject.getModuleWithDependenciesScope(),
				PsiClass.class);
        for (PsiClass match : pattern.findAll()) {
            collectApplicationPath(match, applicationPathRef);
        }
		return applicationPathRef.get();
	}

	private static void collectApplicationPath(PsiClass type, AtomicReference<String> applicationPathRef) {
		String applicationPathValue = getJaxRsApplicationPathValue(type);
		if (applicationPathValue != null) {
			applicationPathRef.set(applicationPathValue);
		}
	}
}
