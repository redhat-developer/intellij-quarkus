/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.microprofile.psi.quarkus.renarde;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.microprofile.psi.quarkus.QuarkusMavenModuleImportingTestCase;
import com.redhat.microprofile.psi.quarkus.QuarkusMavenProjectName;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeLensParams;
import org.junit.Test;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.*;

/**
 * Tests for the CodeLens features introduced by
 * {@link com.redhat.microprofile.psi.internal.quarkus.renarde.java.RenardeJaxRsInfoProvider}.
 */
public class RenardeJaxRsTest extends QuarkusMavenModuleImportingTestCase {

	@Test
	public void testCodeLens() throws Exception {
		Module javaProject = loadMavenProject(QuarkusMavenProjectName.quarkus_renarde_todo);
		assertNotNull(javaProject);

		MicroProfileJavaCodeLensParams params = new MicroProfileJavaCodeLensParams();
		params.setCheckServerAvailable(false);
		String javaFileUri = getFileUri("src/main/java/rest/Application.java", javaProject);
		params.setUri(javaFileUri);
		params.setUrlCodeLensEnabled(true);

		assertCodeLens(params, PsiUtilsLSImpl.getInstance(myProject), //
				cl("http://localhost:8080/", "", r(20, 4, 4)), //
				cl("http://localhost:8080/about", "", r(25, 4, 4)), //
				cl("http://localhost:8080/Application/test", "", r(30, 4, 4)), //
				cl("http://localhost:8080/Application/endpoint", "", r(34, 4, 4)));
	}

	@Test
	public void testAbsolutePathCodeLens() throws Exception {
		Module javaProject = loadMavenProject(QuarkusMavenProjectName.quarkus_renarde_todo);

		assertNotNull(javaProject);

		MicroProfileJavaCodeLensParams params = new MicroProfileJavaCodeLensParams();
		params.setCheckServerAvailable(false);
		String javaFileUri = getFileUri("src/main/java/rest/Game.java", javaProject);
		params.setUri(javaFileUri);
		params.setUrlCodeLensEnabled(true);

		assertCodeLens(params, PsiUtilsLSImpl.getInstance(myProject), //
				cl("http://localhost:8080/play/id", "", r(9, 4, 4)),
				cl("http://localhost:8080/play/start", "", r(13, 4, 4)));
	}

	/*@Test
	public void workspaceSymbols() throws Exception {
		IJavaProject javaProject = loadMavenProject(quarkus_renarde_todo);

		assertNotNull(javaProject);

		assertWorkspaceSymbols(javaProject, JDT_UTILS, //
				si("@/: GET", r(20, 28, 33)), //
				si("@/Application/endpoint: GET", r(34, 18, 26)), //
				si("@/Application/test: POST", r(30, 16, 20)), //
				si("@/Login/complete: POST", r(174, 20, 28)), //
				si("@/Login/confirm: GET", r(138, 28, 35)), //
				si("@/Login/login: GET", r(57, 28, 33)), //
				si("@/Login/logoutFirst: GET", r(153, 28, 39)), //
				si("@/Login/manualLogin: POST", r(73, 20, 31)), //
				si("@/Login/register: POST", r(118, 28, 36)), //
				si("@/Login/welcome: GET", r(65, 28, 35)), //
				si("@/Todos/add: POST", r(59, 16, 19)), //
				si("@/Todos/delete: POST", r(35, 16, 22)), //
				si("@/Todos/done: POST", r(46, 16, 20)), //
				si("@/Todos/index: GET", r(29, 28, 33)), //
				si("@/about: GET", r(25, 28, 33)), //
				si("@/play/id: GET", r(9, 18, 26)), //
				si("@/play/start: GET", r(13, 18, 23)));
	}*/

}
