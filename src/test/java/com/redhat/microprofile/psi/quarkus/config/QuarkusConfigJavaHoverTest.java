/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.microprofile.psi.quarkus.config;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.providers.MicroProfileConfigSourceProvider;
import com.redhat.devtools.intellij.quarkus.psi.internal.providers.QuarkusConfigSourceProvider;
import com.redhat.microprofile.psi.quarkus.QuarkusMavenModuleImportingTestCase;
import com.redhat.microprofile.psi.quarkus.QuarkusMavenProjectName;
import org.eclipse.lsp4j.Position;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.saveFile;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.*;

/**
 * JDT Quarkus manager test for hover in Java file.
 * 
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/JavaHoverTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/JavaHoverTest.java</a>
 *
 */
public class QuarkusConfigJavaHoverTest extends QuarkusMavenModuleImportingTestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Test
	public void testConfigPropertyNameRespectsPrecendence() throws Exception {
		Module javaProject = loadMavenProject(QuarkusMavenProjectName.config_quickstart);
		String javaFileUri = getFileUri("src/main/java/org/acme/config/GreetingConstructorResource.java", javaProject);
		String propertiesFileUri = getFileUri("src/main/resources/application.properties", javaProject);

		//fix for having application.yaml being part of the QuarkusConfigSourceProvider
		saveFile(QuarkusConfigSourceProvider.APPLICATION_YAML_FILE, "", javaProject);
		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, "", javaProject);

		// microprofile-config.properties exists
		saveFile(MicroProfileConfigSourceProvider.MICROPROFILE_CONFIG_PROPERTIES_FILE,
				"greeting.constructor.message = hello 1", javaProject);
		assertJavaHover(new Position(23, 48), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()),
				h("`greeting.constructor.message = hello 1` *in* META-INF/microprofile-config.properties", 23, 36, 64));

		// microprofile-config.properties and application.properties exist
		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, "greeting.constructor.message = hello 2",
				javaProject);
		assertJavaHover(new Position(23, 48), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()),
				h("`greeting.constructor.message = hello 2` *in* [application.properties](" + propertiesFileUri + ")",
						23, 36, 64));

		// microprofile-config.properties, application.properties, and application.yaml
		// exist
		saveFile(QuarkusConfigSourceProvider.APPLICATION_YAML_FILE, //
				"greeting:\n" + //
						"  constructor:\n" + //
						"    message: hello 3", //
				javaProject);
		assertJavaHover(new Position(23, 48), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()),
				h("`greeting.constructor.message = hello 3` *in* application.yaml", 23, 36, 64));
	}

	@Test
	public void testConfigPropertyNameYaml() throws Exception {
		Module javaProject = loadMavenProject(QuarkusMavenProjectName.config_hover);
		String javaFileUri = getFileUri("src/main/java/org/acme/config/GreetingResource.java", javaProject);
		String yamlFileUri = getFileUri("src/main/resources/application.yaml", javaProject);
		String propertiesFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/resources/application.properties").toURI());

		saveFile(QuarkusConfigSourceProvider.APPLICATION_YAML_FILE, //
				"greeting:\n" + //
						"  message: message from yaml\n" + //
						"  number: 2001",
				javaProject);

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, //
				"greeting.message = hello\r\n" + //
						"greeting.name = quarkus\r\n" + //
						"greeting.number = 100",
				javaProject);

		// Position(14, 40) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.mes|sage")
		assertJavaHover(new Position(14, 40), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()),
				h("`greeting.message = message from yaml` *in* [application.yaml](" + yamlFileUri + ")", 14, 28, 44));

		// Position(26, 33) is the character after the | symbol:
		// @ConfigProperty(name = "greet|ing.number", defaultValue="0")
		assertJavaHover(new Position(26, 33), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()),
				h("`greeting.number = 2001` *in* [application.yaml](" + yamlFileUri + ")", 26, 28, 43));

		saveFile(QuarkusConfigSourceProvider.APPLICATION_YAML_FILE, //
				"greeting:\n" + //
						"  message: message from yaml",
				javaProject);
		// fallback to application.properties
		assertJavaHover(new Position(26, 33), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()),
				h("`greeting.number = 100` *in* [application.properties](" + propertiesFileUri + ")", 26, 28, 43));
	}

	@Test
	public void testConfigPropertyNameYml() throws Exception {
		Module javaProject = loadMavenProject(QuarkusMavenProjectName.config_hover);
		String javaFileUri = getFileUri("src/main/java/org/acme/config/GreetingResource.java", javaProject);
		String ymlFileUri = getFileUri("src/main/resources/application.yml", javaProject);
		String propertiesFileUri = getFileUri("src/main/resources/application.properties", javaProject);

		saveFile(QuarkusConfigSourceProvider.APPLICATION_YML_FILE, //
				"greeting:\n" + //
						"  message: message from yml\n" + //
						"  number: 2001",
				javaProject);

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, //
				"greeting.message = hello\r\n" + //
						"greeting.name = quarkus\r\n" + //
						"greeting.number = 100",
				javaProject);

		// Position(14, 40) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.mes|sage")
		assertJavaHover(new Position(14, 40), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()),
				h("`greeting.message = message from yml` *in* [application.yml](" + ymlFileUri + ")", 14, 28, 44));

		// Position(26, 33) is the character after the | symbol:
		// @ConfigProperty(name = "greet|ing.number", defaultValue="0")
		assertJavaHover(new Position(26, 33), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()),
				h("`greeting.number = 2001` *in* [application.yml](" + ymlFileUri + ")", 26, 28, 43));

		saveFile(QuarkusConfigSourceProvider.APPLICATION_YML_FILE, //
				"greeting:\n" + //
						"  message: message from yml",
				javaProject);
		// fallback to application.properties
		assertJavaHover(new Position(26, 33), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()),
				h("`greeting.number = 100` *in* [application.properties](" + propertiesFileUri + ")", 26, 28, 43));
	}

	@Test
	public void testConfigPropertyHoverPropertyExpression() throws Exception {

		Module javaProject = loadMavenProject(QuarkusMavenProjectName.config_hover);
		String javaFileUri = getFileUri("src/main/java/org/acme/config/GreetingResource.java", javaProject);

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, "greeting.hover = test", javaProject);

		// no hover should show for property expression
		assertJavaHover(new Position(31, 29), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()), null);
	}

	@Test
	public void testPerProfileConfigPropertyFile() throws Exception {

		Module javaProject = loadMavenProject(QuarkusMavenProjectName.config_hover);
		String javaFileUri = getFileUri("src/main/java/org/acme/config/GreetingResource.java", javaProject);
		String propertiesFileUri = getFileUri("src/main/resources/application-foo.properties", javaProject);

		saveFile("application-foo.properties", "greeting.message = hello from foo profile\n",
				javaProject);
		assertJavaHover(new Position(14, 29), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()),
				h("`%foo.greeting.message = hello from foo profile` *in* [application-foo.properties](" + propertiesFileUri + ")  \n`greeting.message` is not set", 14, 28, 44));

	}

}
