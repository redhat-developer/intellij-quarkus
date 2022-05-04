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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.faulttolerance.java;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.java.MicroProfileFaultToleranceErrorCode;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.MicroProfileFaultToleranceConstants;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.assertJavaDiagnostics;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.d;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.fixURI;

/**
 * MicroProfile Fault Tolerance definition in Java file.
 *
 * @author Angelo ZERR
 *
 */
public class MicroProfileFaultToleranceJavaDiagnosticsTest extends MavenModuleImportingTestCase {

	@Test
	public void testFallbackMethodsMissing() throws Exception {
		Module module = createMavenModule("microprofile-fault-tolerance", new File("projects/maven/microprofile-fault-tolerance"));
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/acme/FaultTolerantResource.java").toURI());
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

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
	public void testAsynchronousNonFutureOrCompletionStage() throws Exception {
		Module module = createMavenModule("microprofile-fault-tolerance", new File("projects/maven/microprofile-fault-tolerance"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/acme/AsynchronousFaultToleranceResource.java").toURI());
		diagnosticsParams.setUris(Arrays.asList(javaFileUri));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d1 = d(34, 11, 17,
				"The annotated method does not return an object of type Future, CompletionStage or Uni",
				DiagnosticSeverity.Error, MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
				MicroProfileFaultToleranceErrorCode.FAULT_TOLERANCE_DEFINITION_EXCEPTION);

		Diagnostic d2 = d(39, 11, 15,
				"The annotated method does not return an object of type Future, CompletionStage or Uni",
				DiagnosticSeverity.Error, MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
				MicroProfileFaultToleranceErrorCode.FAULT_TOLERANCE_DEFINITION_EXCEPTION);

		Diagnostic d3 = d(44, 11, 36,
				"The annotated method does not return an object of type Future, CompletionStage or Uni",
				DiagnosticSeverity.Error, MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
				MicroProfileFaultToleranceErrorCode.FAULT_TOLERANCE_DEFINITION_EXCEPTION);

		assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3);
	}

	@Test
	public void testAsynchronousClassNonFutureOrCompletionStage() throws Exception {
		Module module = createMavenModule("microprofile-fault-tolerance", new File("projects/maven/microprofile-fault-tolerance"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/acme/AsynchronousFaultToleranceClassResource.java").toURI());
		diagnosticsParams.setUris(Arrays.asList(javaFileUri));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d1 = d(32, 11, 17,
				"The annotated method does not return an object of type Future, CompletionStage or Uni",
				DiagnosticSeverity.Error, MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
				MicroProfileFaultToleranceErrorCode.FAULT_TOLERANCE_DEFINITION_EXCEPTION);

		Diagnostic d2 = d(36, 11, 15,
				"The annotated method does not return an object of type Future, CompletionStage or Uni",
				DiagnosticSeverity.Error, MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
				MicroProfileFaultToleranceErrorCode.FAULT_TOLERANCE_DEFINITION_EXCEPTION);

		Diagnostic d3 = d(40, 11, 36,
				"The annotated method does not return an object of type Future, CompletionStage or Uni",
				DiagnosticSeverity.Error, MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
				MicroProfileFaultToleranceErrorCode.FAULT_TOLERANCE_DEFINITION_EXCEPTION);

		assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3);
	}

	@Test
	public void testFallbackMethodValidationFaultTolerant() throws Exception {
		Module module = createMavenModule("microprofile-fault-tolerance", new File("projects/maven/microprofile-fault-tolerance"));
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/acme/OtherFaultTolerantResource.java").toURI());
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		diagnosticsParams.setUris(Arrays.asList(javaFileUri));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);
		assertJavaDiagnostics(diagnosticsParams, utils);
	}
}
