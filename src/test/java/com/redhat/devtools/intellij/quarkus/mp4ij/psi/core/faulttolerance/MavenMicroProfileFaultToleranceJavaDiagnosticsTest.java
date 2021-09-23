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
import com.redhat.devtools.intellij.quarkus.mp4ij.psi.internal.faulttolerance.java.MicroProfileFaultToleranceErrorCode;
import com.redhat.devtools.intellij.quarkus.search.PsiUtilsImpl;
import com.redhat.devtools.intellij.quarkus.search.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.quarkus.search.providers.MicroProfileFaultToleranceConstants;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static com.redhat.devtools.intellij.quarkus.module.MicroProfileForJavaAssert.assertJavaDiagnostics;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileForJavaAssert.d;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileForJavaAssert.fixURI;

/**
 * MicroProfile Fault Tolerance definition in Java file.
 *
 * @author Angelo ZERR
 *
 */
public class MavenMicroProfileFaultToleranceJavaDiagnosticsTest extends MavenImportingTestCase {

	@Test
	@Ignore("fault tolerance not yet implemented")
	public void fallbackMethodsMissing() throws Exception {
		Module module = createMavenModule("microprofile-fault-tolerance", new File("projects/maven/microprofile-fault-tolerance"));
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/acme/FaultTolerantResource.java").toURI());
		IPsiUtils utils = PsiUtilsImpl.getInstance();

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		diagnosticsParams.setUris(Arrays.asList(javaFileUri));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d = d(14, 31, 36, "The referenced fallback method 'aaa' does not exist", DiagnosticSeverity.Error,
				MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
				MicroProfileFaultToleranceErrorCode.FALLBACK_METHOD_DOES_NOT_EXIST);
		assertJavaDiagnostics(diagnosticsParams, utils, //
				d);
	}

	@Test
	public void testFallbackMethodValidationFaultTolerant() throws Exception {
		Module module = createMavenModule("microprofile-fault-tolerance", new File("projects/maven/microprofile-fault-tolerance"));
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/acme/OtherFaultTolerantResource.java").toURI());
		IPsiUtils utils = PsiUtilsImpl.getInstance();

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		diagnosticsParams.setUris(Arrays.asList(javaFileUri));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);
		assertJavaDiagnostics(diagnosticsParams, utils);
	}
}
