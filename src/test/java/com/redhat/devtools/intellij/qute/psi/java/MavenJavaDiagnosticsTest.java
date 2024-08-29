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
import com.redhat.devtools.intellij.qute.psi.internal.java.QuteErrorCode;
import com.redhat.qute.commons.QuteJavaDiagnosticsParams;
import org.eclipse.lsp4j.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tests for Qute @CheckedTemplate support validation inside Java files.
 *
 * @author Angelo ZERR
 */
public class MavenJavaDiagnosticsTest extends QuteMavenModuleImportingTestCase {

    private static final Logger LOGGER = Logger.getLogger(MavenJavaDiagnosticsTest.class.getSimpleName());
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
        //
        // Template bonjour;
        //
        // Template aurevoir;
        //
        // public HelloResource(@Location("detail/page1.html") Template page1,
        // @Location("detail/page2.html") Template page2) {
        // this.bonjour = page1;
        // this.aurevoir = requireNonNull(page2, "page is required");
        // }

        QuteJavaDiagnosticsParams params = new QuteJavaDiagnosticsParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/HelloResource.java").toASCIIString();
        params.setUris(Arrays.asList(javaFileUri));

        List<PublishDiagnosticsParams> publishDiagnostics = QuteSupportForJava.getInstance().diagnostics(params,
                PsiUtilsLSImpl.getInstance(myProject), new EmptyProgressIndicator());
        assertEquals(1, publishDiagnostics.size());

        List<Diagnostic> diagnostics = publishDiagnostics.get(0).getDiagnostics();
        assertEquals(3, diagnostics.size());

