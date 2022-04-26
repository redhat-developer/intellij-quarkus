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
import com.redhat.devtools.intellij.qute.psi.internal.java.QuteErrorCode;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.junit.Test;

import com.redhat.qute.commons.QuteJavaDiagnosticsParams;

/**
 * Tests for Qute @CheckedTemplate support validation inside Java files.
 * 
 * @author Angelo ZERR
 *
 */
public class JavaDiagnosticsTest extends MavenModuleImportingTestCase {

	private static final Logger LOGGER = Logger.getLogger(JavaDiagnosticsTest.class.getSimpleName());
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

		QuteJavaDiagnosticsParams params = new QuteJavaDiagnosticsParams();
		VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/java/org/acme/qute/HelloResource.java");
		params.setUris(Arrays.asList(VfsUtilCore.virtualToIoFile(javaFile).toURI().toString()));

		List<PublishDiagnosticsParams> publishDiagnostics = QuteSupportForJava.getInstance().diagnostics(params,
				PsiUtilsLSImpl.getInstance(myProject), new EmptyProgressIndicator());
		assertEquals(1, publishDiagnostics.size());

		List<Diagnostic> diagnostics = publishDiagnostics.get(0).getDiagnostics();
		assertEquals(2, diagnostics.size());

		assertDiagnostic(diagnostics, //
				new Diagnostic(r(20, 10, 20, 17),
						"No template matching the path goodbye could be found for: org.acme.qute.HelloResource",
						DiagnosticSeverity.Error, "qute", QuteErrorCode.NoMatchingTemplate.name()), //
				new Diagnostic(r(24, 10, 24, 15),
						"No template matching the path detail/items2_v1.html could be found for: org.acme.qute.HelloResource",
						DiagnosticSeverity.Error, "qute", QuteErrorCode.NoMatchingTemplate.name()));
	}

	@Test
	public void testcheckedTemplate() throws Exception {
		// @CheckedTemplate
		// public class Templates {
		//
		// public static native TemplateInstance hello2(String name);
		//
		// public static native TemplateInstance hello3(String name);
		QuteJavaDiagnosticsParams params = new QuteJavaDiagnosticsParams();
		VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/java/org/acme/qute/Templates.java");
		params.setUris(Arrays.asList(VfsUtilCore.virtualToIoFile(javaFile).toURI().toString()));

		List<PublishDiagnosticsParams> publishDiagnostics = QuteSupportForJava.getInstance().diagnostics(params,
				PsiUtilsLSImpl.getInstance(myProject), new EmptyProgressIndicator());
		assertEquals(1, publishDiagnostics.size());

		List<Diagnostic> diagnostics = publishDiagnostics.get(0).getDiagnostics();
		assertEquals(1, diagnostics.size());

		assertDiagnostic(diagnostics, //
				new Diagnostic(r(9, 42, 9, 48),
						"No template matching the path hello3 could be found for: org.acme.qute.Templates",
						DiagnosticSeverity.Error, "qute", QuteErrorCode.NoMatchingTemplate.name()));
	}

	@Test
	public void testcheckedTemplateInInnerClass() throws Exception {
		// public class ItemResource {
		// @CheckedTemplate
		// static class Templates {
		// [Open `src/main/resources/templates/ItemResource/items.qute.html`]
		// static native TemplateInstance items(List<Item> items);

		QuteJavaDiagnosticsParams params = new QuteJavaDiagnosticsParams();
		VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/java/org/acme/qute/ItemResource.java");
		params.setUris(Arrays.asList(VfsUtilCore.virtualToIoFile(javaFile).toURI().toString()));

		List<PublishDiagnosticsParams> publishDiagnostics = QuteSupportForJava.getInstance().diagnostics(params,
				PsiUtilsLSImpl.getInstance(myProject), new EmptyProgressIndicator());
		assertEquals(1, publishDiagnostics.size());

		List<Diagnostic> diagnostics = publishDiagnostics.get(0).getDiagnostics();
		assertEquals(1, diagnostics.size());

		assertDiagnostic(diagnostics, //
				new Diagnostic(r(25, 33, 25, 39),
						"No template matching the path ItemResource/items2 could be found for: org.acme.qute.ItemResource$Templates2",
						DiagnosticSeverity.Error, "qute", QuteErrorCode.NoMatchingTemplate.name()));
	}

	public static Range r(int line, int startChar, int endChar) {
		return r(line, startChar, line, endChar);
	}

	public static Range r(int startLine, int startChar, int endLine, int endChar) {
		Position start = new Position(startLine, startChar);
		Position end = new Position(endLine, endChar);
		return new Range(start, end);
	}

	public static Diagnostic d(Range range, String message, String source, DiagnosticSeverity severity) {
		return new Diagnostic(range, message, severity, "qute");
	}

	public static void assertDiagnostic(List<? extends Diagnostic> actual, Diagnostic... expected) {
		assertEquals(expected.length, actual.size());
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i].getRange(), actual.get(i).getRange());
			assertEquals(expected[i].getMessage(), actual.get(i).getMessage());
			assertEquals(expected[i].getData(), actual.get(i).getData());
		}
	}

}
