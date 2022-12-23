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
package com.redhat.microprofile.psi.quarkus.config;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManagerForJava;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.quarkus.psi.internal.providers.QuarkusConfigSourceProvider;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeLensParams;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.saveFile;

/**
 * MicroProfile RestClient URL Codelens test for Java file.
 *
 * @author Angelo ZERR
 *
 */
public class JavaCodeLensQuarkusRestClientTest extends MavenModuleImportingTestCase {

	@Test
	public void testUrlCodeLensYaml() throws Exception {
		Module javaProject = createMavenModule(new File("projects/quarkus/projects/maven/rest-client-quickstart"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		// Initialize file
		initConfigFile(javaProject);

		MicroProfileJavaCodeLensParams params = new MicroProfileJavaCodeLensParams();
		String javaFileUri = VfsUtilCore.virtualToIoFile(LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(javaProject) + "/src/main/java/org/acme/restclient/CountriesService.java")).toURI().toString();
		params.setUri(javaFileUri);
		params.setUrlCodeLensEnabled(true);

		// No configuration of base url
		List<? extends CodeLens> lenses = PropertiesManagerForJava.getInstance().codeLens(params, utils);
		Assert.assertEquals(0, lenses.size());

		// /mp-rest/url
		saveFile(QuarkusConfigSourceProvider.APPLICATION_YAML_FILE, "org:\r\n" + //
				"  acme:\r\n" + //
				"    restclient:\r\n" + //
				"      CountriesService/mp-rest/url: https://restcountries.url/rest", javaProject);
		assertCodeLenses("https://restcountries.url/rest", params, utils);

		// /mp-rest/uri
		saveFile(QuarkusConfigSourceProvider.APPLICATION_YAML_FILE, //
				"org:\r\n" + //
						"  acme:\r\n" + //
						"    restclient:\r\n" + //
						"      CountriesService/mp-rest/url: https://restcountries.url/rest\r\n" + //
						"      CountriesService/mp-rest/uri: https://restcountries.uri/rest\r\n" + //
						"", //
				javaProject);
		assertCodeLenses("https://restcountries.uri/rest", params, utils);
	}

	private static void initConfigFile(Module javaProject) throws Exception {
		saveFile(QuarkusConfigSourceProvider.APPLICATION_YAML_FILE, "", javaProject);
		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, "", javaProject);
	}

	private static void assertCodeLenses(String baseURL, MicroProfileJavaCodeLensParams params, IPsiUtils utils) {
		List<? extends CodeLens> lenses = PropertiesManagerForJava.getInstance().codeLens(params, utils);
		Assert.assertEquals(2, lenses.size());

		// @GET
		// @Path("/name/{name}")
		// Set<Country> getByName(@PathParam String name);
		CodeLens lensForGet = lenses.get(0);
		Assert.assertNotNull(lensForGet.getCommand());
		Assert.assertEquals(baseURL + "/v2/name/{name}", lensForGet.getCommand().getTitle());

		// @GET
		// @Path("/name/{name}")
		// CompletionStage<Set<Country>> getByNameAsync(@PathParam String name);
		CodeLens lensForGetSingle = lenses.get(1);
		Assert.assertNotNull(lensForGetSingle.getCommand());
		Assert.assertEquals(baseURL + "/v2/name/{name}", lensForGetSingle.getCommand().getTitle());
	}

}
