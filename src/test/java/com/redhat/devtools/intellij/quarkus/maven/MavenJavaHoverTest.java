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
import com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert;
import com.redhat.devtools.intellij.quarkus.search.PsiUtilsImpl;
import com.redhat.devtools.intellij.quarkus.search.core.PropertiesManagerForJava;
import com.redhat.devtools.intellij.quarkus.search.core.project.PsiMicroProfileProject;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileJavaHoverParams;
import org.junit.Test;

import java.io.File;

/**
 * JDT Quarkus manager test for hover in Java file.
 * 
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/JavaHoverTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/JavaHoverTest.java</a>
 *
 */
public class MavenJavaHoverTest extends MavenImportingTestCase {
	private Module javaProject;
	private String javaFileUri;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		javaProject = createMavenModule("config-hover", new File("projects/maven/config-hover"));
		javaFileUri = new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/config/GreetingResource.java").toURI().toString();
	}

	@Test
	public void testConfigPropertyNameHover() throws Exception {
		MicroProfileAssert.saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE,
				"greeting.message = hello\r\n" + "greeting.name = quarkus\r\n" + "greeting.number = 100", javaProject);
		// Position(14, 40) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.mes|sage")
		Hover info = getActualHover(new Position(14, 40));
		MicroProfileAssert.assertHover("greeting.message", "hello", 14, 28, 44, info);

		// Test left edge
		// Position(14, 28) is the character after the | symbol:
		// @ConfigProperty(name = "|greeting.message")
		info = getActualHover(new Position(14, 28));
		MicroProfileAssert.assertHover("greeting.message", "hello", 14, 28, 44, info);

		// Test right edge
		// Position(14, 43) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.messag|e")
		info = getActualHover(new Position(14, 43));
		MicroProfileAssert.assertHover("greeting.message", "hello", 14, 28, 44, info);

		// Test no hover
		// Position(14, 27) is the character after the | symbol:
		// @ConfigProperty(name = |"greeting.message")
		info = getActualHover(new Position(14, 27));
		assertNull(info);

		// Test no hover 2
		// Position(14, 44) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.message|")
		info = getActualHover(new Position(14, 44));
		assertNull(info);

		// Hover default value
		// Position(17, 33) is the character after the | symbol:
		// @ConfigProperty(name = "greet|ing.suffix", defaultValue="!")
		info = getActualHover(new Position(17, 33));
		MicroProfileAssert.assertHover("greeting.suffix", "!", 17, 28, 43, info);

		// Hover override default value
		// Position(26, 33) is the character after the | symbol:
		// @ConfigProperty(name = "greet|ing.number", defaultValue="0")
		info = getActualHover(new Position(26, 33));
		MicroProfileAssert.assertHover("greeting.number", "100", 26, 28, 43, info);

		// Hover when no value
		// Position(23, 33) is the character after the | symbol:
		// @ConfigProperty(name = "greet|ing.missing")
		info = getActualHover(new Position(23, 33));
		MicroProfileAssert.assertHover("greeting.missing", null, 23, 28, 44, info);
	}

	@Test
	public void testConfigPropertyNameYaml() throws Exception {
		MicroProfileAssert.saveFile(PsiMicroProfileProject.APPLICATION_YAML_FILE,
				"greeting:\n" + "  message: message from yaml\n" + "  number: 2001", javaProject);

		MicroProfileAssert.saveFile(PsiMicroProfileProject.APPLICATION_PROPERTIES_FILE,
				"greeting.message = hello\r\n" + "greeting.name = quarkus\r\n" + "greeting.number = 100", javaProject);

		// Position(14, 40) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.mes|sage")
		Hover info = getActualHover(new Position(14, 40));
		MicroProfileAssert.assertHover("greeting.message", "message from yaml", 14, 28, 44, info);

		// Position(26, 33) is the character after the | symbol:
		// @ConfigProperty(name = "greet|ing.number", defaultValue="0")
		info = getActualHover(new Position(26, 33));
		MicroProfileAssert.assertHover("greeting.number", "2001", 26, 28, 43, info);

		MicroProfileAssert.saveFile(PsiMicroProfileProject.APPLICATION_YAML_FILE, "greeting:\n" + "  message: message from yaml",
				javaProject);

		// fallback to application.properties
		info = getActualHover(new Position(26, 33));
		MicroProfileAssert.assertHover("greeting.number", "100", 26, 28, 43, info);
	}

	private Hover getActualHover(Position hoverPosition) {
		MicroProfileJavaHoverParams params = new MicroProfileJavaHoverParams();
		params.setDocumentFormat(DocumentFormat.Markdown);
		params.setPosition(hoverPosition);
		params.setUri(javaFileUri);

		return PropertiesManagerForJava.getInstance().hover(params, PsiUtilsImpl.getInstance());
	}
}
