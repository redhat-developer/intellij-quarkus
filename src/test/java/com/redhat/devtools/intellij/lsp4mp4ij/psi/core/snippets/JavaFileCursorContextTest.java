/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.snippets;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManagerForJava;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4mp.commons.JavaCursorContextKind;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCompletionParams;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.saveFile;

/**
 * Tests for the implementation of
 * <code>microprofile/java/javaCursorContext</code>.
 */
public class JavaFileCursorContextTest extends MavenModuleImportingTestCase {

    private Module javaProject;

    private static final String SRC_PREFIX = "src/main/java/";
    private static final String EMPTY_FILE_PATH = SRC_PREFIX + "org/acme/config/Empty.java";

    private static final String GREETING_FILE_PATH = SRC_PREFIX + "org/acme/config/GreetingResource.java";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        javaProject = createMavenModule(new File("projects/lsp4mp/projects/maven/config-hover"));
    }

    private String getJavaFileUri(String path) {
        path = path.startsWith(SRC_PREFIX) ? path : SRC_PREFIX + path;
        return new File(ModuleUtilCore.getModuleDirPath(javaProject), path).toURI().toString();
    }

    @After
    public void tearDown() throws Exception {
        resetEmptyFile();
        super.tearDown();
    }

    private void resetEmptyFile() throws Exception {
        setContents(EMPTY_FILE_PATH, "");
    }

    private void setContents(String path, String content) throws Exception {
        path = path.startsWith(SRC_PREFIX) ? path.substring(SRC_PREFIX.length()) : path;
        saveFile(path, content, javaProject, true);
    }


    // context kind tests

    public void testEmptyFileContext() throws Exception {
        String javaFileUri = getJavaFileUri(EMPTY_FILE_PATH);
        // |
        MicroProfileJavaCompletionParams params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(0, 0));
        assertEquals(JavaCursorContextKind.IN_EMPTY_FILE,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());
    }

    @Test
    public void testJustSnippetFileContext() throws Exception {
        String javaFileUri = getJavaFileUri(EMPTY_FILE_PATH);
        setContents(EMPTY_FILE_PATH, "rest_class");

        // rest_class|
        MicroProfileJavaCompletionParams params = new MicroProfileJavaCompletionParams(javaFileUri,
                new Position(0, "rest_class".length()));
        assertEquals(JavaCursorContextKind.IN_EMPTY_FILE,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());

        // |rest_class
        params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(0, 0));
        assertEquals(JavaCursorContextKind.IN_EMPTY_FILE,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());

        // rest|_class
        params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(0, 4));
        assertEquals(JavaCursorContextKind.IN_EMPTY_FILE,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());
    }

    @Test
    public void testBeforeFieldContext() throws Exception {
        String javaFileUri = getJavaFileUri(GREETING_FILE_PATH);

        // ...
        // @ConfigProperty(name = "greeting.message")
        // |String message;
        // ...
        MicroProfileJavaCompletionParams params = new MicroProfileJavaCompletionParams(javaFileUri,
                new Position(15, 4));
        assertEquals(JavaCursorContextKind.IN_FIELD_ANNOTATIONS,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());

        // ...
        // |@ConfigProperty(name = "greeting.message")
        // String message;
        // ...
        params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(14, 4));
        assertEquals(JavaCursorContextKind.BEFORE_FIELD,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());
    }

    @Test
    public void testBeforeMethodContext() throws Exception {
        String javaFileUri = getJavaFileUri(GREETING_FILE_PATH);
        // ...
        // @GET
        // @Produces(MediaType.TEXT_PLAIN)
        // |public String hello() {
        // ...
        MicroProfileJavaCompletionParams params = new MicroProfileJavaCompletionParams(javaFileUri,
                new Position(34, 4));
        assertEquals(JavaCursorContextKind.IN_METHOD_ANNOTATIONS,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());

        // ...
        // |@GET
        // @Produces(MediaType.TEXT_PLAIN)
        // public String hello() {
        // ...
        params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(32, 4));
        assertEquals(JavaCursorContextKind.BEFORE_METHOD,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());
    }

    @Test
    public void testInMethodContext() throws Exception {
        String javaFileUri = getJavaFileUri(GREETING_FILE_PATH);
        // ...
        // @GET
        // @Produces(MediaType.TEXT_PLAIN)
        // public String hello() {
        // | return message + " " + name.orElse("world") + suffix;
        // }
        // ...
        MicroProfileJavaCompletionParams params = new MicroProfileJavaCompletionParams(javaFileUri,
                new Position(35, 0));
        assertEquals(JavaCursorContextKind.NONE,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());

        // ...
        // @GET
        // @Produces(MediaType.TEXT_PLAIN)
        // p|ublic String hello() {
        // return message + " " + name.orElse("world") + suffix;
        // }
        // ...
        params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(34, 5));
        assertEquals(JavaCursorContextKind.NONE,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());
    }

    @Test
    public void testInClassContext() throws Exception {
        String javaFileUri = getJavaFileUri(GREETING_FILE_PATH);
        // ...
        // public String hello() {
        // return message + " " + name.orElse("world") + suffix;
        // }
        // |}
        MicroProfileJavaCompletionParams params = new MicroProfileJavaCompletionParams(javaFileUri,
                new Position(37, 0));
        assertEquals(JavaCursorContextKind.IN_CLASS,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());
    }

    @Test
    public void testAfterClassContext() throws Exception {
        String javaFileUri = getJavaFileUri(GREETING_FILE_PATH);
        // ...
        // public String hello() {
        // return message + " " + name.orElse("world") + suffix;
        // }
        // }
        // |
        MicroProfileJavaCompletionParams params = new MicroProfileJavaCompletionParams(javaFileUri,
                new Position(38, 0));
        assertEquals(JavaCursorContextKind.NONE,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());
    }

    @Test
    public void testClassContextUsingInterface() throws Exception {
        String javaFileUri = getJavaFileUri("src/main/java/org/acme/config/MyInterface.java");
        // ...
        // public interface MyInterface {
        // |
        // ...
        MicroProfileJavaCompletionParams params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(3, 0));
        assertEquals(JavaCursorContextKind.BEFORE_FIELD,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());

        // ...
        // public interface MyInterface {
        // ...
        // public void helloWorld();
        // |
        // }
        params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(7, 0));
        assertEquals(JavaCursorContextKind.IN_CLASS,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());
    }

    @Test
    public void testClassContextUsingEnum() throws Exception {
        String javaFileUri = getJavaFileUri("src/main/java/org/acme/config/MyEnum.java");
        // ...
        // public enum MyEnum {
        // |
        // VALUE;
        // ...
        MicroProfileJavaCompletionParams params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(3, 0));
        assertEquals(JavaCursorContextKind.BEFORE_FIELD,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());

        // ...
        // public enum MyEnum {
        // ...
        // |
        // public void helloWorld();
        // ...
        params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(7, 0));
        assertEquals(JavaCursorContextKind.BEFORE_METHOD,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());

        // ...
        // public enum MyEnum {
        // ...
        // public void helloWorld();
        // |
        // }
        params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(9, 0));
        assertEquals(JavaCursorContextKind.IN_CLASS,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());
    }

    @Test
    public void testClassContextUsingAnnotation() throws Exception {
        String javaFileUri = getJavaFileUri("src/main/java/org/acme/config/MyAnnotation.java");
        // ...
        // public @interface MyAnnotation {
        // |
        // public static String MY_STRING = "asdf";
        // ...
        MicroProfileJavaCompletionParams params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(3, 0));
        assertEquals(JavaCursorContextKind.BEFORE_FIELD,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());

        // ...
        // public @interface MyAnnotation {
        // ...
        // |
        // public String value() default MY_STRING;
        // ...
        params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(5, 0));
        assertEquals(JavaCursorContextKind.BEFORE_METHOD,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());

        // ...
        // public @interface MyAnnotation {
        // ...
        // public String value() default MY_STRING;
        // |
        // }
        params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(7, 0));
        assertEquals(JavaCursorContextKind.IN_CLASS,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());
    }

    @Test
    public void testBeforeClassContext() throws Exception {
        String javaFileUri = getJavaFileUri("src/main/java/org/acme/config/MyNestedClass.java");
        // ...
        // @Singleton
        // public class MyNestedClass {
        // |
        // @Singleton
        // static class MyNestedNestedClass {
        // ...
        MicroProfileJavaCompletionParams params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(4, 0));
        assertEquals(JavaCursorContextKind.BEFORE_INNER_CLASS,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());

        // ...
        // @Singleton
        // public class MyNestedClass {
        //
        // |@Singleton
        // static class MyNestedNestedClass {
        // ...
        params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(5, 0));
        assertEquals(JavaCursorContextKind.BEFORE_INNER_CLASS,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());

        // ...
        // @Singleton
        // public class MyNestedClass {
        //
        // @Singleton
        // | static class MyNestedNestedClass {
        // ...
        params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(6, 0));
        assertEquals(JavaCursorContextKind.IN_CLASS_ANNOTATIONS,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());

        // ...
        // |
        // @Singleton
        // public class MyNestedClass {
        // ...
        params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(1, 0));
        assertEquals(JavaCursorContextKind.BEFORE_TOP_LEVEL_CLASS,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());

        // ...
        // @Singleton
        // |public class MyNestedClass {
        // ...
        params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(3, 0));
        assertEquals(JavaCursorContextKind.IN_CLASS_ANNOTATIONS,
                PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getKind());
    }

    // prefix tests

    @Test
    public void testAtBeginningOfFile() throws Exception {
        String javaFileUri = getJavaFileUri(EMPTY_FILE_PATH);
        // |
        MicroProfileJavaCompletionParams params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(0, 0));
        assertEquals("", PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getPrefix());
    }

    @Test
    public void testOneWord() throws Exception {
        String javaFileUri = getJavaFileUri(EMPTY_FILE_PATH);
        setContents(EMPTY_FILE_PATH, "rest_class");

        // rest_class|
        MicroProfileJavaCompletionParams params = new MicroProfileJavaCompletionParams(javaFileUri,
                new Position(0, "rest_class".length()));
        assertEquals("rest_class", PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getPrefix());

        // |rest_class
        params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(0, 0));
        assertEquals("", PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getPrefix());

        // rest_|class
        params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(0, 5));
        assertEquals("rest_", PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getPrefix());
    }

    @Test
    public void testTwoWords() throws Exception {
        String javaFileUri = getJavaFileUri(EMPTY_FILE_PATH);
        setContents(EMPTY_FILE_PATH, "asdf hjkl");

        // asdf hjk|l
        MicroProfileJavaCompletionParams params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(0, 8));
        assertEquals("hjk", PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getPrefix());

        // asdf |hjkl
        params = new MicroProfileJavaCompletionParams(javaFileUri, new Position(0, 5));
        assertEquals("", PropertiesManagerForJava.getInstance().javaCursorContext(params, getJDTUtils()).getPrefix());
    }

}