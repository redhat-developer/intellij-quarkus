/*******************************************************************************
* Copyright (c) 2020 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.metrics.java;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.metrics.MicroProfileMetricsConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.metrics.java.MicroProfileMetricsErrorCode;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.assertJavaDiagnostics;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.createCodeActionParams;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.d;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.fixURI;

/**
 * Java diagnostics and code action for MicroProfile Metrics.
 * 
 * @author Kathryn Kodama
 */
public class JavaDiagnosticsMicroProfileMetricsTest extends MavenModuleImportingTestCase {

	@Test
	public void testApplicationScopedAnnotationMissing() throws Exception {
		Module module = createMavenModule("microprofile-metrics", new File("projects/maven/microprofile-metrics"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(module), "src/main/java/org/acme/IncorrectScope.java").toURI());
		diagnosticsParams.setUris(Arrays.asList(javaFileUri.toString()));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		// check for MicroProfile metrics diagnostic warning
		Diagnostic d = d(10, 13, 27,
				"The class `org.acme.IncorrectScope` using the @Gauge annotation should use the @ApplicationScoped annotation." + 
				" The @Gauge annotation does not support multiple instances of the underlying bean to be created.",
				DiagnosticSeverity.Warning, MicroProfileMetricsConstants.DIAGNOSTIC_SOURCE,
				MicroProfileMetricsErrorCode.ApplicationScopedAnnotationMissing);
		assertJavaDiagnostics(diagnosticsParams, utils, d);

		String uri = javaFileUri;
		MicroProfileJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
		// check for MicroProfile metrics quick fix code action associated with diagnostic warning
		/*assertJavaCodeAction(codeActionParams, utils, //
			ca(uri, "Replace current scope with @ApplicationScoped", d, //
				te(4, 57, 9, 0, "\n\nimport javax.enterprise.context.ApplicationScoped;\n" + //
					"import javax.enterprise.context.RequestScoped;\n\n" + //
					"@ApplicationScoped\n")));*/
	}
	
}