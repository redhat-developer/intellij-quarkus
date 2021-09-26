/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
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
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProject;
import com.redhat.devtools.intellij.MavenImportingTestCase;
import org.eclipse.lsp4j.Position;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.saveFile;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.assertJavaHover;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.fixURI;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.h;

/**
 * JDT Quarkus manager test for hover in Java file.
 * 
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/JavaHoverTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/JavaHoverTest.java</a>
 *
 */
public class MavenMicroProfileConfigJavaHoverTest extends MavenImportingTestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Test
	public void testConfigPropertyNameHover() throws Exception {
		Module javaProject = createMavenModule("config-hover", new File("projects/maven/config-hover"));
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/config/GreetingResource.java").toURI());
		String propertiesFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/resources/application.properties").toURI());

		saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE, //
				"greeting.message = hello\r\n" + //
						"greeting.name = quarkus\r\n" + //
						"greeting.number = 100",
				javaProject);
		// Position(14, 40) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.mes|sage")
		assertJavaHover(new Position(14, 40), javaFileUri, PsiUtilsLSImpl.getInstance(),
				h("`greeting.message = hello` *in* [application.properties](" + propertiesFileUri + ")", 14, 28, 44));

		// Test left edge
		// Position(14, 28) is the character after the | symbol:
		// @ConfigProperty(name = "|greeting.message")
		assertJavaHover(new Position(14, 28), javaFileUri, PsiUtilsLSImpl.getInstance(),
				h("`greeting.message = hello` *in* [application.properties](" + propertiesFileUri + ")", 14, 28, 44));

		// Test right edge
		// Position(14, 43) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.messag|e")
		assertJavaHover(new Position(14, 43), javaFileUri, PsiUtilsLSImpl.getInstance(),
				h("`greeting.message = hello` *in* [application.properties](" + propertiesFileUri + ")", 14, 28, 44));

		// Test no hover
		// Position(14, 27) is the character after the | symbol:
		// @ConfigProperty(name = |"greeting.message")
		assertJavaHover(new Position(14, 27), javaFileUri, PsiUtilsLSImpl.getInstance(), null);

		// Test no hover 2
		// Position(14, 44) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.message|")
		assertJavaHover(new Position(14, 44), javaFileUri, PsiUtilsLSImpl.getInstance(), null);

		// Hover default value
		// Position(17, 33) is the character after the | symbol:
		// @ConfigProperty(name = "greet|ing.suffix", defaultValue="!")
		assertJavaHover(new Position(17, 33), javaFileUri, PsiUtilsLSImpl.getInstance(),
				h("`greeting.suffix = !` *in* [GreetingResource.java](" + javaFileUri + ")", 17, 28, 43));

		// Hover override default value
		// Position(26, 33) is the character after the | symbol:
		// @ConfigProperty(name = "greet|ing.number", defaultValue="0")
		assertJavaHover(new Position(26, 33), javaFileUri, PsiUtilsLSImpl.getInstance(),
				h("`greeting.number = 100` *in* [application.properties](" + propertiesFileUri + ")", 26, 28, 43));

		// Hover when no value
		// Position(23, 33) is the character after the | symbol:
		// @ConfigProperty(name = "greet|ing.missing")
		assertJavaHover(new Position(23, 33), javaFileUri, PsiUtilsLSImpl.getInstance(), h("`greeting.missing` is not set", 23, 28, 44));
	}

	@Test
	public void testConfigPropertyNameHoverWithProfiles() throws Exception {
		Module javaProject = createMavenModule("config-hover", new File("projects/maven/config-hover"));
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/config/GreetingResource.java").toURI());
		String propertiesFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/resources/application.properties").toURI());

		saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE, //
				"greeting.message = hello\r\n" + //
						"%dev.greeting.message = hello dev\r\n" + //
						"%prod.greeting.message = hello prod\r\n" + //
						"my.greeting.message\r\n" + //
						"%dev.my.greeting.message",
				javaProject);

		// Position(14, 40) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.mes|sage")
		assertJavaHover(new Position(14, 40), javaFileUri, PsiUtilsLSImpl.getInstance(), //
				h("`%dev.greeting.message = hello dev` *in* [application.properties](" + propertiesFileUri + ")  \n" + //
								"`%prod.greeting.message = hello prod` *in* [application.properties](" + propertiesFileUri
								+ ")  \n" + //
								"`greeting.message = hello` *in* [application.properties](" + propertiesFileUri + ")", //
						14, 28, 44));

		saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE, //
				"%dev.greeting.message = hello dev\r\n" + //
						"%prod.greeting.message = hello prod\r\n" + //
						"my.greeting.message\r\n" + //
						"%dev.my.greeting.message",
				javaProject);

		// Position(14, 40) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.mes|sage")
		assertJavaHover(new Position(14, 40), javaFileUri, PsiUtilsLSImpl.getInstance(), //
				h("`%dev.greeting.message = hello dev` *in* [application.properties](" + propertiesFileUri + ")  \n" + //
								"`%prod.greeting.message = hello prod` *in* [application.properties](" + propertiesFileUri
								+ ")  \n" + //
								"`greeting.message` is not set", //
						14, 28, 44));
	}


	@Test
	public void testConfigPropertyNameYaml() throws Exception {
		Module javaProject = createMavenModule("config-hover", new File("projects/maven/config-hover"));
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/config/GreetingResource.java").toURI());
		String yamlFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/resources/application.yaml").toURI());
		String propertiesFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/resources/application.properties").toURI());

		saveFile(PsiMicroProfileProject.APPLICATION_YAML_FILE, //
				"greeting:\n" + //
						"  message: message from yaml\n" + //
						"  number: 2001",
				javaProject);

		saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE, //
				"greeting.message = hello\r\n" + //
						"greeting.name = quarkus\r\n" + //
						"greeting.number = 100",
				javaProject);

		// Position(14, 40) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.mes|sage")
		assertJavaHover(new Position(14, 40), javaFileUri, PsiUtilsLSImpl.getInstance(),
				h("`greeting.message = message from yaml` *in* [application.yaml](" + yamlFileUri + ")", 14, 28, 44));

		// Position(26, 33) is the character after the | symbol:
		// @ConfigProperty(name = "greet|ing.number", defaultValue="0")
		assertJavaHover(new Position(26, 33), javaFileUri, PsiUtilsLSImpl.getInstance(),
				h("`greeting.number = 2001` *in* [application.yaml](" + yamlFileUri + ")", 26, 28, 43));

		saveFile(PsiMicroProfileProject.APPLICATION_YAML_FILE, //
				"greeting:\n" + //
						"  message: message from yaml",
				javaProject);
		// fallback to application.properties
		assertJavaHover(new Position(26, 33), javaFileUri, PsiUtilsLSImpl.getInstance(),
				h("`greeting.number = 100` *in* [application.properties](" + propertiesFileUri + ")", 26, 28, 43));
	}

	@Test
	public void testConfigPropertyNameMethod() throws Exception {
		Module javaProject = createMavenModule("config-quickstart", new File("projects/maven/config-quickstart"));
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/config/GreetingMethodResource.java").toURI());
		String propertiesFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/resources/application.properties").toURI());

		saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE, "greeting.method.message = hello", javaProject);

		// Position(22, 61) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.m|ethod.message")
		assertJavaHover(new Position(22, 61), javaFileUri, PsiUtilsLSImpl.getInstance(),
				h("`greeting.method.message = hello` *in* [application.properties](" + propertiesFileUri + ")", 22, 51,
						74));

		// Position(27, 60) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.m|ethod.suffix" , defaultValue="!")
		assertJavaHover(new Position(27, 60), javaFileUri, PsiUtilsLSImpl.getInstance(),
				h("`greeting.method.suffix = !` *in* [GreetingMethodResource.java](" + javaFileUri + ")", 27, 50, 72));

		// Position(32, 58) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.method.name")
		assertJavaHover(new Position(32, 48), javaFileUri, PsiUtilsLSImpl.getInstance(),
				h("`greeting.method.name` is not set", 32, 48, 68));
	}

	@Test
	public void testConfigPropertyNameConstructor() throws Exception {
		Module javaProject = createMavenModule("config-quickstart", new File("projects/maven/config-quickstart"));
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/config/GreetingConstructorResource.java").toURI());
		String propertiesFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/resources/application.properties").toURI());

		saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE, "greeting.constructor.message = hello",
				javaProject);

		// Position(23, 48) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.con|structor.message")
		assertJavaHover(new Position(23, 48), javaFileUri, PsiUtilsLSImpl.getInstance(), //
				h("`greeting.constructor.message = hello` *in* [application.properties](" + propertiesFileUri + ")", 23,
						36, 64));

		// Position(24, 48) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.con|structor.suffix" , defaultValue="!")
		assertJavaHover(new Position(24, 48), javaFileUri, PsiUtilsLSImpl.getInstance(), //
				h("`greeting.constructor.suffix = !` *in* [GreetingConstructorResource.java](" + javaFileUri + ")", 24,
						36, 63));

		// Position(25, 48) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.con|structor.name")
		assertJavaHover(new Position(25, 48), javaFileUri, PsiUtilsLSImpl.getInstance(), //
				h("`greeting.constructor.name` is not set", 25, 36, 61));
	}

	@Test
	public void testConfigPropertyNameRespectsPrecendence() throws Exception {
		Module javaProject = createMavenModule("config-quickstart", new File("projects/maven/config-quickstart"));
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/config/GreetingConstructorResource.java").toURI());
		String propertiesFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/resources/application.properties").toURI());
		String configFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/META-INF/microprofile-config.properties").toURI());
		String yamlFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/application.yaml").toURI());

		// microprofile-config.properties exists
		saveFile(PsiMicroProfileProject.MICROPROFILE_CONFIG_PROPERTIES_FILE, "greeting.constructor.message = hello 1",
				javaProject);
		assertJavaHover(new Position(23, 48), javaFileUri, PsiUtilsLSImpl.getInstance(),
				h("`greeting.constructor.message = hello 1` *in* [META-INF/microprofile-config.properties](" + configFileUri + ")", 23, 36, 64));

		// microprofile-config.properties and application.properties exist
		saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE, "greeting.constructor.message = hello 2",
				javaProject);
		assertJavaHover(new Position(23, 48), javaFileUri, PsiUtilsLSImpl.getInstance(),
				h("`greeting.constructor.message = hello 2` *in* [application.properties](" + propertiesFileUri + ")",
						23, 36, 64));

		// microprofile-config.properties, application.properties, and application.yaml
		// exist
		saveFile(PsiMicroProfileProject.APPLICATION_YAML_FILE, //
				"greeting:\n" + //
						"  constructor:\n" + //
						"    message: hello 3", //
				javaProject);
		assertJavaHover(new Position(23, 48), javaFileUri, PsiUtilsLSImpl.getInstance(),
				h("`greeting.constructor.message = hello 3` *in* [application.yaml](" + yamlFileUri + ")", 23, 36, 64));
	}
}
