/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.faulttolerance.java;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.LSP4MPMavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileMavenProjectName;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCompletionParams;
import org.junit.Test;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.*;

/**
 * Tests for completion in Java files
 *
 * @author datho7561
 */
public class MicroProfileFaultToleranceJavaCompletionTest extends LSP4MPMavenModuleImportingTestCase {

    @Test
    public void testFallbackMethodCompletion() throws Exception {
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.microprofile_fault_tolerance);
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        String javaFileUri = getFileUri("src/main/java/org/acme/FaultTolerantResource.java", javaProject);

        // fallbackMethod = "b|bb"
        assertJavaCompletion(new MicroProfileJavaCompletionParams(javaFileUri, p(21, 33)), utils, //
                c(te(21, 32, 21, 35, "hello"), "hello()", CompletionItemKind.Method), //
                c(te(21, 32, 21, 35, "bbb"), "bbb()", CompletionItemKind.Method), //
                c(te(21, 32, 21, 35, "stringMethod"), "stringMethod()", CompletionItemKind.Method), //
                c(te(21, 32, 21, 35, "ccc"), "ccc()", CompletionItemKind.Method));
    }

    @Test
    public void testFallbackMethodCompletionBeginning() throws Exception {
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.microprofile_fault_tolerance);
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        String javaFileUri = getFileUri("src/main/java/org/acme/FaultTolerantResource.java", javaProject);

        // fallbackMethod = "|bbb"
        assertJavaCompletion(new MicroProfileJavaCompletionParams(javaFileUri, p(21, 32)), utils, //
                c(te(21, 32, 21, 35, "hello"), "hello()", CompletionItemKind.Method), //
                c(te(21, 32, 21, 35, "bbb"), "bbb()", CompletionItemKind.Method), //
                c(te(21, 32, 21, 35, "stringMethod"), "stringMethod()", CompletionItemKind.Method), //
                c(te(21, 32, 21, 35, "ccc"), "ccc()", CompletionItemKind.Method));
    }


    @Test
    public void testFallbackMethodNoCompletionOutside() throws Exception {
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.microprofile_fault_tolerance);
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        String javaFileUri = getFileUri("src/main/java/org/acme/FaultTolerantResource.java", javaProject);

        // fallbackMethod = |"bbb"
        assertJavaCompletion(new MicroProfileJavaCompletionParams(javaFileUri, p(21, 31)), utils);
    }

    @Test
    public void testFallbackMethodEmptyQuotes() throws Exception {
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.microprofile_fault_tolerance);
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        String javaFileUri = getFileUri("src/main/java/org/acme/OtherFaultToleranceResource.java", javaProject);

        assertJavaCompletion(new MicroProfileJavaCompletionParams(javaFileUri, p(28, 32)), utils, //
                c(te(28, 32, 28, 32, "hello"), "hello()", CompletionItemKind.Method), //
                c(te(28, 32, 28, 32, "hi"), "hi()", CompletionItemKind.Method), //
                c(te(28, 32, 28, 32, "fourth"), "fourth()", CompletionItemKind.Method), //
                c(te(28, 32, 28, 32, "fifth"), "fifth()", CompletionItemKind.Method), //
                c(te(28, 32, 28, 32, "aaa"), "aaa()", CompletionItemKind.Method));
    }

    @Test
    public void testFallbackMethodNoSpacesAroundEquals() throws Exception {
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.microprofile_fault_tolerance);
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        String javaFileUri = getFileUri("src/main/java/org/acme/OtherFaultToleranceResource.java", javaProject);

        assertJavaCompletion(new MicroProfileJavaCompletionParams(javaFileUri, p(35, 30)), utils, //
                c(te(35, 30, 35, 30, "hello"), "hello()", CompletionItemKind.Method), //
                c(te(35, 30, 35, 30, "hi"), "hi()", CompletionItemKind.Method), //
                c(te(35, 30, 35, 30, "third"), "third()", CompletionItemKind.Method), //
                c(te(35, 30, 35, 30, "fifth"), "fifth()", CompletionItemKind.Method), //
                c(te(35, 30, 35, 30, "aaa"), "aaa()", CompletionItemKind.Method));
    }

    @Test
    public void testFallbackMethodMultiline() throws Exception {
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.microprofile_fault_tolerance);
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        String javaFileUri = getFileUri("src/main/java/org/acme/OtherFaultToleranceResource.java", javaProject);

        assertJavaCompletion(new MicroProfileJavaCompletionParams(javaFileUri, p(43, 9)), utils, //
                c(te(43, 9, 43, 9, "hello"), "hello()", CompletionItemKind.Method), //
                c(te(43, 9, 43, 9, "hi"), "hi()", CompletionItemKind.Method), //
                c(te(43, 9, 43, 9, "third"), "third()", CompletionItemKind.Method), //
                c(te(43, 9, 43, 9, "fourth"), "fourth()", CompletionItemKind.Method), //
                c(te(43, 9, 43, 9, "aaa"), "aaa()", CompletionItemKind.Method));
    }
}
