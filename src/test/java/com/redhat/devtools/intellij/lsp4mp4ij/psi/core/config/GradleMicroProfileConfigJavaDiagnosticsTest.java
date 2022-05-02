/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.config;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.redhat.devtools.intellij.GradleTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.metrics.MicroProfileMetricsConstants;
import org.apache.commons.io.FileUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.assertJavaDiagnostics;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.d;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.fixURI;

/**
 * JDT Quarkus manager test for hover in Java file.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/JavaHoverTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/JavaHoverTest.java</a>
 *
 */
public class GradleMicroProfileConfigJavaDiagnosticsTest extends GradleTestCase {

	private Module loadProject(String name) throws IOException {
		FileUtils.copyDirectory(new File("projects/gradle/" + name), new File(getProjectPath()));
		importProject();
		return getModule(name + ".main");
	}

	@Test
	public void testImproperDefaultValues() throws Exception {
		Module javaProject = loadProject("config-quickstart");
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/config/DefaultValueResource.java").toURI());
		diagnosticsParams.setUris(Arrays.asList(javaFileUri.toString()));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d1 = d(8, 53, 58, "'foo' does not match the expected type of 'int'.",
				DiagnosticSeverity.Error, MicroProfileMetricsConstants.DIAGNOSTIC_SOURCE, null);

		Diagnostic d2 = d(11, 53, 58, "'bar' does not match the expected type of 'Integer'.",
				DiagnosticSeverity.Error, MicroProfileMetricsConstants.DIAGNOSTIC_SOURCE, null);

		Diagnostic d3 = d(17, 53, 58, "'128' does not match the expected type of 'byte'.",
				DiagnosticSeverity.Error, MicroProfileMetricsConstants.DIAGNOSTIC_SOURCE, null);

		assertJavaDiagnostics(diagnosticsParams, utils, //
				d1, d2, d3);
	}
}
