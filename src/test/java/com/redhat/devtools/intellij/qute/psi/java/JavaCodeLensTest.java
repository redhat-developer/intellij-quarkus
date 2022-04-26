/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.java;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.qute.psi.QuteMavenProjectName;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForJava;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.Test;

import com.redhat.qute.commons.QuteJavaCodeLensParams;

/**
 * Tests for Qute @CheckedTemplate support code lens inside Java files.
 *  
 * @author Angelo ZERR
 *
 */
public class JavaCodeLensTest extends MavenModuleImportingTestCase {

	private static final Logger LOGGER = Logger.getLogger(JavaCodeLensTest.class.getSimpleName());
	private static Level oldLevel;

	private Module module;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		module = createMavenModule(QuteMavenProjectName.qute_quickstart, new File("projects/qute/projects/maven/" + QuteMavenProjectName.qute_quickstart));
	}

	@Test
	public void testtemplateField() throws Exception {
		// public class HelloResource {
		
		// [Open `src/main/resources/templates/hello.qute.html`]
		// Template hello;
		
		// [Create `src/main/resources/templates/goodbye.qute.html`]
		// Template goodbye;
		
		// [Create `src/main/resources/templates/detail/items2_v1.html`]
		// @Location("detail/items2_v1.html")
		// Template hallo;

		QuteJavaCodeLensParams params = new QuteJavaCodeLensParams();
		VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/java/org/acme/qute/HelloResource.java");
		params.setUri(VfsUtilCore.virtualToIoFile(javaFile).toURI().toString());

		List<? extends CodeLens> lenses = QuteSupportForJava.getInstance().codeLens(params, PsiUtilsLSImpl.getInstance(myProject),
				new EmptyProgressIndicator());
		assertEquals(3, lenses.size());

		String helloTemplateFileUri = new File(ModuleUtilCore.getModuleDirPath(module),"src/main/resources/templates/hello.qute.html").toURI().toString();
		String goodbyeTemplateFileUri = new File(ModuleUtilCore.getModuleDirPath(module),"src/main/resources/templates/goodbye.qute.html").toURI().toString();
		String halloTemplateFileUri = new File(ModuleUtilCore.getModuleDirPath(module),"src/main/resources/templates/detail/items2_v1.html").toURI().toString();
		
		assertCodeLens(lenses, //
				cl(r(16, 1, 17, 16), //
						"Open `src/main/resources/templates/hello.qute.html`", //
						"qute.command.open.uri", Arrays.asList(helloTemplateFileUri)), //
				cl(r(19, 1, 20, 18), //
						"Create `src/main/resources/templates/goodbye.html`", //
						"qute.command.generate.template.file", Arrays.asList(goodbyeTemplateFileUri)), //
				cl(r(22, 1, 24, 16), //
						"Create `src/main/resources/templates/detail/items2_v1.html`", //
						"qute.command.generate.template.file", Arrays.asList(halloTemplateFileUri)));
	}

	@Test
	public void testcheckedTemplate() throws Exception {
		// @CheckedTemplate
		// public class Templates {
		// [Open `src/main/resources/templates/hello2.qute.html`]
		// public static native TemplateInstance hello2(String name);
		// [Open `src/main/resources/templates/hello3.qute.html`]
		// public static native TemplateInstance hello3(String name);
		QuteJavaCodeLensParams params = new QuteJavaCodeLensParams();
		VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/java/org/acme/qute/Templates.java");
		params.setUri(VfsUtilCore.virtualToIoFile(javaFile).toURI().toString());

		List<? extends CodeLens> lenses = QuteSupportForJava.getInstance().codeLens(params, PsiUtilsLSImpl.getInstance(myProject),
				new EmptyProgressIndicator());
		assertEquals(2, lenses.size());

		String goodbyeFileUri = new File(ModuleUtilCore.getModuleDirPath(module),"src/main/resources/templates/hello2.qute.html").toURI().toString();
		String hello3FileUri1 = new File(ModuleUtilCore.getModuleDirPath(module), "src/main/resources/templates/hello3.qute.html").toURI().toString();

		assertCodeLens(lenses, //
				cl(r(8, 1, 8, 59), //
						"Open `src/main/resources/templates/hello2.qute.html`", //
						"qute.command.open.uri", Arrays.asList(goodbyeFileUri)), //
				cl(r(9, 4, 9, 62), //
						"Create `src/main/resources/templates/hello3.html`", //
						"qute.command.generate.template.file", Arrays.asList(hello3FileUri1)));
	}

	@Test
	public void testcheckedTemplateInInnerClass() throws Exception {
		// public class ItemResource {
		// @CheckedTemplate
		// static class Templates {
		// [Open `src/main/resources/templates/ItemResource/items.qute.html`]
		// static native TemplateInstance items(List<Item> items);

		// static class Templates2 {
		// [Create `src/main/resources/templates/ItemResource/items2.qute.html`]
		// static native TemplateInstance items2(List<Item> items);

		QuteJavaCodeLensParams params = new QuteJavaCodeLensParams();
		VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/java/org/acme/qute/ItemResource.java");
		params.setUri(VfsUtilCore.virtualToIoFile(javaFile).toURI().toString());

		List<? extends CodeLens> lenses = QuteSupportForJava.getInstance().codeLens(params, PsiUtilsLSImpl.getInstance(myProject),
				new EmptyProgressIndicator());
		assertEquals(2, lenses.size());

		String templateFileUri = VfsUtilCore.virtualToIoFile(LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/resources/templates/ItemResource/items.qute.html")).toURI().toString();

		assertCodeLens(lenses, //
				cl(r(20, 2, 20, 57), //
						"Open `src/main/resources/templates/ItemResource/items.qute.html`", //
						"qute.command.open.uri", Arrays.asList(templateFileUri)), //
				cl(r(25, 2, 25, 58), //
						"Create `src/main/resources/templates/ItemResource/items2.html`", //
						"qute.command.generate.template.file", Arrays.asList(templateFileUri)));
	}

	public static Range r(int line, int startChar, int endChar) {
		return r(line, startChar, line, endChar);
	}

	public static Range r(int startLine, int startChar, int endLine, int endChar) {
		Position start = new Position(startLine, startChar);
		Position end = new Position(endLine, endChar);
		return new Range(start, end);
	}

	public static CodeLens cl(Range range, String title, String command, List<Object> arguments) {
		return new CodeLens(range, new Command(title, command, arguments), null);
	}

	public static void assertCodeLens(List<? extends CodeLens> actual, CodeLens... expected) {
		assertEquals(expected.length, actual.size());
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i].getRange(), actual.get(i).getRange());
			Command expectedCommand = expected[i].getCommand();
			Command actualCommand = actual.get(i).getCommand();
			if (expectedCommand != null && actualCommand != null) {
				assertEquals(expectedCommand.getTitle(), actualCommand.getTitle());
				assertEquals(expectedCommand.getCommand(), actualCommand.getCommand());
			}
			assertEquals(expected[i].getData(), actual.get(i).getData());
		}
	}

}
