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
package com.redhat.microprofile.psi.quarkus.jaxrs;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManagerForJava;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProject;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeLensParams;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.saveFile;

/**
 * JAX-RS URL Codelens test for Java file.
 * 
 * @author Angelo ZERR
 *
 */
public class MavenJaxRsCodeLensTest extends MavenModuleImportingTestCase {

	@Test
	public void testUrlCodeLensProperties() throws Exception {
		Module javaProject = createMavenModule("hibernate-orm-resteasy", new File("projects/quarkus/maven/hibernate-orm-resteasy"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaCodeLensParams params = new MicroProfileJavaCodeLensParams();
		params.setCheckServerAvailable(false);
		VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(javaProject) + "/src/main/java/org/acme/hibernate/orm/FruitResource.java");
		String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();
		params.setUri(uri);

		params.setUrlCodeLensEnabled(true);

		// Default port
		assertCodeLenses(8080, params, utils);

		// META-INF/microprofile-config.properties : 8081
		saveFile(PsiMicroProfileProject.MICROPROFILE_CONFIG_PROPERTIES_FILE, "quarkus.http.port = 8081", javaProject);
		assertCodeLenses(8081, params, utils);

		// application.properties : 8082 -> it overrides 8081 coming from the
		// META-INF/microprofile-config.properties
		saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE, "quarkus.http.port = 8082", javaProject);
		assertCodeLenses(8082, params, utils);

		// application.properties : 8083
		// META-INF/microprofile-config.properties
		saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE, "quarkus.http.port = 8083", javaProject);
		assertCodeLenses(8083, params, utils);

		// remove quarkus.http.port from application.properties
		saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE, "", javaProject);
		assertCodeLenses(8081, params, utils); // here port is 8081 coming from META-INF/microprofile-config.properties
		
		// Set a different value for the dev profile.
		// If the dev profile for quarkus.http.port exists, this should be used instead of the default profile
		saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE, "quarkus.http.port = 8080\n%dev.quarkus.http.port = 9090", javaProject);
		assertCodeLenses(9090, params, utils);
	}

	@Test
	public void testUrlCodeLensYaml() throws Exception {
		Module javaProject = createMavenModule("hibernate-orm-resteasy", new File("projects/quarkus/maven/hibernate-orm-resteasy"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaCodeLensParams params = new MicroProfileJavaCodeLensParams();
		params.setCheckServerAvailable(false);
		VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(javaProject) + "/src/main/java/org/acme/hibernate/orm/FruitResource.java");
		String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();
		params.setUri(uri);
		params.setUrlCodeLensEnabled(true);

		// Default port
		assertCodeLenses(8080, params, utils);

		// application.yaml : 8081
		saveFile(PsiMicroProfileProject.APPLICATION_YAML_FILE, "quarkus:\n" + "  http:\n" + "    port: 8081",
				javaProject);
		assertCodeLenses(8081, params, utils);

		// application.properties : 8082 -> application.yaml overrides
		// application.properties
		saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE, "quarkus.http.port = 8082", javaProject);
		assertCodeLenses(8081, params, utils);

		// remove quarkus.http.port from application.yaml
		saveFile(PsiMicroProfileProject.APPLICATION_YAML_FILE, "", javaProject);
		assertCodeLenses(8082, params, utils); // here port is 8082 coming from application.properties

		// application.yaml: 8083 with more keys and a prefix related name conflict
		saveFile(PsiMicroProfileProject.APPLICATION_YAML_FILE, "quarkus:\r\n" + //
				"  application:\r\n" + //
				"    name: name\r\n" + //
				"    version: version\r\n" + //
				"  http:\r\n" + //
				"    port:\r\n" + //
				"      ~: 8083\r\n" + //
				"      unknown_property: 123", javaProject);
		assertCodeLenses(8083, params, utils);

	}

	private static void assertCodeLenses(int port, MicroProfileJavaCodeLensParams params, IPsiUtils utils) {
		List<? extends CodeLens> lenses = PropertiesManagerForJava.getInstance().codeLens(params, utils);
		Assert.assertEquals(2, lenses.size());

		// @GET
		// public Fruit[] get() {
		CodeLens lensForGet = lenses.get(0);
		Assert.assertNotNull(lensForGet.getCommand());
		Assert.assertEquals("http://localhost:" + port + "/fruits", lensForGet.getCommand().getTitle());

		// @GET
		// @Path("{id}")
		// public Fruit getSingle(@PathParam Integer id) {
		CodeLens lensForGetSingle = lenses.get(1);
		Assert.assertNotNull(lensForGetSingle.getCommand());
		Assert.assertEquals("http://localhost:" + port + "/fruits/{id}", lensForGetSingle.getCommand().getTitle());
	}

}