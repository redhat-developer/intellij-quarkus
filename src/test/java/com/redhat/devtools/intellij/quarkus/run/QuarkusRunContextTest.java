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
package com.redhat.devtools.intellij.quarkus.run;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProject;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.quarkus.psi.internal.providers.QuarkusConfigSourceProvider;
import com.redhat.microprofile.psi.quarkus.QuarkusMavenModuleImportingTestCase;
import com.redhat.microprofile.psi.quarkus.QuarkusMavenProjectName;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.saveFile;

/**
 * QuarkusRunContext test for application.properties with quarkus.http.root-path,
 * quarkus.http.non-application-root-path and quarkus.http.port.
 *
 */
public class QuarkusRunContextTest extends QuarkusMavenModuleImportingTestCase {

	@Test
	public void testDevUIURL() throws Exception {
		Module javaProject = loadMavenProject(QuarkusMavenProjectName.microprofile_applicationpath);

		PsiMicroProfileProject mpProject = PsiMicroProfileProjectManager.getInstance(javaProject.getProject()).getMicroProfileProject(javaProject);

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, "", javaProject);
		assertEquals("http://localhost:8080/q/dev", QuarkusModuleUtil.getDevUIUrl(mpProject));

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, "quarkus.http.root-path=/root", javaProject);
		assertEquals("http://localhost:8080/root/q/dev", QuarkusModuleUtil.getDevUIUrl(mpProject));

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, "quarkus.http.non-application-root-path=/nonroot", javaProject);
		assertEquals("http://localhost:8080/nonroot/dev", QuarkusModuleUtil.getDevUIUrl(mpProject));

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, "quarkus.http.non-application-root-path=nonroot", javaProject);
		assertEquals("http://localhost:8080/nonroot/dev", QuarkusModuleUtil.getDevUIUrl(mpProject));

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, "quarkus.http.port=8081", javaProject);
		assertEquals("http://localhost:8081/q/dev", QuarkusModuleUtil.getDevUIUrl(mpProject));
	}

	@Test
	public void testApplicationURL() throws Exception {
		Module javaProject = loadMavenProject(QuarkusMavenProjectName.microprofile_applicationpath);

		PsiMicroProfileProject mpProject = PsiMicroProfileProjectManager.getInstance(javaProject.getProject()).getMicroProfileProject(javaProject);

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, "", javaProject);
		assertEquals("http://localhost:8080/", QuarkusModuleUtil.getApplicationUrl(mpProject));

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, "quarkus.http.root-path=/root", javaProject);
		assertEquals("http://localhost:8080/root/", QuarkusModuleUtil.getApplicationUrl(mpProject));

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, "quarkus.http.port=8081", javaProject);
		assertEquals("http://localhost:8081/", QuarkusModuleUtil.getApplicationUrl(mpProject));
	}
}
