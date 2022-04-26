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
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.Test;

import com.redhat.qute.commons.QuteJavaDocumentLinkParams;

/**
 * Tests for Qute @CheckedTemplate support document link inside Java files.
 * 
 * @author Angelo ZERR
 *
 */
public class JavaDocumentLinkTest extends MavenModuleImportingTestCase {

	private static final Logger LOGGER = Logger.getLogger(JavaDocumentLinkTest.class.getSimpleName());
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
		// Template hello;
		//
		// Template goodbye;
		//
		// @Location("detail/items2_v1.html")
		// Template hallo;

		QuteJavaDocumentLinkParams params = new QuteJavaDocumentLinkParams();
		VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/java/org/acme/qute/HelloResource.java");
		params.setUri(VfsUtilCore.virtualToIoFile(javaFile).toURI().toString());

		List<DocumentLink> links = QuteSupportForJava.getInstance().documentLink(params, PsiUtilsLSImpl.getInstance(myProject),
				new EmptyProgressIndicator());
		assertEquals(3, links.size());

		String helloTemplateUri = new File(ModuleUtilCore.getModuleDirPath(module),"src/main/resources/templates/hello.qute.html").toURI().toString();
		String goodbyeTemplateUri = new File(ModuleUtilCore.getModuleDirPath(module),"src/main/resources/templates/hello2.qute.html").toURI().toString();
		String halloTemplateUri = new File(ModuleUtilCore.getModuleDirPath(module),"src/main/resources/templates/detail/items2_v1.html").toURI().toString();

		
		assertDocumentLink(links, //
				dl(r(17, 10, 17, 15), //
						helloTemplateUri, "Open `src/main/resources/templates/hello.qute.html`"),
				dl(r(20, 10, 20, 17), //
						goodbyeTemplateUri, "Open `src/main/resources/templates/goodbye.qute.html`"), //
				dl(r(24, 10, 24, 15), //
						halloTemplateUri, "Open `src/main/resources/templates/detail/items2_v1.html`"));
	}

	@Test
	public void testcheckedTemplate() throws Exception {
		// @CheckedTemplate
		// public class Templates {
		//
		// public static native TemplateInstance hello2(String name);
		//
		// public static native TemplateInstance hello3(String name);
		QuteJavaDocumentLinkParams params = new QuteJavaDocumentLinkParams();
		VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/java/org/acme/qute/Templates.java");
		params.setUri(VfsUtilCore.virtualToIoFile(javaFile).toURI().toString());

		List<DocumentLink> links = QuteSupportForJava.getInstance().documentLink(params, PsiUtilsLSImpl.getInstance(myProject),
				new EmptyProgressIndicator());
		assertEquals(2, links.size());

		String hello2FileUri = new File(ModuleUtilCore.getModuleDirPath(module),"src/main/resources/templates/hello2.qute.html").toURI().toString();
		String hello3FileUri1 = new File(ModuleUtilCore.getModuleDirPath(module),"src/main/resources/templates/hello3.qute.html").toURI().toString();


		assertDocumentLink(links, //
				dl(r(8, 39, 8, 45), //
						hello2FileUri, "Open `src/main/resources/templates/hello2.qute.html`"), //
				dl(r(9, 42, 9, 48), //
						hello3FileUri1, "Create `src/main/resources/templates/hello3.qute.html`"));
	}

	@Test
	public void testcheckedTemplateInInnerClass() throws Exception {
		// public class ItemResource {
		// @CheckedTemplate
		// static class Templates {
		//
		// static native TemplateInstance items(List<Item> items);

		// static class Templates2 {
		//
		// static native TemplateInstance items2(List<Item> items);

		QuteJavaDocumentLinkParams params = new QuteJavaDocumentLinkParams();
		VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/java/org/acme/qute/ItemResource.java");
		params.setUri(VfsUtilCore.virtualToIoFile(javaFile).toURI().toString());

		List<DocumentLink> links = QuteSupportForJava.getInstance().documentLink(params, PsiUtilsLSImpl.getInstance(myProject),
				new EmptyProgressIndicator());
		assertEquals(2, links.size());

		String templateFileUri = new File(ModuleUtilCore.getModuleDirPath(module),"src/main/resources/templates/ItemResource/items.qute.html").toURI().toString();

		assertDocumentLink(links, //
				dl(r(20, 33, 20, 38), //
						templateFileUri, "Open `src/main/resources/templates/ItemResource/items.qute.html`"), //
				dl(r(25, 33, 25, 39), //
						templateFileUri, "Create `src/main/resources/templates/ItemResource/items2.qute.html`"));
	}

	public static Range r(int line, int startChar, int endChar) {
		return r(line, startChar, line, endChar);
	}

	public static Range r(int startLine, int startChar, int endLine, int endChar) {
		Position start = new Position(startLine, startChar);
		Position end = new Position(endLine, endChar);
		return new Range(start, end);
	}

	public static DocumentLink dl(Range range, String target, String tooltip) {
		return new DocumentLink(range, target, null, tooltip);
	}

	public static void assertDocumentLink(List<DocumentLink> actual, DocumentLink... expected) {
		assertEquals(expected.length, actual.size());
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i].getRange(), actual.get(i).getRange());
			assertEquals(expected[i].getData(), actual.get(i).getData());
		}
	}

}
