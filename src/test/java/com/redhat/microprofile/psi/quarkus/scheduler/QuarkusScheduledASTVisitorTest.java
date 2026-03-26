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
package com.redhat.microprofile.psi.quarkus.scheduler;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.microprofile.psi.internal.quarkus.scheduler.SchedulerErrorCodes;
import com.redhat.microprofile.psi.quarkus.QuarkusMavenModuleImportingTestCase;
import com.redhat.microprofile.psi.quarkus.QuarkusMavenProjectName;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.*;
import static com.redhat.microprofile.psi.internal.quarkus.QuarkusConstants.QUARKUS_PREFIX;

/**
 * Quarkus @Scheduled annotation property test for diagnostics in Java file.
 */
public class QuarkusScheduledASTVisitorTest extends QuarkusMavenModuleImportingTestCase {

	@Test
	public void testScheduledAnnotationTest() throws Exception {

		Module javaProject = loadMavenProject(QuarkusMavenProjectName.scheduler_diagnostic);
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();

		String javaFileUri = getFileUri("src/main/java/org/acme/ScheduledResource.java", javaProject);
		diagnosticsParams.setUris(Arrays.asList(javaFileUri));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		// cron member diagnostics

		Diagnostic d1 = d(42, 22, 24, SchedulerErrorCodes.INVALID_CRON_LENGTH.getErrorMessage(),
				DiagnosticSeverity.Warning, QUARKUS_PREFIX, SchedulerErrorCodes.INVALID_CRON_LENGTH);

		Diagnostic d2 = d(45, 22, 39, SchedulerErrorCodes.INVALID_CRON_LENGTH.getErrorMessage(),
				DiagnosticSeverity.Warning, QUARKUS_PREFIX, SchedulerErrorCodes.INVALID_CRON_LENGTH);

		Diagnostic d3 = d(48, 22, 36, SchedulerErrorCodes.INVALID_CRON_SECOND.getErrorMessage(),
				DiagnosticSeverity.Warning, QUARKUS_PREFIX, SchedulerErrorCodes.INVALID_CRON_SECOND);

		Diagnostic d4 = d(51, 22, 36, SchedulerErrorCodes.INVALID_CRON_MINUTE.getErrorMessage(),
				DiagnosticSeverity.Warning, QUARKUS_PREFIX, SchedulerErrorCodes.INVALID_CRON_MINUTE);

		Diagnostic d5 = d(54, 22, 36, SchedulerErrorCodes.INVALID_CRON_HOUR.getErrorMessage(),
				DiagnosticSeverity.Warning, QUARKUS_PREFIX, SchedulerErrorCodes.INVALID_CRON_HOUR);

		Diagnostic d6 = d(57, 22, 36, SchedulerErrorCodes.INVALID_CRON_DAY_OF_MONTH.getErrorMessage(),
				DiagnosticSeverity.Warning, QUARKUS_PREFIX, SchedulerErrorCodes.INVALID_CRON_DAY_OF_MONTH);

		Diagnostic d7 = d(60, 22, 36, SchedulerErrorCodes.INVALID_CRON_MONTH.getErrorMessage(),
				DiagnosticSeverity.Warning, QUARKUS_PREFIX, SchedulerErrorCodes.INVALID_CRON_MONTH);

		Diagnostic d8 = d(63, 22, 41, SchedulerErrorCodes.INVALID_CRON_MONTH.getErrorMessage(),
				DiagnosticSeverity.Warning, QUARKUS_PREFIX, SchedulerErrorCodes.INVALID_CRON_MONTH);

		Diagnostic d9 = d(66, 22, 35, SchedulerErrorCodes.INVALID_CRON_DAY_OF_WEEK.getErrorMessage(),
				DiagnosticSeverity.Warning, QUARKUS_PREFIX, SchedulerErrorCodes.INVALID_CRON_DAY_OF_WEEK);

		Diagnostic d10 = d(69, 22, 40, SchedulerErrorCodes.INVALID_CRON_DAY_OF_WEEK.getErrorMessage(),
				DiagnosticSeverity.Warning, QUARKUS_PREFIX, SchedulerErrorCodes.INVALID_CRON_DAY_OF_WEEK);

		Diagnostic d11 = d(72, 22, 40, SchedulerErrorCodes.INVALID_CRON_YEAR.getErrorMessage(),
				DiagnosticSeverity.Warning, QUARKUS_PREFIX, SchedulerErrorCodes.INVALID_CRON_YEAR);

		Diagnostic d12 = d(75, 22, 43, SchedulerErrorCodes.INVALID_CHAR_IN_EXPRESSION.getErrorMessage(),
				DiagnosticSeverity.Warning, QUARKUS_PREFIX, SchedulerErrorCodes.INVALID_CHAR_IN_EXPRESSION);

		// every member diagnostics

		Diagnostic d13 = d(87, 23, 27, SchedulerErrorCodes.INVALID_DURATION_PARSE_PATTERN.getErrorMessage(),
				DiagnosticSeverity.Warning, QUARKUS_PREFIX, SchedulerErrorCodes.INVALID_DURATION_PARSE_PATTERN);

		Diagnostic d14 = d(90, 23, 31, SchedulerErrorCodes.INVALID_DURATION_PARSE_PATTERN.getErrorMessage(),
				DiagnosticSeverity.Warning, QUARKUS_PREFIX, SchedulerErrorCodes.INVALID_DURATION_PARSE_PATTERN);

		Diagnostic d15 = d(93, 23, 39, SchedulerErrorCodes.INVALID_CHAR_IN_EXPRESSION.getErrorMessage(),
				DiagnosticSeverity.Warning, QUARKUS_PREFIX, SchedulerErrorCodes.INVALID_CHAR_IN_EXPRESSION);

		// delayed member diagnostics

		Diagnostic d16 = d(105, 25, 29, SchedulerErrorCodes.INVALID_DURATION_PARSE_PATTERN.getErrorMessage(),
				DiagnosticSeverity.Warning, QUARKUS_PREFIX, SchedulerErrorCodes.INVALID_DURATION_PARSE_PATTERN);

		Diagnostic d17 = d(108, 25, 33, SchedulerErrorCodes.INVALID_DURATION_PARSE_PATTERN.getErrorMessage(),
				DiagnosticSeverity.Warning, QUARKUS_PREFIX, SchedulerErrorCodes.INVALID_DURATION_PARSE_PATTERN);

		Diagnostic d18 = d(111, 25, 43, SchedulerErrorCodes.INVALID_CHAR_IN_EXPRESSION.getErrorMessage(),
				DiagnosticSeverity.Warning, QUARKUS_PREFIX, SchedulerErrorCodes.INVALID_CHAR_IN_EXPRESSION);

		assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14,
				d15, d16, d17, d18);

	}
}