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
import com.redhat.qute.commons.QuteJavaDocumentLinkParams;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.Test;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tests for Qute @CheckedTemplate support document link inside Java files.
 *
 * @author Angelo ZERR
 */
public class MavenJavaDocumentLinkTest extends QuteMavenModuleImportingTestCase {

    private static final Logger LOGGER = Logger.getLogger(MavenJavaDocumentLinkTest.class.getSimpleName());
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
        // Template hello;
        //
        // Template goodbye;
        //
        // @Location("detail/items2_v1.html")
        // Template hallo;

        QuteJavaDocumentLinkParams params = new QuteJavaDocumentLinkParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/HelloResource.java").toASCIIString();
        params.setUri(javaFileUri);

        List<DocumentLink> links = QuteSupportForJava.getInstance().documentLink(params, PsiUtilsLSImpl.getInstance(myProject),
                new EmptyProgressIndicator());
        assertEquals(5, links.size());

        String helloTemplateUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/hello.qute.html").toASCIIString();
        String goodbyeTemplateUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/hello2.qute.html").toASCIIString();
        String halloTemplateUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/detail/items2_v1.html").toASCIIString();
        String bonjourTemplateFileUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/detail/page1.html").toASCIIString();
        String aurevoirTemplateFileUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/detail/page2.html").toASCIIString();

