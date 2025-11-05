/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.template;


import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.testFramework.IndexingTestUtil;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.qute.psi.QuteMavenModuleImportingTestCase;
import com.redhat.devtools.intellij.qute.psi.QuteMavenProjectName;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForTemplate;
import com.redhat.qute.commons.GenerateMissingJavaMemberParams;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Integration tests for QuteSupportForTemplateGenerateMissingJavaMember
 * 
 * Invokes the code in JDT to generate the WorkspaceEditd for the CodeActions
 * directly instead of going through the Qute language server.
 * 
 * @author datho7561
 */
public class TemplateGenerateMissingJavaMemberTest extends QuteMavenModuleImportingTestCase {

	@Test
	public void testGenerateField() throws Exception {
		Module project = loadMavenProject(QuteMavenProjectName.qute_quickstart);
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());
		GenerateMissingJavaMemberParams params = new GenerateMissingJavaMemberParams(
				GenerateMissingJavaMemberParams.MemberType.Field, "asdf", "org.acme.qute.Item",
				QuteMavenProjectName.qute_quickstart);
		WorkspaceEdit actual = QuteSupportForTemplate.getInstance().generateMissingJavaMember(params, getJDTUtils(),
				new EmptyProgressIndicator());
		WorkspaceEdit expected = we(Either.forLeft(tde(project, "src/main/java/org/acme/qute/Item.java",
				te(0, 0, 39, 1, "package org.acme.qute;\n\nimport java.math.BigDecimal;\n\npublic class Item {\n\n    /**\n     * The name of the item\n     */\n    public final String name;\n\n    public final BigDecimal price;\n\n    private final int identifier = 0, version = 1;\n\n    private double volume;\n    public String asdf;\n\n    public Item(BigDecimal price, String name) {\n        this.price = price;\n        this.name = name;\n    }\n\n    /**\n     * Returns the derived items.\n     *\n     * @return the derived items\n     */\n    public Item[] getDerivedItems() {\n        return null;\n    }\n\n    public String varArgsMethod(int index, String... elements) {\n        return null;\n    }\n\n    public static BigDecimal staticMethod(Item item) {\n        return item.price.multiply(new BigDecimal(\"0.9\"));\n    }\n\n}"))));
		assertWorkspaceEdit(expected, actual);

	}

	@Test
	public void testUpdateVisibilityOfFieldSimple() throws Exception {
		Module project = loadMavenProject(QuteMavenProjectName.qute_quickstart);
		GenerateMissingJavaMemberParams params = new GenerateMissingJavaMemberParams(
				GenerateMissingJavaMemberParams.MemberType.Field, "volume", "org.acme.qute.Item",
				QuteMavenProjectName.qute_quickstart);
		WorkspaceEdit actual = QuteSupportForTemplate.getInstance().generateMissingJavaMember(params, getJDTUtils(),
				new EmptyProgressIndicator());
		WorkspaceEdit expected = we(
				Either.forLeft(tde(project, "src/main/java/org/acme/qute/Item.java", te(0, 0, 39, 1, "package org.acme.qute;\n\nimport java.math.BigDecimal;\n\npublic class Item {\n\n    /**\n     * The name of the item\n     */\n    public final String name;\n\n    public final BigDecimal price;\n\n    private final int identifier = 0, version = 1;\n\n    public double volume;\n\n    public Item(BigDecimal price, String name) {\n        this.price = price;\n        this.name = name;\n    }\n\n    /**\n     * Returns the derived items.\n     *\n     * @return the derived items\n     */\n    public Item[] getDerivedItems() {\n        return null;\n    }\n\n    public String varArgsMethod(int index, String... elements) {\n        return null;\n    }\n\n    public static BigDecimal staticMethod(Item item) {\n        return item.price.multiply(new BigDecimal(\"0.9\"));\n    }\n\n}"))));
		assertWorkspaceEdit(expected, actual);

	}

	@Test
	public void testUpdateVisibilityOfFieldComplex() throws Exception {
		Module project = loadMavenProject(QuteMavenProjectName.qute_quickstart);
		GenerateMissingJavaMemberParams params = new GenerateMissingJavaMemberParams(
				GenerateMissingJavaMemberParams.MemberType.Field, "identifier", "org.acme.qute.Item",
				QuteMavenProjectName.qute_quickstart);
		WorkspaceEdit actual = QuteSupportForTemplate.getInstance().generateMissingJavaMember(params, getJDTUtils(),
				new EmptyProgressIndicator());
		WorkspaceEdit expected = we(Either.forLeft(tde(project, "src/main/java/org/acme/qute/Item.java",
				te(0, 0, 39, 1, //
						"package org.acme.qute;\n\nimport java.math.BigDecimal;\n\npublic class Item {\n\n    /**\n     * The name of the item\n     */\n    public final String name;\n\n    public final BigDecimal price;\n\n    public final int identifier = 0;\n    private final int version = 1;\n\n    private double volume;\n\n    public Item(BigDecimal price, String name) {\n        this.price = price;\n        this.name = name;\n    }\n\n    /**\n     * Returns the derived items.\n     *\n     * @return the derived items\n     */\n    public Item[] getDerivedItems() {\n        return null;\n    }\n\n    public String varArgsMethod(int index, String... elements) {\n        return null;\n    }\n\n    public static BigDecimal staticMethod(Item item) {\n        return item.price.multiply(new BigDecimal(\"0.9\"));\n    }\n\n}"))));
		assertWorkspaceEdit(expected, actual);

	}

	@Test
	public void testGenerateGetterWithNoMatchingField() throws Exception {
		Module project = loadMavenProject(QuteMavenProjectName.qute_quickstart);
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());
		GenerateMissingJavaMemberParams params = new GenerateMissingJavaMemberParams(
				GenerateMissingJavaMemberParams.MemberType.Getter, "asdf", "org.acme.qute.Item",
				QuteMavenProjectName.qute_quickstart);
		WorkspaceEdit actual = QuteSupportForTemplate.getInstance().generateMissingJavaMember(params, getJDTUtils(),
				new EmptyProgressIndicator());
		WorkspaceEdit expected = we(Either.forLeft(tde(project, "src/main/java/org/acme/qute/Item.java",
				te(0, 0, 39, 1, "package org.acme.qute;\n\nimport java.math.BigDecimal;\n\npublic class Item {\n\n    /**\n     * The name of the item\n     */\n    public final String name;\n\n    public final BigDecimal price;\n\n    private final int identifier = 0, version = 1;\n\n    private double volume;\n\n    public Item(BigDecimal price, String name) {\n        this.price = price;\n        this.name = name;\n    }\n\n    /**\n     * Returns the derived items.\n     *\n     * @return the derived items\n     */\n    public Item[] getDerivedItems() {\n        return null;\n    }\n\n    public String varArgsMethod(int index, String... elements) {\n        return null;\n    }\n\n    public static BigDecimal staticMethod(Item item) {\n        return item.price.multiply(new BigDecimal(\"0.9\"));\n    }\n\n    public String getAsdf() {\n        return null;\n    }\n}"))));
		assertWorkspaceEdit(expected, actual);

	}

	@Test
	public void testGenerateGetterWithMatchingField() throws Exception {
		Module project = loadMavenProject(QuteMavenProjectName.qute_quickstart);
		GenerateMissingJavaMemberParams params = new GenerateMissingJavaMemberParams(
				GenerateMissingJavaMemberParams.MemberType.Getter, "identifier", "org.acme.qute.Item",
				QuteMavenProjectName.qute_quickstart);
		WorkspaceEdit actual = QuteSupportForTemplate.getInstance().generateMissingJavaMember(params, getJDTUtils(),
				new EmptyProgressIndicator());
		WorkspaceEdit expected = we(Either.forLeft(tde(project, "src/main/java/org/acme/qute/Item.java",
				te(0, 0, 39, 1, //
						"package org.acme.qute;\n\nimport java.math.BigDecimal;\n\npublic class Item {\n\n    /**\n     * The name of the item\n     */\n    public final String name;\n\n    public final BigDecimal price;\n\n    private final int identifier = 0, version = 1;\n\n    private double volume;\n\n    public Item(BigDecimal price, String name) {\n        this.price = price;\n        this.name = name;\n    }\n\n    /**\n     * Returns the derived items.\n     *\n     * @return the derived items\n     */\n    public Item[] getDerivedItems() {\n        return null;\n    }\n\n    public String varArgsMethod(int index, String... elements) {\n        return null;\n    }\n\n    public static BigDecimal staticMethod(Item item) {\n        return item.price.multiply(new BigDecimal(\"0.9\"));\n    }\n\n    public int getIdentifier() {\n        return identifier;\n    }\n}"))));
		assertWorkspaceEdit(expected, actual);

	}

	@Test
	public void testGenerateTemplateExtensionInNewClass() throws Exception {

		String sep = System.lineSeparator();

		Module project = loadMavenProject(QuteMavenProjectName.qute_quickstart);
		GenerateMissingJavaMemberParams params = new GenerateMissingJavaMemberParams(
				GenerateMissingJavaMemberParams.MemberType.CreateTemplateExtension, "asdf", "org.acme.qute.Item",
				QuteMavenProjectName.qute_quickstart);
		WorkspaceEdit actual = QuteSupportForTemplate.getInstance().generateMissingJavaMember(params, getJDTUtils(),
				new EmptyProgressIndicator());
		WorkspaceEdit expected = we(Either.forRight(createOp(project, "src/main/java/TemplateExtensions.java")),
				Either.forLeft(tde(project, "src/main/java/TemplateExtensions.java", te(0, 0, 0, 0, //
						"@io.quarkus.qute.TemplateExtension" + sep + "public class TemplateExtensions {" + sep + "	public static String asdf(org.acme.qute.Item item) {" + sep + "		return null;" + sep + "	}" + sep + "}" + sep + ""))));
		assertWorkspaceEdit(expected, actual);
	}

	@Test
	public void testGenerateTemplateExtensionInExistingClass() throws Exception {
		Module project = loadMavenProject(QuteMavenProjectName.qute_quickstart);
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());
		GenerateMissingJavaMemberParams params = new GenerateMissingJavaMemberParams(
				GenerateMissingJavaMemberParams.MemberType.AppendTemplateExtension, "asdf", "org.acme.qute.Item",
				QuteMavenProjectName.qute_quickstart, "org.acme.qute.MyTemplateExtensions");
		WorkspaceEdit actual = QuteSupportForTemplate.getInstance().generateMissingJavaMember(params, getJDTUtils(),
				new EmptyProgressIndicator());
		WorkspaceEdit expected = we(Either.forLeft(tde(project, "src/main/java/org/acme/qute/MyTemplateExtensions.java",
				te(0, 0, 8, 0, //
						"package org.acme.qute;\n\nimport io.quarkus.qute.TemplateExtension;\n\n@TemplateExtension\npublic class MyTemplateExtensions {\n\n    public static String asdf(Item item) {\n        return null;\n    }\n}\n"))));
		assertWorkspaceEdit(expected, actual);
	}

	// ------------------- WorkspaceEdit assert

	public static void assertWorkspaceEdit(WorkspaceEdit expected, WorkspaceEdit actual) {
		if (expected == null) {
			Assert.assertNull(actual);
			return;
		} else {
			Assert.assertNotNull(actual);
		}
		Assert.assertEquals(expected.getDocumentChanges().size(), actual.getDocumentChanges().size());
		for (int i = 0; i < expected.getDocumentChanges().size(); i++) {
			Assert.assertEquals(expected.getDocumentChanges().get(i), actual.getDocumentChanges().get(i));
		}
	}

	public static WorkspaceEdit we(Either<TextDocumentEdit, ResourceOperation>... documentChanges) {
		return new WorkspaceEdit(Arrays.asList(documentChanges));
	}

	// ------------------- edits constants

	private static Pattern FILE_PREFIX_PATTERN = Pattern.compile("file:/(?!//)");

	// ------------------- ResourceOperation assert

	private static ResourceOperation createOp(Module project, String projectFile) {
		String brokenLocationUri = QuarkusModuleUtil.getModuleDirPath(project).getUrl();
		/*Matcher m = FILE_PREFIX_PATTERN.matcher(brokenLocationUri);
		String fixedLocationUri = m.replaceFirst("file:///");*/
		String fixedLocationUri = VfsUtil.toUri(QuarkusModuleUtil.getModuleDirPath(project).getUrl()).toString();
		return new CreateFile(fixedLocationUri + "/" + projectFile);
	}

	// ------------------- TextDocumentEdit assert

	public static TextDocumentEdit tde(Module project, String projectFile, TextEdit... te) {
		/*String brokenLocationUri = QuarkusModuleUtil.getModuleDirPath(project).getUrl();
		Matcher m = FILE_PREFIX_PATTERN.matcher(brokenLocationUri);
		String fixedLocationUri = m.replaceFirst("file:///");*/
		String fixedLocationUri = VfsUtil.toUri(QuarkusModuleUtil.getModuleDirPath(project).getUrl()).toString();
		return tde(fixedLocationUri + "/" + projectFile, 0, te);
	}

	public static TextDocumentEdit tde(String uri, int version, TextEdit... te) {
		VersionedTextDocumentIdentifier versionedTextDocumentIdentifier = new VersionedTextDocumentIdentifier(uri,
				version);
		return new TextDocumentEdit(versionedTextDocumentIdentifier, Arrays.asList(te));
	}

	// ------------------- TextEdit assert

	public static TextEdit te(int startLine, int startCharacter, int endLine, int endCharacter, String newText) {
		TextEdit textEdit = new TextEdit();
		textEdit.setNewText(newText);
		textEdit.setRange(r(startLine, startCharacter, endLine, endCharacter));
		return textEdit;
	}

	// ------------------- range utils
	public static Range r(int startLine, int startCharacter, int endLine, int endCharacter) {
		return new Range(new Position(startLine, startCharacter), new Position(endLine, endCharacter));
	}
}