        assertDiagnostic(diagnostics, //
                new Diagnostic(r(20, 10, 20, 17),
                        "No template matching the path goodbye could be found for: org.acme.qute.HelloResource",
                        DiagnosticSeverity.Error, "qute", QuteErrorCode.NoMatchingTemplate.name()), //
                new Diagnostic(r(22, 11, 22, 34),
                        "No template matching the path detail/items2_v1.html could be found for: org.acme.qute.HelloResource",
                        DiagnosticSeverity.Error, "qute", QuteErrorCode.NoMatchingTemplate.name()), //
                new Diagnostic(r(32, 79, 32, 98),
                        "No template matching the path detail/page2.html could be found for: org.acme.qute.HelloResource",
                        DiagnosticSeverity.Error, "qute", QuteErrorCode.NoMatchingTemplate.name()));
    }

    @Test
    public void testCheckedTemplate() throws Exception {
        // @CheckedTemplate
        // public class Templates {
        //
        // public static native TemplateInstance hello2(String name);
        //
        // public static native TemplateInstance hello3(String name);
        QuteJavaDiagnosticsParams params = new QuteJavaDiagnosticsParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/Templates.java").toASCIIString();
        params.setUris(Arrays.asList(javaFileUri));

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

    public void testCheckedTemplateWithFragment() throws Exception {

        // @CheckedTemplate
        //public class ItemTemplates {
        //
        //    static native TemplateInstance items(List<Item> items);
        //    static native TemplateInstance items$id1(List<Item> items);
        //    static native TemplateInstance items3$id2(List<Item> items);
        //    static native TemplateInstance items3$(List<Item> items);
        //}

        QuteJavaDiagnosticsParams params = new QuteJavaDiagnosticsParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/ItemTemplates.java").toASCIIString();
        params.setUris(Arrays.asList(javaFileUri));

        List<PublishDiagnosticsParams> publishDiagnostics = QuteSupportForJava.getInstance().diagnostics(params,
                PsiUtilsLSImpl.getInstance(myProject), new EmptyProgressIndicator());
        assertEquals(1, publishDiagnostics.size());

        List<Diagnostic> diagnostics = publishDiagnostics.get(0).getDiagnostics();
        assertEquals(2, diagnostics.size());

        assertDiagnostic(diagnostics, //
                new Diagnostic(r(12, 35, 12, 45),
                        "No template matching the path items3 could be found for: org.acme.qute.ItemTemplates",
                        DiagnosticSeverity.Error, "qute", QuteErrorCode.NoMatchingTemplate.name()), //
                new Diagnostic(r(13, 35, 13, 42),
                        "Fragment [] not defined in template items3$",
                        DiagnosticSeverity.Error, "qute", QuteErrorCode.FragmentNotDefined.name()));

        // @CheckedTemplate(ignoreFragments = true)
        // public class ItemTemplatesIgnoreFragments {
        //
        //    static native TemplateInstance items2(List<Item> items);
        //    static native TemplateInstance items2$id1(List<Item> items);
        //    static native TemplateInstance items2$id2(List<Item> items);
        //}

        params = new QuteJavaDiagnosticsParams();
        javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/ItemTemplatesIgnoreFragments.java").toASCIIString();
        params.setUris(Arrays.asList(javaFileUri));

        publishDiagnostics = QuteSupportForJava.getInstance().diagnostics(params,
                PsiUtilsLSImpl.getInstance(myProject), new EmptyProgressIndicator());
        assertEquals(1, publishDiagnostics.size());

        diagnostics = publishDiagnostics.get(0).getDiagnostics();
        assertEquals(1, diagnostics.size());

        assertDiagnostic(diagnostics, //
                new Diagnostic(r(12, 35, 12, 45),
                        "No template matching the path items2$id2 could be found for: org.acme.qute.ItemTemplatesIgnoreFragments",
                        DiagnosticSeverity.Error, "qute", QuteErrorCode.NoMatchingTemplate.name()));
    }

    @Test
    public void testCheckedTemplateInInnerClass() throws Exception {
        // public class ItemResource {
        // @CheckedTemplate
        // static class Templates {
        // [Open `src/main/resources/templates/ItemResource/items.qute.html`]
        // static native TemplateInstance items(List<Item> items);

        QuteJavaDiagnosticsParams params = new QuteJavaDiagnosticsParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/ItemResource.java").toASCIIString();
        params.setUris(Arrays.asList(javaFileUri));

        List<PublishDiagnosticsParams> publishDiagnostics = QuteSupportForJava.getInstance().diagnostics(params,
                PsiUtilsLSImpl.getInstance(myProject), new EmptyProgressIndicator());
        assertEquals(1, publishDiagnostics.size());

        List<Diagnostic> diagnostics = publishDiagnostics.get(0).getDiagnostics();
        assertEquals(2, diagnostics.size());

        assertDiagnostic(diagnostics, //
                new Diagnostic(r(23, 33, 23, 36),
                        "No template matching the path ItemResource/map could be found for: org.acme.qute.ItemResource$Templates",
                        DiagnosticSeverity.Error, "qute", QuteErrorCode.NoMatchingTemplate.name()), //
                new Diagnostic(r(28, 33, 28, 39),
                        "No template matching the path ItemResource/items2 could be found for: org.acme.qute.ItemResource$Templates2",
                        DiagnosticSeverity.Error, "qute", QuteErrorCode.NoMatchingTemplate.name()));
    }

    @Test
    public void testCheckedTemplateInInnerClassWithFragment() throws Exception {

        QuteJavaDiagnosticsParams params = new QuteJavaDiagnosticsParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/ItemResourceWithFragment.java").toASCIIString();
        params.setUris(Arrays.asList(javaFileUri));

        List<PublishDiagnosticsParams> publishDiagnostics = QuteSupportForJava.getInstance().diagnostics(params,
                PsiUtilsLSImpl.getInstance(myProject), new EmptyProgressIndicator());
        assertEquals(1, publishDiagnostics.size());

        List<Diagnostic> diagnostics = publishDiagnostics.get(0).getDiagnostics();
        assertEquals(3, diagnostics.size());

        assertDiagnostic(diagnostics, //
                new Diagnostic(r(23, 33, 23, 43),
                        "No template matching the path ItemResourceWithFragment/items3 could be found for: org.acme.qute.ItemResourceWithFragment$Templates",
                        DiagnosticSeverity.Error, "qute", QuteErrorCode.NoMatchingTemplate.name()), //
                new Diagnostic(r(24, 33, 24, 40),
                        "Fragment [] not defined in template ItemResourceWithFragment/items3$",
                        DiagnosticSeverity.Error, "qute", QuteErrorCode.FragmentNotDefined.name()), //
                new Diagnostic(r(31, 33, 31, 43),
                        "No template matching the path ItemResourceWithFragment/items2$id2 could be found for: org.acme.qute.ItemResourceWithFragment$Templates2",
                        DiagnosticSeverity.Error, "qute", QuteErrorCode.NoMatchingTemplate.name()));
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

        QuteJavaDiagnosticsParams params = new QuteJavaDiagnosticsParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/qute/ItemTemplatesCustomBasePath.java").toASCIIString();
        params.setUris(Arrays.asList(javaFileUri));

        List<PublishDiagnosticsParams> publishDiagnostics = QuteSupportForJava.getInstance().diagnostics(params,
                PsiUtilsLSImpl.getInstance(myProject), new EmptyProgressIndicator());
        assertEquals(1, publishDiagnostics.size());

        List<Diagnostic> diagnostics = publishDiagnostics.get(0).getDiagnostics();
        assertEquals(2, diagnostics.size());

        assertDiagnostic(diagnostics, //
                new Diagnostic(r(11, 32, 11, 42),
                        "No template matching the path ItemResourceWithFragment/items3 could be found for: org.acme.qute.ItemTemplatesCustomBasePath",
                        DiagnosticSeverity.Error, "qute", QuteErrorCode.NoMatchingTemplate.name()), //
                new Diagnostic(r(12, 32, 12, 39),
                        "Fragment [] not defined in template ItemResourceWithFragment/items3$",
                        DiagnosticSeverity.Error, "qute", QuteErrorCode.FragmentNotDefined.name()));
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
        QuteJavaDiagnosticsParams params = new QuteJavaDiagnosticsParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/sample/HelloResource.java").toASCIIString();
        params.setUris(Arrays.asList(javaFileUri));

        List<PublishDiagnosticsParams> publishDiagnostics = QuteSupportForJava.getInstance().diagnostics(params,
                PsiUtilsLSImpl.getInstance(myProject), new EmptyProgressIndicator());
        assertEquals(1, publishDiagnostics.size());

        List<Diagnostic> diagnostics = publishDiagnostics.get(0).getDiagnostics();
        assertEquals(2, diagnostics.size());

        assertDiagnostic(diagnostics, //
                new Diagnostic(r(17, 11, 17, 18),
                        "No template matching the path HelloResource/Bonjour could be found for: org.acme.sample.HelloResource$Bonjour",
                        DiagnosticSeverity.Error, "qute", QuteErrorCode.NoMatchingTemplate.name()), //
                new Diagnostic(r(22, 11, 22, 21),
                        "No template matching the path Foo/HelloWorld could be found for: org.acme.sample.HelloResource$HelloWorld",
                        DiagnosticSeverity.Error, "qute", QuteErrorCode.NoMatchingTemplate.name()));
    }

    @Test
    public void testCheckedTemplateWithDefaultName() throws Exception {

        // @CheckedTemplate(defaultName=CheckedTemplate.HYPHENATED_ELEMENT_NAME)
        // static class Templates {
        // static native TemplateInstance HelloWorld(String name);

        var module = loadMavenProject(QuteMavenProjectName.qute_record);
        QuteJavaDiagnosticsParams params = new QuteJavaDiagnosticsParams();
        String javaFileUri = LSPIJUtils.toUri(module).resolve("src/main/java/org/acme/sample/ItemResource.java").toASCIIString();
        params.setUris(Arrays.asList(javaFileUri));

        List<PublishDiagnosticsParams> publishDiagnostics = QuteSupportForJava.getInstance().diagnostics(params,
                PsiUtilsLSImpl.getInstance(myProject), new EmptyProgressIndicator());

        assertEquals(1, publishDiagnostics.size());

        List<Diagnostic> diagnostics = publishDiagnostics.get(0).getDiagnostics();
        assertEquals(3, diagnostics.size());

        assertDiagnostic(diagnostics, //
                new Diagnostic(r(19, 33, 19, 43),
                        "No template matching the path ItemResource/HelloWorld could be found for: org.acme.sample.ItemResource$Templates",
                        DiagnosticSeverity.Error, "qute", QuteErrorCode.NoMatchingTemplate.name()), //
                new Diagnostic(r(25, 33, 25, 43),
                        "No template matching the path ItemResource/HelloWorld could be found for: org.acme.sample.ItemResource$Templates2",
                        DiagnosticSeverity.Error, "qute", QuteErrorCode.NoMatchingTemplate.name()), //
                new Diagnostic(r(31, 33, 31, 43),
                        "No template matching the path ItemResource/HelloWorld could be found for: org.acme.sample.ItemResource$Templates3",
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
