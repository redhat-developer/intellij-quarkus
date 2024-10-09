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
package com.redhat.microprofile.psi.internal.quarkus.jaxrs.java;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codelens.IJavaCodeLensParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codelens.JavaCodeLensContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProject;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import org.eclipse.lsp4j.CodeLens;

import java.util.List;

/**
 *
 * Quarkus JAX-RS CodeLens participant used to update the server port declared
 * with "quarkus.http.port" property.
 *
 * @author Angelo ZERR
 *
 */
public class QuarkusJaxRsCodeLensParticipant implements IJavaCodeLensParticipant {

	private static final String QUARKUS_DEV_HTTP_PORT = "%dev.quarkus.http.port";
	private static final String QUARKUS_HTTP_PORT = "quarkus.http.port";
	private static final String QUARKUS_DEV_HTTP_ROOT_PATH = "%dev.quarkus.http.root-path";
	private static final String QUARKUS_HTTP_ROOT_PATH = "quarkus.http.root-path";
	private static final String QUARKUS_REST_PATH = "quarkus.rest.path";
	private static final String QUARKUS_DEV_REST_PATH = "%dev.quarkus.rest.path";

	@Override
	public void beginCodeLens(JavaCodeLensContext context, ProgressIndicator monitor) {
		// Update the JAX-RS server port from the declared quarkus property
		// "quarkus.http.port"
		Module javaProject = context.getJavaProject();
		PsiMicroProfileProject mpProject = PsiMicroProfileProjectManager.getInstance(javaProject.getProject())
				.getMicroProfileProject(javaProject);
		int serverPort = mpProject.getPropertyAsInteger(QUARKUS_HTTP_PORT, JaxRsContext.DEFAULT_PORT);
		int devServerPort = mpProject.getPropertyAsInteger(QUARKUS_DEV_HTTP_PORT, serverPort);
		JaxRsContext.getJaxRsContext(context).setServerPort(devServerPort);

		// Retrieve HTTP root path from application.properties
		// quarkus.http.root-path
		String httpRootPath = mpProject.getProperty(QUARKUS_HTTP_ROOT_PATH);
		String devHttpRootPath = mpProject.getProperty(QUARKUS_DEV_HTTP_ROOT_PATH, httpRootPath);
		JaxRsContext.getJaxRsContext(context).setRootPath(devHttpRootPath);

		// quarkus.rest.path
		// see https://quarkus.io/guides/rest#declaring-endpoints-uri-mapping
		String restPath = mpProject.getProperty(QUARKUS_REST_PATH);
		String devRestPath = mpProject.getProperty(QUARKUS_DEV_REST_PATH, restPath);
		JaxRsContext.getJaxRsContext(context).setApplicationPath(devRestPath);
	}

	@Override
	public List<CodeLens> collectCodeLens(JavaCodeLensContext context, ProgressIndicator monitor) {
		// Do nothing
		return null;
	}
}
