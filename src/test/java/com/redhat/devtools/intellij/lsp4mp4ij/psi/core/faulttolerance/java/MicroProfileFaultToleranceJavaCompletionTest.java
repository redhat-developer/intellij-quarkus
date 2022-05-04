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
import com.intellij.openapi.module.ModuleUtilCore;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCompletionParams;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.assertJavaCompletion;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.c;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.p;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.te;

/**
 * Tests for completion in Java files
 *
 * @author datho7561
 */
public class MicroProfileFaultToleranceJavaCompletionTest extends MavenModuleImportingTestCase {

	@Test
	public void testFallbackMethodCompletion() throws Exception {
		Module module = createMavenModule("microprofile-fault-tolerance", new File("projects/maven/microprofile-fault-tolerance"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		String javaFileUri = MicroProfileForJavaAssert.fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/acme/FaultTolerantResource.java").toURI());

		// fallbackMethod = "b|bb"
		assertJavaCompletion(new MicroProfileJavaCompletionParams(javaFileUri, p(21, 33)), utils, //
				c(te(21, 32, 21, 35, "hello"), "hello()", CompletionItemKind.Method), //
				c(te(21, 32, 21, 35, "bbb"), "bbb()", CompletionItemKind.Method), //
				c(te(21, 32, 21, 35, "stringMethod"), "stringMethod()", CompletionItemKind.Method), //
				c(te(21, 32, 21, 35, "ccc"), "ccc()", CompletionItemKind.Method));
	}

	@Test
	public void testFallbackMethodCompletionBeginning() throws Exception {
		Module module = createMavenModule("microprofile-fault-tolerance", new File("projects/maven/microprofile-fault-tolerance"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		String javaFileUri = MicroProfileForJavaAssert.fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/acme/FaultTolerantResource.java").toURI());

		// fallbackMethod = "|bbb"
		assertJavaCompletion(new MicroProfileJavaCompletionParams(javaFileUri, p(21, 32)), utils, //
				c(te(21, 32, 21, 35, "hello"), "hello()", CompletionItemKind.Method), //
				c(te(21, 32, 21, 35, "bbb"), "bbb()", CompletionItemKind.Method), //
				c(te(21, 32, 21, 35, "stringMethod"), "stringMethod()", CompletionItemKind.Method), //
				c(te(21, 32, 21, 35, "ccc"), "ccc()", CompletionItemKind.Method));
	}


	@Test
	public void testFallbackMethodNoCompletionOutside() throws Exception {
		Module module = createMavenModule("microprofile-fault-tolerance", new File("projects/maven/microprofile-fault-tolerance"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		String javaFileUri = MicroProfileForJavaAssert.fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/acme/FaultTolerantResource.java").toURI());

		// fallbackMethod = |"bbb"
		assertJavaCompletion(new MicroProfileJavaCompletionParams(javaFileUri, p(21, 31)), utils);
	}

	@Test
	public void testFallbackMethodEmptyQuotes() throws Exception {
		Module module = createMavenModule("microprofile-fault-tolerance", new File("projects/maven/microprofile-fault-tolerance"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		String javaFileUri = MicroProfileForJavaAssert.fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/acme/OtherFaultToleranceResource.java").toURI());

		assertJavaCompletion(new MicroProfileJavaCompletionParams(javaFileUri, p(28, 32)), utils, //
				c(te(28, 32, 28, 32, "hello"), "hello()", CompletionItemKind.Method), //
				c(te(28, 32, 28, 32, "hi"), "hi()", CompletionItemKind.Method), //
				c(te(28, 32, 28, 32, "fourth"), "fourth()", CompletionItemKind.Method), //
				c(te(28, 32, 28, 32, "fifth"), "fifth()", CompletionItemKind.Method), //
				c(te(28, 32, 28, 32, "aaa"), "aaa()", CompletionItemKind.Method));
	}

	@Test
	public void testFallbackMethodNoSpacesAroundEquals() throws Exception {
		Module module = createMavenModule("microprofile-fault-tolerance", new File("projects/maven/microprofile-fault-tolerance"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		String javaFileUri = MicroProfileForJavaAssert.fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/acme/OtherFaultToleranceResource.java").toURI());

		assertJavaCompletion(new MicroProfileJavaCompletionParams(javaFileUri, p(35, 30)), utils, //
				c(te(35, 30, 35, 30, "hello"), "hello()", CompletionItemKind.Method), //
				c(te(35, 30, 35, 30, "hi"), "hi()", CompletionItemKind.Method), //
				c(te(35, 30, 35, 30, "third"), "third()", CompletionItemKind.Method), //
				c(te(35, 30, 35, 30, "fifth"), "fifth()", CompletionItemKind.Method), //
				c(te(35, 30, 35, 30, "aaa"), "aaa()", CompletionItemKind.Method));
	}

	@Test
	public void testFallbackMethodMultiline() throws Exception {
		Module module = createMavenModule("microprofile-fault-tolerance", new File("projects/maven/microprofile-fault-tolerance"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		String javaFileUri = MicroProfileForJavaAssert.fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/acme/OtherFaultToleranceResource.java").toURI());

		assertJavaCompletion(new MicroProfileJavaCompletionParams(javaFileUri, p(43, 9)), utils, //
				c(te(43, 9, 43, 9, "hello"), "hello()", CompletionItemKind.Method), //
				c(te(43, 9, 43, 9, "hi"), "hi()", CompletionItemKind.Method), //
				c(te(43, 9, 43, 9, "third"), "third()", CompletionItemKind.Method), //
				c(te(43, 9, 43, 9, "fourth"), "fourth()", CompletionItemKind.Method), //
				c(te(43, 9, 43, 9, "aaa"), "aaa()", CompletionItemKind.Method));
	}
}