        assertDocumentLink(links, //
                dl(r(17, 10, 17, 15), //
                        helloTemplateUri, "Open `src/main/resources/templates/hello.qute.html`"),
                dl(r(20, 10, 20, 17), //
                        goodbyeTemplateUri, "Open `src/main/resources/templates/goodbye.qute.html`"), //
                dl(r(22, 11, 22, 34), //
                        halloTemplateUri, "Open `src/main/resources/templates/detail/items2_v1.html`"), //
                dl(r(32, 32, 32, 51), //
                        bonjourTemplateFileUri, "Open `src/main/resources/templates/detail/page1.html`"), //
                dl(r(32, 79, 32, 98), //
                        aurevoirTemplateFileUri, "Create `src/main/resources/templates/detail/page2.html`"));
    }

    @Test
    public void testCheckedTemplate() throws Exception {
        // @CheckedTemplate
        // public class Templates {
        //
        // public static native TemplateInstance hello2(String name);
        //
        // public static native TemplateInstance hello3(String name);
        QuteJavaDocumentLinkParams params = new QuteJavaDocumentLinkParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/Templates.java").toASCIIString();
        params.setUri(javaFileUri);

        List<DocumentLink> links = QuteSupportForJava.getInstance().documentLink(params, PsiUtilsLSImpl.getInstance(myProject),
                new EmptyProgressIndicator());
        assertEquals(2, links.size());

        String hello2FileUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/hello2.qute.html").toASCIIString();
        String hello3FileUri1 = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/hello3.qute.html").toASCIIString();

        assertDocumentLink(links, //
                dl(r(8, 39, 8, 45), //
                        hello2FileUri, "Open `src/main/resources/templates/hello2.qute.html`"), //
                dl(r(9, 42, 9, 48), //
                        hello3FileUri1, "Create `src/main/resources/templates/hello3.qute.html`"));
    }

    @Test
    public void testCheckedTemplateInInnerClass() throws Exception {
        // public class ItemResource {
        // @CheckedTemplate
        // static class Templates {
        //
        // static native TemplateInstance items(List<Item> items);

        // static class Templates2 {
        //
        // static native TemplateInstance items2(List<Item> items);

        QuteJavaDocumentLinkParams params = new QuteJavaDocumentLinkParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/ItemResource.java").toASCIIString();
        params.setUri(javaFileUri);

        List<DocumentLink> links = QuteSupportForJava.getInstance().documentLink(params, PsiUtilsLSImpl.getInstance(myProject),
                new EmptyProgressIndicator());
        assertEquals(3, links.size());

        String templateFileUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/ItemResource/items.qute.html").toASCIIString();

        assertDocumentLink(links, //
                dl(r(21, 33, 21, 38), //
                        templateFileUri, "Open `src/main/resources/templates/ItemResource/items.qute.html`"), //
                dl(r(23, 33, 23, 36), //
                        templateFileUri, "Create `src/main/resources/templates/ItemResource/map.qute.html`"), //
                dl(r(28, 33, 28, 39), //
                        templateFileUri, "Create `src/main/resources/templates/ItemResource/items2.qute.html`"));
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

        QuteJavaDocumentLinkParams params = new QuteJavaDocumentLinkParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/ItemTemplates.java").toASCIIString();
        params.setUri(javaFileUri);

        List<DocumentLink> links = QuteSupportForJava.getInstance().documentLink(params, PsiUtilsLSImpl.getInstance(myProject),
                new EmptyProgressIndicator());
        assertEquals(3, links.size());

        String templateFileUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/items.html").toASCIIString();

        assertDocumentLink(links, //
                dl(r(10, 35, 10, 40), //
                        templateFileUri, "Open `src/main/resources/templates/items.html`"), //
                dl(r(11, 35, 11, 44), //
                        templateFileUri, "Open `src/main/resources/templates/items.html`"), //
                dl(r(12, 35, 12, 45), //
                        templateFileUri, "Create `src/main/resources/templates/items3.html`"));

        // @CheckedTemplate(ignoreFragments = true)
        // public class ItemTemplatesIgnoreFragments {
        //
        //    static native TemplateInstance items2(List<Item> items);
        //    static native TemplateInstance items2$id1(List<Item> items);
        //    static native TemplateInstance items2$id2(List<Item> items);
        //}

        params = new QuteJavaDocumentLinkParams();
        javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/ItemTemplatesIgnoreFragments.java").toASCIIString();
        params.setUri(javaFileUri);

        links = QuteSupportForJava.getInstance().documentLink(params, PsiUtilsLSImpl.getInstance(myProject),
                new EmptyProgressIndicator());
        assertEquals(3, links.size());

        templateFileUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/items.html").toASCIIString();

        assertDocumentLink(links, //
                dl(r(10, 35, 10, 41), //
                        templateFileUri, "Open `src/main/resources/templates/items2.html`"), //
                dl(r(11, 35, 11, 45), //
                        templateFileUri,
                        "Open `src/main/resources/templates/items2$id1.html`"), //
                dl(r(12, 35, 12, 45), //
                        templateFileUri,
                        "Open `src/main/resources/templates/items2$id2.html`"));
    }

    @Test
    public void testCheckedTemplateInInnerClassWithFragment() throws Exception {

        QuteJavaDocumentLinkParams params = new QuteJavaDocumentLinkParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/ItemResourceWithFragment.java").toASCIIString();
        params.setUri(javaFileUri);

        List<DocumentLink> links = QuteSupportForJava.getInstance().documentLink(params, PsiUtilsLSImpl.getInstance(myProject),
                new EmptyProgressIndicator());
        assertEquals(6, links.size());

        String templateFileUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/ItemResourceWithFragment/items.html").toASCIIString();

        assertDocumentLink(links, //
                dl(r(21, 33, 21, 38), //
                        templateFileUri, "Open `src/main/resources/templates/ItemResourceWithFragment/items.html`"), //
                dl(r(22, 33, 22, 42), //
                        templateFileUri, "Open `src/main/resources/templates/ItemResourceWithFragment/items.html`"), //
                dl(r(23, 33, 23, 43), //
                        templateFileUri, "Create `src/main/resources/templates/ItemResourceWithFragment/items3.html`"), //
                dl(r(29, 33, 29, 39), //
                        templateFileUri, "Open `src/main/resources/templates/ItemResourceWithFragment/items2.html`"), //
                dl(r(30, 33, 30, 43), //
                        templateFileUri,
                        "Open `src/main/resources/templates/ItemResourceWithFragment/items2$id1.html`"), //
                dl(r(31, 33, 31, 43), //
                        templateFileUri,
                        "Open `src/main/resources/templates/ItemResourceWithFragment/items2$id2.html`"));
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

        QuteJavaDocumentLinkParams params = new QuteJavaDocumentLinkParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/ItemTemplatesCustomBasePath.java").toASCIIString();
        params.setUri(javaFileUri);

        List<DocumentLink> links = QuteSupportForJava.getInstance().documentLink(params, PsiUtilsLSImpl.getInstance(myProject),
                new EmptyProgressIndicator());
        assertEquals(3, links.size());

        String templateFileUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/ItemResourceWithFragment/items.html").toASCIIString();

        assertDocumentLink(links, //
                dl(r(9, 32, 9, 37), //
                        templateFileUri, "Open `src/main/resources/templates/ItemResourceWithFragment/items.html`"), //
                dl(r(10, 32, 10, 41), //
                        templateFileUri, "Open `src/main/resources/templates/ItemResourceWithFragment/items.html`"), //
                dl(r(11, 32, 11, 42), //
                        templateFileUri, "Create `src/main/resources/templates/ItemResourceWithFragment/items3.html`"));
    }

    public void testCheckedTemplateInInnerClassWithCustomBasePath() throws Exception {
        QuteJavaDocumentLinkParams params = new QuteJavaDocumentLinkParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/ItemResourceWithCustomBasePath.java").toASCIIString();
        params.setUri(javaFileUri);

        List<DocumentLink> links = QuteSupportForJava.getInstance().documentLink(params, PsiUtilsLSImpl.getInstance(myProject),
                new EmptyProgressIndicator());
        assertEquals(3, links.size());

        String templateFileUri = LSPIJUtils.toUri(module).resolve("src/main/resources/templates/ItemResourceWithFragment/items.html").toASCIIString();

        assertDocumentLink(links, //
                dl(r(21, 33, 21, 38), //
                        templateFileUri, "Open `src/main/resources/templates/ItemResourceWithFragment/items.html`"), //
                dl(r(22, 33, 22, 42), //
                        templateFileUri, "Open `src/main/resources/templates/ItemResourceWithFragment/items.html`"), //
                dl(r(23, 33, 23, 43), //
                        templateFileUri, "Create `src/main/resources/templates/ItemResourceWithFragment/items3.html`")); //
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
