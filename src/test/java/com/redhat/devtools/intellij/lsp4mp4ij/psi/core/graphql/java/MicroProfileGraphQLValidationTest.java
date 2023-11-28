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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.graphql.java;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.LSP4MPMavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileMavenProjectName;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.graphql.MicroProfileGraphQLConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.graphql.java.MicroProfileGraphQLErrorCode;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.junit.Test;

import java.util.Collections;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.*;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.getFileUri;

/**
 * Tests for {@link com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.graphql.java.MicroProfileGraphQLASTValidator}.
 */
public class MicroProfileGraphQLValidationTest extends LSP4MPMavenModuleImportingTestCase {

    @Test
    public void testIncorrectDirectivePlacement() throws Exception {
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.microprofile_graphql);
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
        String javaFileUri = getFileUri("/src/main/java/io/openliberty/graphql/sample/WeatherService.java", javaProject);
        diagnosticsParams.setUris(Collections.singletonList(javaFileUri));
        diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

        Diagnostic d1 = d(41, 4, 15,
                "Directive 'io.openliberty.graphql.sample.Optimistic' is not allowed on element type 'FIELD_DEFINITION'",
                DiagnosticSeverity.Error, MicroProfileGraphQLConstants.DIAGNOSTIC_SOURCE,
                MicroProfileGraphQLErrorCode.WRONG_DIRECTIVE_PLACEMENT);

        Diagnostic d2 = d(51, 50, 61,
                "Directive 'io.openliberty.graphql.sample.Optimistic' is not allowed on element type 'ARGUMENT_DEFINITION'",
                DiagnosticSeverity.Error, MicroProfileGraphQLConstants.DIAGNOSTIC_SOURCE,
                MicroProfileGraphQLErrorCode.WRONG_DIRECTIVE_PLACEMENT);

        Diagnostic d3 = d(75, 59, 70,
                "Directive 'io.openliberty.graphql.sample.Optimistic' is not allowed on element type 'ARGUMENT_DEFINITION'",
                DiagnosticSeverity.Error, MicroProfileGraphQLConstants.DIAGNOSTIC_SOURCE,
                MicroProfileGraphQLErrorCode.WRONG_DIRECTIVE_PLACEMENT);

        Diagnostic d4 = d(101, 11, 17,
                "Methods annotated with `@Subscription` have to return either `io.smallrye.mutiny.Multi` or `java.util.concurrent.Flow.Publisher`.",
                DiagnosticSeverity.Error, MicroProfileGraphQLConstants.DIAGNOSTIC_SOURCE,
                MicroProfileGraphQLErrorCode.SUBSCRIPTION_MUST_RETURN_MULTI);

        Diagnostic d5 = d(106, 11, 33,
                "Methods annotated with `@Query` or `@Mutation` cannot return `io.smallrye.mutiny.Multi` or `java.util.concurrent.Flow.Publisher`.",
                DiagnosticSeverity.Error, MicroProfileGraphQLConstants.DIAGNOSTIC_SOURCE,
                MicroProfileGraphQLErrorCode.SINGLE_RESULT_OPERATION_MUST_NOT_RETURN_MULTI);

        assertJavaDiagnostics(diagnosticsParams, utils,
                d1, d2, d3, d4, d5);
    }

}