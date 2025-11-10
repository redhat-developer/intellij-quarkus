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
import com.intellij.testFramework.IndexingTestUtil;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.MicroProfileFaultToleranceConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.java.MicroProfileFaultToleranceErrorCode;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.*;

/**
 * MicroProfile Fault Tolerance definition in Java file.
 *
 * @author Angelo ZERR
 */
public class MicroProfileFaultToleranceJavaDiagnosticsTest extends MavenModuleImportingTestCase {

    @Test
    public void testFallbackMethodsMissing() throws Exception {
        Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-fault-tolerance"));
        String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/acme/FaultTolerantResource.java").toURI());
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(javaFileUri));
        diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

        Diagnostic d = d(14, 31, 36,
                "The referenced fallback method 'aaa' does not exist.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
                MicroProfileFaultToleranceErrorCode.FALLBACK_METHOD_DOES_NOT_EXIST);
        assertJavaDiagnostics(diagnosticsParams, utils, d);
    }

    @Test
    public void testAsynchronousNonFutureOrCompletionStage() throws Exception {
        Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-fault-tolerance"));
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
        String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/acme/AsynchronousFaultToleranceResource.java").toURI());
        diagnosticsParams.setUris(Arrays.asList(javaFileUri));
        diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

        Diagnostic d1 = d(34, 11, 17,
                "The annotated method 'objectReturnTypeAsynchronousMethod' with @Asynchronous should return an object of type 'java.util.concurrent.Future', 'java.util.concurrent.CompletionStage'.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
                MicroProfileFaultToleranceErrorCode.FAULT_TOLERANCE_DEFINITION_EXCEPTION);

        Diagnostic d2 = d(39, 11, 15,
                "The annotated method 'noReturnTypeAsynchronousMethod' with @Asynchronous should return an object of type 'java.util.concurrent.Future', 'java.util.concurrent.CompletionStage'.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
                MicroProfileFaultToleranceErrorCode.FAULT_TOLERANCE_DEFINITION_EXCEPTION);

        Diagnostic d3 = d(44, 11, 36,
                "The annotated method 'completableFutureAsynchronousMethod' with @Asynchronous should return an object of type 'java.util.concurrent.Future', 'java.util.concurrent.CompletionStage'.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
                MicroProfileFaultToleranceErrorCode.FAULT_TOLERANCE_DEFINITION_EXCEPTION);

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3);
    }

    @Test
    public void testAsynchronousClassNonFutureOrCompletionStage() throws Exception {
        Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-fault-tolerance"));
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
        String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/acme/AsynchronousFaultToleranceClassResource.java").toURI());
        diagnosticsParams.setUris(Arrays.asList(javaFileUri));
        diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

        Diagnostic d1 = d(32, 11, 17,
                "The annotated method 'objectReturnTypeAsynchronousMethod' with @Asynchronous should return an object of type 'java.util.concurrent.Future', 'java.util.concurrent.CompletionStage'.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
                MicroProfileFaultToleranceErrorCode.FAULT_TOLERANCE_DEFINITION_EXCEPTION);

        Diagnostic d2 = d(36, 11, 15,
                "The annotated method 'noReturnTypeAsynchronousMethod' with @Asynchronous should return an object of type 'java.util.concurrent.Future', 'java.util.concurrent.CompletionStage'.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
                MicroProfileFaultToleranceErrorCode.FAULT_TOLERANCE_DEFINITION_EXCEPTION);

        Diagnostic d3 = d(40, 11, 36,
                "The annotated method 'completableFutureAsynchronousMethod' with @Asynchronous should return an object of type 'java.util.concurrent.Future', 'java.util.concurrent.CompletionStage'.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
                MicroProfileFaultToleranceErrorCode.FAULT_TOLERANCE_DEFINITION_EXCEPTION);

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3);
    }

    @Test
    public void testFallbackMethodValidationFaultTolerant() throws Exception {
        Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-fault-tolerance"));
        String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/acme/OtherFaultTolerantResource.java").toURI());
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(javaFileUri));
        diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testCircuitBreakerClientForValidationDelay() throws Exception {
        Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-fault-tolerance"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
        String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/eclipse/microprofile/fault/tolerance/tck/invalidParameters/CircuitBreakerClientForValidationDelay.java").toURI());
        diagnosticsParams.setUris(Arrays.asList(javaFileUri));
        diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

        Diagnostic d1 = d(36, 35, 37,
                "The value `-1` must be between `0` (inclusive) and `1` (inclusive).",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE, null);

        Diagnostic d2 = d(41, 35, 36,
                "The value `2` must be between `0` (inclusive) and `1` (inclusive).",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE, null);

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);
    }

    @Test
    public void testBulkheadClientForValidation() throws Exception {
        Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-fault-tolerance"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
        String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/eclipse/microprofile/fault/tolerance/tck/invalidParameters/BulkheadClientForValidation.java").toURI());
        diagnosticsParams.setUris(Arrays.asList(javaFileUri));
        diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

        Diagnostic d1 = d(34, 14, 16,
                "The value `-1` must be greater than or equal to `0`.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE, null);

        Diagnostic d2 = d(39, 20, 22,
                "The value `-1` must be greater than or equal to `0`.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE, null);

        Diagnostic d3 = d(44, 31, 33,
                "The value `-1` must be greater than or equal to `0`.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE, null);

        Diagnostic d4_a = d(49, 20, 22,
                "The value `-1` must be greater than or equal to `0`.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE, null);

        Diagnostic d4_b = d(49, 41, 43,
                "The value `-1` must be greater than or equal to `0`.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE, null);

        Diagnostic d5 = d(54, 20, 22,
                "The value `-1` must be greater than or equal to `0`.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE, null);

        Diagnostic d6 = d(59, 40, 42,
                "The value `-1` must be greater than or equal to `0`.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE, null);

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3, d4_a, d4_b,
                d5, d6);
    }

    @Test
    public void testTimeoutClientForValidation() throws Exception {
        Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-fault-tolerance"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
        String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/eclipse/microprofile/fault/tolerance/tck/invalidParameters/TimeoutClientForValidation.java").toURI());
        diagnosticsParams.setUris(Arrays.asList(javaFileUri));
        diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

        Diagnostic d1 = d(33, 13, 15,
                "The value `-1` must be greater than or equal to `0`.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE, null);

        Diagnostic d2 = d(38, 19, 21,
                "The value `-1` must be greater than or equal to `0`.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE, null);

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);
    }

    @Test
    public void testRetryClientForValidation() throws Exception {
        Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-fault-tolerance"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
        String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/eclipse/microprofile/fault/tolerance/tck/invalidParameters/RetryClientForValidation.java").toURI());
        diagnosticsParams.setUris(Arrays.asList(javaFileUri));
        diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

        Diagnostic d1 = d(33, 19, 21,
                "The value `-1` must be greater than or equal to `0`.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE, null);

        Diagnostic d2 = d(38, 25, 27,
                "The value `-1` must be greater than or equal to `0`.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE, null);

        Diagnostic d3 = d(43, 20, 22,
                "The value `-1` must be greater than or equal to `0`.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE, null);

        Diagnostic d4 = d(48, 24, 26,
                "The value `-2` must be greater than or equal to `-1`.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE, null);

        Diagnostic d5 = d(53, 19, 23,
                "The effective delay may exceed the `maxDuration` member value.",
                DiagnosticSeverity.Warning,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
                MicroProfileFaultToleranceErrorCode.DELAY_EXCEEDS_MAX_DURATION);

        Diagnostic d6 = d(58, 19, 23,
                "The effective delay may exceed the `maxDuration` member value.",
                DiagnosticSeverity.Warning,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
                MicroProfileFaultToleranceErrorCode.DELAY_EXCEEDS_MAX_DURATION);

        Diagnostic d7 = d(88, 19, 23,
                "The effective delay may exceed the `maxDuration` member value.",
                DiagnosticSeverity.Warning,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
                MicroProfileFaultToleranceErrorCode.DELAY_EXCEEDS_MAX_DURATION);

        Diagnostic d8 = d(98, 19, 31,
                "The effective delay may exceed the `maxDuration` member value.",
                DiagnosticSeverity.Warning,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
                MicroProfileFaultToleranceErrorCode.DELAY_EXCEEDS_MAX_DURATION);

        Diagnostic d9 = d(103, 19, 25,
                "The effective delay may exceed the `maxDuration` member value.",
                DiagnosticSeverity.Warning,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
                MicroProfileFaultToleranceErrorCode.DELAY_EXCEEDS_MAX_DURATION);

        Diagnostic d10 = d(108, 19, 23,
                "The value `-12` must be greater than or equal to `0`.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE, null);


        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3, d4, d10, d5, d6, d7, d8, d9);
    }

    @Test
    public void testRetryClientForValidationClass() throws Exception {
        Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-fault-tolerance"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
        String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/eclipse/microprofile/fault/tolerance/tck/invalidParameters/RetryClientForValidationClass.java").toURI());
        diagnosticsParams.setUris(Arrays.asList(javaFileUri));
        diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

        Diagnostic d1 = d(32, 15, 17,
                "The value `-2` must be greater than or equal to `0`.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE, null);

        Diagnostic d2 = d(32, 33, 35,
                "The value `-1` must be greater than or equal to `0`.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE, null);

        Diagnostic d3 = d(32, 46, 48,
                "The value `-1` must be greater than or equal to `0`.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE, null);

        Diagnostic d4 = d(32, 63, 65,
                "The value `-2` must be greater than or equal to `-1`.",
                DiagnosticSeverity.Error,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE, null);

        Diagnostic d5 = d(39, 19, 23,
                "The effective delay may exceed the `maxDuration` member value.",
                DiagnosticSeverity.Warning,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
                MicroProfileFaultToleranceErrorCode.DELAY_EXCEEDS_MAX_DURATION);

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3, d4, d5);
    }

    @Test
    public void testRetryClientForValidationChronoUnit() throws Exception {
        Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-fault-tolerance"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
        String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/eclipse/microprofile/fault/tolerance/tck/invalidParameters/RetryClientForValidationChronoUnit.java").toURI());
        diagnosticsParams.setUris(Arrays
                .asList(javaFileUri));
        diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

        Diagnostic d1 = d(24, 15, 16,
                "The effective delay may exceed the `maxDuration` member value.",
                DiagnosticSeverity.Warning,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
                MicroProfileFaultToleranceErrorCode.DELAY_EXCEEDS_MAX_DURATION);

        Diagnostic d2 = d(42, 19, 20,
                "The effective delay may exceed the `maxDuration` member value.",
                DiagnosticSeverity.Warning,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
                MicroProfileFaultToleranceErrorCode.DELAY_EXCEEDS_MAX_DURATION);

        Diagnostic d3 = d(47, 19, 20,
                "The effective delay may exceed the `maxDuration` member value.",
                DiagnosticSeverity.Warning,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
                MicroProfileFaultToleranceErrorCode.DELAY_EXCEEDS_MAX_DURATION);

        Diagnostic d4 = d(52, 19, 21,
                "The effective delay may exceed the `maxDuration` member value.",
                DiagnosticSeverity.Warning,
                MicroProfileFaultToleranceConstants.DIAGNOSTIC_SOURCE,
                MicroProfileFaultToleranceErrorCode.DELAY_EXCEEDS_MAX_DURATION);

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3, d4);
    }
}
