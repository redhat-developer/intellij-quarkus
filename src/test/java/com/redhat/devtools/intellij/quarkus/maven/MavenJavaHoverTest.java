/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.maven;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.redhat.devtools.intellij.quarkus.search.PsiUtilsImpl;
import com.redhat.devtools.intellij.quarkus.search.core.PropertiesManagerForJava;
import com.redhat.devtools.intellij.quarkus.search.core.project.PsiMicroProfileProject;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileJavaHoverParams;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.assertHoverEquals;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.ho;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.saveFile;

/**
 * JDT Quarkus manager test for hover in Java file.
 * 
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/JavaHoverTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/JavaHoverTest.java</a>
 *
 */
public class MavenJavaHoverTest extends MavenImportingTestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Test
	public void testConfigPropertyNameHover() throws Exception {
		Module javaProject = createMavenModule("config-hover", new File("projects/maven/config-hover"));
		String javaFileUri = new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/config/GreetingResource.java").toURI().toString();
		saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE,
				"greeting.message = hello\r\n" + "greeting.name = quarkus\r\n" + "greeting.number = 100", javaProject);
		// Position(14, 40) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.mes|sage")
		Hover info = getActualHover(new Position(14, 40), javaFileUri);
		assertHoverEquals(ho("`greeting.message = hello` *in* application.properties", 14, 28, 44), info);

		// Test left edge
		// Position(14, 28) is the character after the | symbol:
		// @ConfigProperty(name = "|greeting.message")
		info = getActualHover(new Position(14, 28), javaFileUri);
		assertHoverEquals(ho("`greeting.message = hello` *in* application.properties", 14, 28, 44), info);

		// Test right edge
		// Position(14, 43) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.messag|e")
		info = getActualHover(new Position(14, 43), javaFileUri);
		assertHoverEquals(ho("`greeting.message = hello` *in* application.properties", 14, 28, 44), info);

		// Test no hover
		// Position(14, 27) is the character after the | symbol:
		// @ConfigProperty(name = |"greeting.message")
		info = getActualHover(new Position(14, 27), javaFileUri);
		assertNull(info);

		// Test no hover 2
		// Position(14, 44) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.message|")
		info = getActualHover(new Position(14, 44), javaFileUri);
		assertNull(info);

		// Hover default value
		// Position(17, 33) is the character after the | symbol:
		// @ConfigProperty(name = "greet|ing.suffix", defaultValue="!")
		info = getActualHover(new Position(17, 33), javaFileUri);
		assertHoverEquals(ho("`greeting.suffix = !` *in* GreetingResource.java", 17, 28, 43), info);

		// Hover override default value
		// Position(26, 33) is the character after the | symbol:
		// @ConfigProperty(name = "greet|ing.number", defaultValue="0")
		info = getActualHover(new Position(26, 33), javaFileUri);
		assertHoverEquals(ho("`greeting.number = 100` *in* application.properties", 26, 28, 43), info);

		// Hover when no value
		// Position(23, 33) is the character after the | symbol:
		// @ConfigProperty(name = "greet|ing.missing")
		info = getActualHover(new Position(23, 33), javaFileUri);
		assertHoverEquals(ho("`greeting.missing` is not set", 23, 28, 44), info);
	}

	@Test
	public void testConfigPropertyNameHoverWithProfiles() throws Exception {
		Module javaProject = createMavenModule("config-hover", new File("projects/maven/config-hover"));
		String javaFileUri = new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/config/GreetingResource.java").toURI().toString();
		saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE, //
				"greeting.message = hello\r\n" + //
						"%dev.greeting.message = hello dev\r\n" + //
						"%prod.greeting.message = hello prod\r\n" + //
						"my.greeting.message\r\n" + //
						"%dev.my.greeting.message",
				javaProject);

		// Position(14, 40) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.mes|sage")
		Hover info = getActualHover(new Position(14, 40), javaFileUri);
		assertHoverEquals(ho("`%dev.greeting.message = hello dev` *in* application.properties  \n" + //
						"`%prod.greeting.message = hello prod` *in* application.properties  \n" + //
						"`greeting.message = hello` *in* application.properties", //
				14, 28, 44), info);

		saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE, //
				"%dev.greeting.message = hello dev\r\n" + //
						"%prod.greeting.message = hello prod\r\n" + //
						"my.greeting.message\r\n" + //
						"%dev.my.greeting.message",
				javaProject);

		// Position(14, 40) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.mes|sage")
		info = getActualHover(new Position(14, 40), javaFileUri);
		assertHoverEquals(ho("`%dev.greeting.message = hello dev` *in* application.properties  \n" + //
						"`%prod.greeting.message = hello prod` *in* application.properties  \n" + //
						"`greeting.message` is not set", //
				14, 28, 44), info);
	}


	@Test
	public void testConfigPropertyNameYaml() throws Exception {
		Module javaProject = createMavenModule("config-hover", new File("projects/maven/config-hover"));
		String javaFileUri = new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/config/GreetingResource.java").toURI().toString();
		saveFile(PsiMicroProfileProject.APPLICATION_YAML_FILE,
				"greeting:\n" + "  message: message from yaml\n" + "  number: 2001", javaProject);

		saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE,
				"greeting.message = hello\r\n" + "greeting.name = quarkus\r\n" + "greeting.number = 100", javaProject);

		// Position(14, 40) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.mes|sage")
		Hover info = getActualHover(new Position(14, 40), javaFileUri);
		assertHoverEquals(ho("`greeting.message = message from yaml` *in* application.yaml", 14, 28, 44), info);

		// Position(26, 33) is the character after the | symbol:
		// @ConfigProperty(name = "greet|ing.number", defaultValue="0")
		info = getActualHover(new Position(26, 33), javaFileUri);
		assertHoverEquals(ho("`greeting.number = 2001` *in* application.yaml", 26, 28, 43), info);

		saveFile(PsiMicroProfileProject.APPLICATION_YAML_FILE, "greeting:\n" + "  message: message from yaml",
				javaProject);

		// fallback to application.properties
		info = getActualHover(new Position(26, 33), javaFileUri);
		assertHoverEquals(ho("`greeting.number = 100` *in* application.properties", 26, 28, 43), info);
	}

	@Test
	public void testConfigPropertyNameMethod() throws Exception {
		Module javaProject = createMavenModule("config-quickstart", new File("projects/maven/config-quickstart"));
		String javaFileUri = new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/config/GreetingMethodResource.java").toURI().toString();
		saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE, "greeting.method.message = hello", javaProject);

		// Position(22, 61) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.m|ethod.message")
		Hover info = getActualHover(new Position(22, 61), javaFileUri);
		assertHoverEquals(ho("`greeting.method.message = hello` *in* application.properties", 22, 51, 74), info);

		// Position(27, 60) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.m|ethod.suffix" , defaultValue="!")
		info = getActualHover(new Position(27, 60), javaFileUri);
		assertHoverEquals(ho("`greeting.method.suffix = !` *in* GreetingMethodResource.java", 27, 50, 72), info);

		// Position(32, 58) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.method.name")
		info = getActualHover(new Position(32, 58), javaFileUri);
		assertHoverEquals(ho("`greeting.method.name` is not set", 32, 48, 68), info);
	}

	@Test
	public void testConfigPropertyNameConstructor() throws Exception {
		Module javaProject = createMavenModule("config-quickstart", new File("projects/maven/config-quickstart"));
		String javaFileUri = new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/config/GreetingConstructorResource.java").toURI().toString();
		saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE, "greeting.constructor.message = hello",
				javaProject);
		Hover info;

		// Position(23, 48) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.con|structor.message")
		info = getActualHover(new Position(23, 48), javaFileUri);
		assertHoverEquals(ho("`greeting.constructor.message = hello` *in* application.properties", 23, 36, 64), info);

		// Position(24, 48) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.con|structor.suffix" , defaultValue="!")
		info = getActualHover(new Position(24, 48), javaFileUri);
		assertHoverEquals(ho("`greeting.constructor.suffix = !` *in* GreetingConstructorResource.java", 24, 36, 63),
				info);

		// Position(25, 48) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.con|structor.name")
		info = getActualHover(new Position(25, 48), javaFileUri);
		assertHoverEquals(ho("`greeting.constructor.name` is not set", 25, 36, 61), info);
	}

	@Test
	public void testConfigPropertyNameRespectsPrecendence() throws Exception {
		Module javaProject = createMavenModule("config-quickstart", new File("projects/maven/config-quickstart"));
		String javaFileUri = new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/config/GreetingConstructorResource.java").toURI().toString();
		Hover info;

		// microprofile-config.properties exists
		saveFile(PsiMicroProfileProject.MICROPROFILE_CONFIG_PROPERTIES_FILE, "greeting.constructor.message = hello 1",
				javaProject);
		info = getActualHover(new Position(23, 48), javaFileUri);
		assertHoverEquals(ho("`greeting.constructor.message = hello 1` *in* META-INF/microprofile-config.properties", 23, 36, 64), info);

		// microprofile-config.properties and application.properties exist
		saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE, "greeting.constructor.message = hello 2",
				javaProject);
		info = getActualHover(new Position(23, 48), javaFileUri);
		assertHoverEquals(ho("`greeting.constructor.message = hello 2` *in* application.properties", 23, 36, 64), info);

		// microprofile-config.properties, application.properties, and application.yaml exist
		saveFile(PsiMicroProfileProject.APPLICATION_YAML_FILE, //
				"greeting:\n" + //
						"  constructor:\n" + //
						"    message: hello 3", //
				javaProject);
		info = getActualHover(new Position(23, 48), javaFileUri);
		assertHoverEquals(ho("`greeting.constructor.message = hello 3` *in* application.yaml", 23, 36, 64), info);

	}

	private Hover getActualHover(Position hoverPosition, String javaFileUri) {
		MicroProfileJavaHoverParams params = new MicroProfileJavaHoverParams();
		params.setDocumentFormat(DocumentFormat.Markdown);
		params.setPosition(hoverPosition);
		params.setUri(javaFileUri);
		params.setSurroundEqualsWithSpaces(true);

		return PropertiesManagerForJava.getInstance().hover(params, PsiUtilsImpl.getInstance());
	}
}
