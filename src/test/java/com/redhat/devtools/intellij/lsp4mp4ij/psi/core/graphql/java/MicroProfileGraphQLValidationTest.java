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

import java.util.Arrays;
import java.util.Collections;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.*;

/**
 * Tests for {@link com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.graphql.java.MicroProfileGraphQLASTValidator}.
 */
public class MicroProfileGraphQLValidationTest extends LSP4MPMavenModuleImportingTestCase {

	@Test
	public void testVoidMethods() throws Exception {
		Module javaProject = loadMavenProject(MicroProfileMavenProjectName.microprofile_graphql);
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		String javaFileUri = getFileUri("/src/main/java/io/openliberty/graphql/sample/WeatherService.java", javaProject);
		diagnosticsParams.setUris(Collections.singletonList(javaFileUri));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d1 = d(89, 11, 15,
				"Methods annotated with microprofile-graphql's `@Query` cannot have 'void' as a return type.",
				DiagnosticSeverity.Error, MicroProfileGraphQLConstants.DIAGNOSTIC_SOURCE,
				MicroProfileGraphQLErrorCode.NO_VOID_QUERIES);

		Diagnostic d2 = d(93, 11, 15,
				"Methods annotated with microprofile-graphql's `@Mutation` cannot have 'void' as a return type.",
				DiagnosticSeverity.Error, MicroProfileGraphQLConstants.DIAGNOSTIC_SOURCE,
				MicroProfileGraphQLErrorCode.NO_VOID_MUTATIONS);

		assertJavaDiagnostics(diagnosticsParams, utils, //
				d1, d2);
	}
}