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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.qute.psi.QuteMavenModuleImportingTestCase;
import com.redhat.devtools.intellij.qute.psi.QuteMavenProjectName;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForJava;
import com.redhat.qute.commons.QuteJavaCodeLensParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tests for Qute @CheckedTemplate support code lens inside Java files.
 *
 * @author Angelo ZERR
 */
public class MavenJavaCodeLensTest extends QuteMavenModuleImportingTestCase {

    private static final Logger LOGGER = Logger.getLogger(MavenJavaCodeLensTest.class.getSimpleName());
    private static Level oldLevel;

    private Module module;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        module = loadMavenProject(QuteMavenProjectName.qute_quickstart);
    }

    @Test
    public void testTemplateField() throws Exception {
        // public class HelloResource {

        // [Open `src/main/resources/templates/hello.qute.html`]
        // Template hello;

        // [Create `src/main/resources/templates/goodbye.qute.html`]
        // Template goodbye;

        // [Create `src/main/resources/templates/detail/items2_v1.html`]
        // @Location("detail/items2_v1.html")
        // Template hallo;
        //
        // [Open `src/main/resources/templates/detail/page1.html`]
        // Template bonjour;
        //
        // [Create `src/main/resources/templates/detail/page2.html`]
        // Template aurevoir;
        //
        // public HelloResource(@Location("detail/page1.html") Template page1,
        // @Location("detail/page2.html") Template page2) {
        // this.bonjour = page1;
        // this.aurevoir = requireNonNull(page2, "page is required");
        // }

        QuteJavaCodeLensParams params = new QuteJavaCodeLensParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/HelloResource.java").toASCIIString();
        params.setUri(javaFileUri);

        List<? extends CodeLens> lenses = QuteSupportForJava.getInstance().codeLens(params, PsiUtilsLSImpl.getInstance(myProject),
                new EmptyProgressIndicator());
        assertEquals(5, lenses.size());

        String helloTemplateFileUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/hello.qute.html").toASCIIString();
        String goodbyeTemplateFileUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/goodbye.qute.html").toASCIIString();
        String halloTemplateFileUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/detail/items2_v1.html").toASCIIString();
        String bonjourTemplateFileUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/detail/page1.html").toASCIIString();
        String aurevoirTemplateFileUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/detail/page2.html").toASCIIString();

        assertCodeLens(lenses, //
                cl(r(16, 1, 17, 16), //
                        "Open `src/main/resources/templates/hello.qute.html`", //
                        "qute.command.open.uri", Arrays.asList(helloTemplateFileUri)), //
                cl(r(19, 1, 20, 18), //
                        "Create `src/main/resources/templates/goodbye.html`", //
                        "qute.command.generate.template.file", Arrays.asList(goodbyeTemplateFileUri)), //
                cl(r(22, 1, 24, 16), //
                        "Create `src/main/resources/templates/detail/items2_v1.html`", //
                        "qute.command.generate.template.file", Arrays.asList(halloTemplateFileUri)), //
                cl(r(26, 1, 27, 18), //
                        "Open `src/main/resources/templates/detail/page1.html`", //
                        "qute.command.open.uri", Arrays.asList(bonjourTemplateFileUri)), //
                cl(r(29, 1, 30, 19), //
                        "Create `src/main/resources/templates/detail/page2.html`", //
                        "qute.command.generate.template.file", Arrays.asList(aurevoirTemplateFileUri)));
    }

    @Test
    public void testCheckedTemplate() throws Exception {
        // @CheckedTemplate
        // public class Templates {
        // [Open `src/main/resources/templates/hello2.qute.html`]
        // public static native TemplateInstance hello2(String name);
        // [Open `src/main/resources/templates/hello3.qute.html`]
        // public static native TemplateInstance hello3(String name);
        QuteJavaCodeLensParams params = new QuteJavaCodeLensParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/Templates.java").toASCIIString();
        params.setUri(javaFileUri);

        List<? extends CodeLens> lenses = QuteSupportForJava.getInstance().codeLens(params, PsiUtilsLSImpl.getInstance(myProject),
                new EmptyProgressIndicator());
        assertEquals(2, lenses.size());

        String goodbyeFileUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/hello2.qute.html").toASCIIString();
        String hello3FileUri1 = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/hello3.qute.html").toASCIIString();

        assertCodeLens(lenses, //
                cl(r(8, 1, 8, 59), //
                        "Open `src/main/resources/templates/hello2.qute.html`", //
                        "qute.command.open.uri", Arrays.asList(goodbyeFileUri)), //
                cl(r(9, 4, 9, 62), //
                        "Create `src/main/resources/templates/hello3.html`", //
                        "qute.command.generate.template.file", Arrays.asList(hello3FileUri1)));
    }

    @Test
    public void testCheckedTemplateInInnerClass() throws Exception {
        // public class ItemResource {
        // @CheckedTemplate
        // static class Templates {
        // [Open `src/main/resources/templates/ItemResource/items.qute.html`]
        // static native TemplateInstance items(List<Item> items);

        // static class Templates2 {
        // [Create `src/main/resources/templates/ItemResource/items2.qute.html`]
        // static native TemplateInstance items2(List<Item> items);

        QuteJavaCodeLensParams params = new QuteJavaCodeLensParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/ItemResource.java").toASCIIString();
        params.setUri(javaFileUri);

        List<? extends CodeLens> lenses = QuteSupportForJava.getInstance().codeLens(params, PsiUtilsLSImpl.getInstance(myProject),
                new EmptyProgressIndicator());
        assertEquals(3, lenses.size());

        String itemsUri = LSPIJUtils.toUri(module).resolve("/src/main/resources/templates/ItemResource/items.qute.html").toASCIIString();
        String mapUri = LSPIJUtils.toUri(module).resolve("/src/main/resources/templates/ItemResource/map.html").toASCIIString();
        String items2Uri = LSPIJUtils.toUri(module).resolve("/src/main/resources/templates/ItemResource/items2.html").toASCIIString();

        assertCodeLens(lenses, //
                cl(r(21, 2, 21, 57), //
                        "Open `src/main/resources/templates/ItemResource/items.qute.html`", //
                        "qute.command.open.uri", Arrays.asList(itemsUri)), //
                cl(r(23, 2, 23, 102), //
                        "Create `src/main/resources/templates/ItemResource/map.html`", //
                        "qute.command.generate.template.file", Arrays.asList(mapUri)), //
                cl(r(28, 2, 28, 58), //
                        "Create `src/main/resources/templates/ItemResource/items2.html`", //
                        "qute.command.generate.template.file", Arrays.asList(items2Uri)));
    }

    @Test
    public void testCheckedTemplateWithFragment() throws Exception {

        // @CheckedTemplate
        //public class ItemTemplates {
        //
        //    static native TemplateInstance items(List<Item> items);
        //    static native TemplateInstance items$id1(List<Item> items);
        //    static native TemplateInstance items3$id2(List<Item> items);
        //    static native TemplateInstance items3$(List<Item> items);
        //}

        QuteJavaCodeLensParams params = new QuteJavaCodeLensParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/ItemTemplates.java").toASCIIString();
        params.setUri(javaFileUri);

        List<? extends CodeLens> lenses = QuteSupportForJava.getInstance().codeLens(params, PsiUtilsLSImpl.getInstance(myProject),
                new EmptyProgressIndicator());
        assertEquals(3, lenses.size());

        String itemsUri = LSPIJUtils.toUri(module).resolve("/src/main/resources/templates/items.html").toASCIIString();
        String items3Uri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/items3.html").toASCIIString();

        assertCodeLens(lenses, //
                cl(r(10, 4, 10, 59), //
                        "Open `src/main/resources/templates/items.html`", //
                        "qute.command.open.uri", Arrays.asList(itemsUri)), //
                cl(r(11, 4, 11, 63), //
                        "Open `id1` fragment of `src/main/resources/templates/items.html`", //
                        "qute.command.open.uri", Arrays.asList(itemsUri, "id1")), //
                cl(r(12, 4, 12, 64), //
                        "Create `src/main/resources/templates/items3.html`", //
                        "qute.command.generate.template.file", Arrays.asList(items3Uri)));

        // @CheckedTemplate(ignoreFragments = true)
        // public class ItemTemplatesIgnoreFragments {
        //
        //    static native TemplateInstance items2(List<Item> items);
        //    static native TemplateInstance items2$id1(List<Item> items);
        //    static native TemplateInstance items2$id2(List<Item> items);
        //}
        params = new QuteJavaCodeLensParams();
        javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/ItemTemplatesIgnoreFragments.java").toASCIIString();
        params.setUri(javaFileUri);

        lenses = QuteSupportForJava.getInstance().codeLens(params, PsiUtilsLSImpl.getInstance(myProject),
                new EmptyProgressIndicator());
        assertEquals(3, lenses.size());

        String items2Uri = LSPIJUtils.toUri(module).resolve("/src/main/resources/templates/items2.html").toASCIIString();
        String items2Uri_id1 = LSPIJUtils.toUri(module).resolve("/src/main/resources/templates/items2$id1.html").toASCIIString();
        String items2Uri_id2 = LSPIJUtils.toUri(module).resolve("/src/main/resources/templates/items2$id2.html").toASCIIString();

        assertCodeLens(lenses, //
                cl(r(10, 4, 10, 60), //
                        "Open `src/main/resources/templates/items2.html`", //
                        "qute.command.open.uri", Arrays.asList(items2Uri)), //
                cl(r(11, 4, 11, 64), //
                        "Open `src/main/resources/templates/items2$id1.html`", //
                        "qute.command.open.uri", Arrays.asList(items2Uri_id1)), //
                cl(r(12, 4, 12, 64), //
                        "Create `src/main/resources/templates/items2$id2.html`", //
                        "qute.command.generate.template.file", Arrays.asList(items2Uri_id2)));
    }

    public void testCheckedTemplateWithCustomBasePath() throws Exception {

        // @CheckedTemplate(basePath="ItemResourceWithFragment")
        //public class ItemTemplatesCustomBasePath {
        //
        //    static native TemplateInstance items(List<Item> items);
        //    static native TemplateInstance items$id1(List<Item> items);
        //    static native TemplateInstance items3$id2(List<Item> items);
        //    static native TemplateInstance items3$(List<Item> items);
        //}

        QuteJavaCodeLensParams params = new QuteJavaCodeLensParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/ItemTemplatesCustomBasePath.java").toASCIIString();
        params.setUri(javaFileUri);

        List<? extends CodeLens> lenses = QuteSupportForJava.getInstance().codeLens(params, PsiUtilsLSImpl.getInstance(myProject),
                new EmptyProgressIndicator());
        assertEquals(3, lenses.size());

        String itemsUri = LSPIJUtils.toUri(module).resolve("/src/main/resources/templates/ItemResourceWithFragment/items.html").toASCIIString();
        String items3Uri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/ItemResourceWithFragment/items3.html").toASCIIString();

        assertCodeLens(lenses, //
                cl(r(9, 1, 9, 56), //
                        "Open `src/main/resources/templates/ItemResourceWithFragment/items.html`", //
                        "qute.command.open.uri", Arrays.asList(itemsUri)), //
                cl(r(10, 1, 10, 60), //
                        "Open `id1` fragment of `src/main/resources/templates/ItemResourceWithFragment/items.html`", //
                        "qute.command.open.uri", Arrays.asList(itemsUri, "id1")), //
                cl(r(11, 1, 11, 61), //
                        "Create `src/main/resources/templates/ItemResourceWithFragment/items3.html`", //
                        "qute.command.generate.template.file", Arrays.asList(items3Uri)));

    }

    public void testCheckedTemplateInInnerClassWithCustomBasePath() throws Exception {

        // @CheckedTemplate(basePath="ItemResourceWithFragment")
        //public class ItemTemplatesCustomBasePath {
        //
        //    static native TemplateInstance items(List<Item> items);
        //    static native TemplateInstance items$id1(List<Item> items);
        //    static native TemplateInstance items3$id2(List<Item> items);
        //    static native TemplateInstance items3$(List<Item> items);
        //}

        QuteJavaCodeLensParams params = new QuteJavaCodeLensParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/ItemTemplatesCustomBasePath.java").toASCIIString();
        params.setUri(javaFileUri);

        List<? extends CodeLens> lenses = QuteSupportForJava.getInstance().codeLens(params, PsiUtilsLSImpl.getInstance(myProject),
                new EmptyProgressIndicator());
        assertEquals(3, lenses.size());

        String itemsUri = LSPIJUtils.toUri(module).resolve("/src/main/resources/templates/ItemResourceWithFragment/items.html").toASCIIString();
        String items3Uri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/ItemResourceWithFragment/items3.html").toASCIIString();

        assertCodeLens(lenses, //
                cl(r(9, 1, 9, 56), //
                        "Open `src/main/resources/templates/ItemResourceWithFragment/items.html`", //
                        "qute.command.open.uri", Arrays.asList(itemsUri)), //
                cl(r(10, 1, 10, 60), //
                        "Open `id1` fragment of `src/main/resources/templates/ItemResourceWithFragment/items.html`", //
                        "qute.command.open.uri", Arrays.asList(itemsUri, "id1")), //
                cl(r(11, 1, 11, 61), //
                        "Create `src/main/resources/templates/ItemResourceWithFragment/items3.html`", //
                        "qute.command.generate.template.file", Arrays.asList(items3Uri)));

    }

    @Test
    public void testCheckedTemplateInInnerClassWithFragment() throws Exception {

        QuteJavaCodeLensParams params = new QuteJavaCodeLensParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/ItemResourceWithFragment.java").toASCIIString();
        params.setUri(javaFileUri);

        List<? extends CodeLens> lenses = QuteSupportForJava.getInstance().codeLens(params, PsiUtilsLSImpl.getInstance(myProject),
                new EmptyProgressIndicator());
        assertEquals(6, lenses.size());

        String itemsUri = LSPIJUtils.toUri(module).resolve("/src/main/resources/templates/ItemResourceWithFragment/items.html").toASCIIString();
        String items3Uri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/ItemResourceWithFragment/items3.html").toASCIIString();
        String items2Uri = LSPIJUtils.toUri(module).resolve("/src/main/resources/templates/ItemResourceWithFragment/items2.html").toASCIIString();
        String items2Uri_id1 = LSPIJUtils.toUri(module).resolve("/src/main/resources/templates/ItemResourceWithFragment/items2$id1.html").toASCIIString();
        String items2Uri_id2 = LSPIJUtils.toUri(module).resolve("/src/main/resources/templates/ItemResourceWithFragment/items2$id2.html").toASCIIString();

        assertCodeLens(lenses, //
                cl(r(21, 2, 21, 57), //
                        "Open `src/main/resources/templates/ItemResourceWithFragment/items.html`", //
                        "qute.command.open.uri", Arrays.asList(itemsUri)), //
                cl(r(22, 2, 22, 61), //
                        "Open `id1` fragment of `src/main/resources/templates/ItemResourceWithFragment/items.html`", //
                        "qute.command.open.uri", Arrays.asList(itemsUri, "id1")), //
                cl(r(23, 2, 23, 62), //
                        "Create `src/main/resources/templates/ItemResourceWithFragment/items3.html`", //
                        "qute.command.generate.template.file", Arrays.asList(items3Uri)), //

                cl(r(29, 2, 29, 58), //
                        "Open `src/main/resources/templates/ItemResourceWithFragment/items2.html`", //
                        "qute.command.open.uri", Arrays.asList(items2Uri)), //
                cl(r(30, 2, 30, 62), //
                        "Open `src/main/resources/templates/ItemResourceWithFragment/items2$id1.html`", //
                        "qute.command.open.uri", Arrays.asList(items2Uri_id1)), //
                cl(r(31, 2, 31, 62), //
                        "Create `src/main/resources/templates/ItemResourceWithFragment/items2$id2.html`", //
                        "qute.command.generate.template.file", Arrays.asList(items2Uri_id2)));
    }

    @Test
    public void testTemplateRecord() throws Exception {
        // public class HelloResource {

        // record Hello(String name) implements TemplateInstance {}

        // record Bonjour(String name) implements TemplateInstance {}

        // record Status() {}

        // @CheckedTemplate(basePath="Foo", defaultName=CheckedTemplate.HYPHENATED_ELEMENT_NAME)
        // record HelloWorld(String name) implements TemplateInstance {}

        var module = loadMavenProject(QuteMavenProjectName.qute_record);

        QuteJavaCodeLensParams params = new QuteJavaCodeLensParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/sample/HelloResource.java").toASCIIString();
        params.setUri(javaFileUri);

        List<? extends CodeLens> lenses = QuteSupportForJava.getInstance().codeLens(params, PsiUtilsLSImpl.getInstance(myProject),
                new EmptyProgressIndicator());

        String helloFileUri = LSPIJUtils.toUri(module).resolve("/src/main/resources/templates/HelloResource/Hello.html").toASCIIString();
        String bonjourFileUri = LSPIJUtils.toUri(module).resolve("/src/main/resources/templates/HelloResource/Bonjour.html").toASCIIString();
        String helloWorldFileUri = LSPIJUtils.toUri(module).resolve("/src/main/resources/templates/Foo/hello-world.html").toASCIIString();

        assertCodeLens(lenses, //
                cl(r(15, 4, 15, 60), //
                        "Open `src/main/resources/templates/HelloResource/Hello.html`", //
                        "qute.command.open.uri", Arrays.asList(helloFileUri)), //
                cl(r(17, 4, 17, 62), //
                        "Create `src/main/resources/templates/HelloResource/Bonjour.html`", //
                        "qute.command.generate.template.file", Arrays.asList(bonjourFileUri)), //
                cl(r(21, 4, 22, 65), //
                        "Create `src/main/resources/templates/Foo/hello-world.html`", //
                        "qute.command.generate.template.file", Arrays.asList(helloWorldFileUri)));
    }

    @Test
    public void testCheckedTemplateWithDefaultName() throws Exception {
        // @CheckedTemplate(defaultName=CheckedTemplate.HYPHENATED_ELEMENT_NAME)
        // static class Templates {
        // static native TemplateInstance HelloWorld(String name);

        var module = loadMavenProject(QuteMavenProjectName.qute_record);

        QuteJavaCodeLensParams params = new QuteJavaCodeLensParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/sample/ItemResource.java").toASCIIString();
        params.setUri(javaFileUri);

        List<? extends CodeLens> lenses = QuteSupportForJava.getInstance().codeLens(params, PsiUtilsLSImpl.getInstance(myProject),
                new EmptyProgressIndicator());
        assertEquals(3, lenses.size());

        String helloFileUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/ItemResource/HelloWorld.html").toASCIIString();
        String hello2FileUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/ItemResource/hello-world.html").toASCIIString();
        String hello3FileUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/ItemResource/hello_world.html").toASCIIString();

        assertCodeLens(lenses, //
                cl(r(19, 2, 19, 57), //
                        "Create `src/main/resources/templates/ItemResource/HelloWorld.html`", //
                        "qute.command.generate.template.file", Arrays.asList(helloFileUri)), //
                cl(r(25, 2, 25, 57), //
                        "Create `src/main/resources/templates/ItemResource/hello-world.html`", //
                        "qute.command.generate.template.file", Arrays.asList(hello2FileUri)), //
                cl(r(31, 2, 31, 57), //
                        "Create `src/main/resources/templates/ItemResource/hello_world.html`", //
                        "qute.command.generate.template.file", Arrays.asList(hello3FileUri)));
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
