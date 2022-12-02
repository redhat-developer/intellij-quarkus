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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.java;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManagerForJava;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeLensParams;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.saveFile;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.fixURI;

/**
 * JAX-RS URL Codelens test for Java file with @ApplicationPath annotation.
 *
 */
public class JaxRsApplicationPathCodeLensTest extends MavenModuleImportingTestCase {

	@Test
	public void testUrlCodeLensApplicationPath() throws Exception {
		Module javaProject = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-applicationpath"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);


		MicroProfileJavaCodeLensParams params = new MicroProfileJavaCodeLensParams();
		params.setCheckServerAvailable(false);
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/ApplicationPathResource.java").toURI());
		params.setUri(javaFileUri);
		params.setUrlCodeLensEnabled(true);

		saveFile("org/acme/MyApplication.java", "package org.acme;\r\n" + //
				"import javax.ws.rs.ApplicationPath;\r\n" + //
				"import javax.ws.rs.core.Application;\r\n" + //
				"@ApplicationPath(\"/api\")\r\n" + //
				"public class MyApplication extends Application {}\r\n", javaProject, true);

		// Default port
		assertCodeLense(8080, params, utils, "/api/path");
	}

	@Test
	public void testUrlCodeLensApplicationPathNoSlash() throws Exception {
		Module javaProject = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-applicationpath"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaCodeLensParams params = new MicroProfileJavaCodeLensParams();
		params.setCheckServerAvailable(false);
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/ApplicationPathResource.java").toURI());
		params.setUri(javaFileUri);
		params.setUrlCodeLensEnabled(true);

		saveFile("org/acme/MyApplication.java", "package org.acme;\r\n" + //
				"import javax.ws.rs.ApplicationPath;\r\n" + //
				"import javax.ws.rs.core.Application;\r\n" + //
				"@ApplicationPath(\"api\")\r\n" + //
				"public class MyApplication extends Application {}\r\n", javaProject, true);

		// Default port
		assertCodeLense(8080, params, utils, "/api/path");
	}

	@Test
	public void testUrlCodeLensApplicationPathChange() throws Exception {
		Module javaProject = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-applicationpath"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaCodeLensParams params = new MicroProfileJavaCodeLensParams();
		params.setCheckServerAvailable(false);
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/ApplicationPathResource.java").toURI());
		params.setUri(javaFileUri);
		params.setUrlCodeLensEnabled(true);

		saveFile("org/acme/MyApplication.java", "package org.acme;\r\n" + //
				"import javax.ws.rs.ApplicationPath;\r\n" + //
				"import javax.ws.rs.core.Application;\r\n" + //
				"@ApplicationPath(\"/api\")\r\n" + //
				"public class MyApplication extends Application {}\r\n", javaProject, true);

		// Default port
		assertCodeLense(8080, params, utils, "/api/path");

		saveFile("org/acme/MyApplication.java", "package org.acme;\r\n" + //
				"import javax.ws.rs.ApplicationPath;\r\n" + //
				"import javax.ws.rs.core.Application;\r\n" + //
				"@ApplicationPath(\"/ipa\")\r\n" + //
				"public class MyApplication extends Application {}\r\n", javaProject, true);

		assertCodeLense(8080, params, utils, "/ipa/path");
	}

	private static void assertCodeLense(int port, MicroProfileJavaCodeLensParams params, IPsiUtils utils,
										String actualEndpoint) {
		List<? extends CodeLens> lenses = PropertiesManagerForJava.getInstance().codeLens(params, utils);
		Assert.assertEquals(1, lenses.size());

		CodeLens lenseForEndpoint = lenses.get(0);
		Assert.assertNotNull(lenseForEndpoint.getCommand());
		Assert.assertEquals("http://localhost:" + port + actualEndpoint, lenseForEndpoint.getCommand().getTitle());
	}
}
