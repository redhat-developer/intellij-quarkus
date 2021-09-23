/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.quarkus.mp4ij.psi.core.faulttolerance;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.redhat.devtools.intellij.quarkus.maven.MavenImportingTestCase;
import com.redhat.devtools.intellij.quarkus.module.MicroProfileForJavaAssert;
import com.redhat.devtools.intellij.quarkus.search.PsiUtilsImpl;
import com.redhat.devtools.intellij.quarkus.search.core.utils.IPsiUtils;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.quarkus.module.MicroProfileForJavaAssert.assertJavaDefinitions;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileForJavaAssert.def;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileForJavaAssert.p;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileForJavaAssert.r;

/**
 * MicroProfile Fault Tolerance validation in Java file.
 *
 * @author Angelo ZERR
 *
 */
public class MavenMicroProfileFaultToleranceJavaDefinitionTest extends MavenImportingTestCase {

	@Test
	public void testFallbackMethodsDefinition() throws Exception {
		Module module = createMavenModule("microprofile-fault-tolerance", new File("projects/maven/microprofile-fault-tolerance"));
		String javaFileUri = MicroProfileForJavaAssert.fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/acme/FaultTolerantResource.java").toURI());
		IPsiUtils utils = PsiUtilsImpl.getInstance();


		// @Fallback(fallbackMethod = "a|aa") --> no definition
		assertJavaDefinitions(p(14, 33), javaFileUri, utils);

		// @Fallback(|) --> no definition
		assertJavaDefinitions(p(35, 14), javaFileUri, utils);

		// @Fallback(fallbackMethod = "b|bb") --> public String bbb()
		assertJavaDefinitions(p(21, 33), javaFileUri, utils, //
				def(r(21, 32, 35), javaFileUri, r(26, 18, 21)));

	}
}
