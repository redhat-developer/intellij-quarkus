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
package com.redhat.microprofile.psi.quarkus.jaxrs;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileMavenProjectName;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManagerForJava;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.quarkus.psi.internal.providers.QuarkusConfigSourceProvider;
import com.redhat.microprofile.psi.quarkus.QuarkusMavenModuleImportingTestCase;
import com.redhat.microprofile.psi.quarkus.QuarkusMavenProjectName;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeLensParams;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.saveFile;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.*;

/**
 * JAX-RS URL Codelens test for Java file with quarkus.http.root-path
 * and @ApplicationPath annotation.
 *
 */
public class JaxRsApplicationPathCodeLensTest extends QuarkusMavenModuleImportingTestCase {

	@Test
	public void testUrlCodeLensProperties() throws Exception {
		Module javaProject = loadMavenProject(MicroProfileMavenProjectName.microprofile_applicationpath);
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaCodeLensParams params = new MicroProfileJavaCodeLensParams();
		params.setCheckServerAvailable(false);
		String javaFileUri = getFileUri("src/main/java/org/acme/ApplicationPathResource.java", javaProject);

		params.setUri(javaFileUri);
		params.setUrlCodeLensEnabled(true);

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, "quarkus.http.root-path=/root", javaProject);

		// Default port
		assertCodeLenses(8080, params, utils, "/root/api/path");

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, "", javaProject);

		assertCodeLenses(8080, params, utils, "/api/path");
	}

	private static void assertCodeLenses(int port, MicroProfileJavaCodeLensParams params, IPsiUtils utils,
										 String testPath) {
		assertCodeLens(params, utils, //
				cl("http://localhost:" + port + testPath, "", r(12, 4, 4)));
	}

}
