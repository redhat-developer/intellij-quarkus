/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.config;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.redhat.devtools.intellij.GradleTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManagerForJava;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.providers.DefaultMicroProfilePropertiesConfigSourceProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.providers.QuarkusConfigSourceProvider;
import org.apache.commons.io.FileUtils;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileJavaHoverParams;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.saveFile;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.assertJavaDefinitions;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.assertJavaHover;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.def;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.fixURI;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.h;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.p;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.r;

/**
 * JDT Quarkus manager test for hover in Java file.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/JavaHoverTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/JavaHoverTest.java</a>
 *
 */
public class GradleMicroProfileConfigJavaDefinitionTest extends GradleTestCase {

	private Module loadProject(String name) throws IOException {
		FileUtils.copyDirectory(new File("projects/gradle/" + name), new File(getProjectPath()));
		importProject();
		return getModule(name + ".main");
	}

	@Test
	public void testConfigPropertyNameDefinition() throws Exception {

		Module javaProject = loadProject("config-hover");

		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/config/GreetingResource.java").toURI());
		String propertiesFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/resources/application.properties").toURI());

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, //
				"greeting.message = hello\r\n" + //
						"greeting.name = quarkus\r\n" + //
						"greeting.number = 100",
				javaProject);
		// Position(14, 40) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.mes|sage")
		assertJavaDefinitions(p(14, 40), javaFileUri, PsiUtilsLSImpl.getInstance(myProject), //
				def(r(14, 28, 44), propertiesFileUri, "greeting.message"));

		// Test left edge
		// Position(14, 28) is the character after the | symbol:
		// @ConfigProperty(name = "|greeting.message")
		assertJavaDefinitions(p(14, 28), javaFileUri, PsiUtilsLSImpl.getInstance(myProject), //
				def(r(14, 28, 44), propertiesFileUri, "greeting.message"));

		// Test right edge
		// Position(14, 43) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.messag|e")
		assertJavaDefinitions(p(14, 43), javaFileUri, PsiUtilsLSImpl.getInstance(myProject), //
				def(r(14, 28, 44), propertiesFileUri, "greeting.message"));

		// Test no hover
		// Position(14, 27) is the character after the | symbol:
		// @ConfigProperty(name = |"greeting.message")
		assertJavaDefinitions(p(14, 27), javaFileUri, PsiUtilsLSImpl.getInstance(myProject));

		// Test no hover 2
		// Position(14, 44) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.message|")
		assertJavaDefinitions(p(14, 44), javaFileUri, PsiUtilsLSImpl.getInstance(myProject));

		// Definition override default value
		// Position(26, 33) is the character after the | symbol:
		// @ConfigProperty(name = "greet|ing.number", defaultValue="0")
		assertJavaDefinitions(p(26, 33), javaFileUri, PsiUtilsLSImpl.getInstance(myProject), //
				def(r(26, 28, 43), propertiesFileUri, "greeting.number"));

		// Definition when no value
		// Position(23, 33) is the character after the | symbol:
		// @ConfigProperty(name = "greet|ing.missing")
		assertJavaDefinitions(p(23, 33), javaFileUri, PsiUtilsLSImpl.getInstance(myProject));

	}

	@Test
	public void testConfigPropertyNameDefinitionYml() throws Exception {

		Module javaProject = loadProject("config-hover");
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/config/GreetingResource.java").toURI());
		String applicationYmlFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/resources/application.yml").toURI());

		saveFile(QuarkusConfigSourceProvider.APPLICATION_YML_FILE, //
				"greeting:\n" + //
						"  message: hello\n" + //
						"  name: quarkus\n" + //
						"  number: 100\n",
				javaProject);
		// Position(14, 40) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.mes|sage")
		assertJavaDefinitions(p(14, 40), javaFileUri, PsiUtilsLSImpl.getInstance(myProject), //
				def(r(14, 28, 44), applicationYmlFileUri, "greeting.message"));

	}
}
