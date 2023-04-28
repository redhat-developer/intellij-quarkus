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
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.metrics.MicroProfileMetricsConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.metrics.java.MicroProfileMetricsErrorCode;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.restclient.MicroProfileRestClientConstants;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.assertJavaCodeAction;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.assertJavaDiagnostics;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.ca;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.createCodeActionParams;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.d;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.fixURI;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.te;

/**
 * Java diagnostics and code action for MicroProfile Metrics.
 * 
 * @author Kathryn Kodama
 */
public class MicroProfileMetricsJavaDiagnosticsTest extends MavenModuleImportingTestCase {

	@Test
	public void testApplicationScopedAnnotationMissing() throws Exception {
		Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-metrics"));
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
		assertJavaCodeAction(codeActionParams, utils, //
			ca(uri, "Replace current scope with @ApplicationScoped", d, //
				te(0, 0, 18, 0, "package org.acme;\n\nimport javax.enterprise.context.ApplicationScoped;\nimport javax.ws.rs.Path;\n\nimport org.eclipse.microprofile.metrics.MetricUnits;\nimport org.eclipse.microprofile.metrics.annotation.Gauge;\n\n@ApplicationScoped\n@Path(\"/\")\npublic class IncorrectScope {\n\n    @Gauge(name = \"Return Int\", unit = MetricUnits.NONE, description = \"Test method for Gauge annotation\")\n    public int returnInt() {\n        return 2;\n    }\n\n}\n")));
	}

	@Test
	public void testApplicationScopedAnnotationMissingJakarta() throws Exception {
		Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/open-liberty"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/java/com/demo/rest/IncorrectScopeJakarta.java");

		String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();
		diagnosticsParams.setUris(Arrays.asList(uri));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		// check for MicroProfile metrics diagnostic warning
		Diagnostic d1 = d(11, 13, 34,
				"The class `com.demo.rest.IncorrectScopeJakarta` using the @Gauge annotation should use the @ApplicationScoped annotation." +
						" The @Gauge annotation does not support multiple instances of the underlying bean to be created.",
				DiagnosticSeverity.Warning, MicroProfileMetricsConstants.DIAGNOSTIC_SOURCE,
				MicroProfileMetricsErrorCode.ApplicationScopedAnnotationMissing);
		Diagnostic d2 = d(15, 18, 26,
				"The corresponding `com.demo.rest.MyService` interface does not have the @RegisterRestClient annotation. The field `service1` will not be injected as a CDI bean.",
				DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE, null);
		assertJavaDiagnostics(diagnosticsParams, utils, d2, d1);

		/* String uri = javaFile.getLocation().toFile().toURI().toString(); */
		MicroProfileJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d1);
		// check for MicroProfile metrics quick fix code action associated with
		// diagnostic warning
		assertJavaCodeAction(codeActionParams, utils, //
				ca(uri, "Replace current scope with @ApplicationScoped", d1, //
						te(0, 0, 21, 1, "package com.demo.rest;\n\nimport jakarta.enterprise.context.ApplicationScoped;\nimport jakarta.ws.rs.Path;\nimport org.eclipse.microprofile.metrics.MetricUnits;\nimport org.eclipse.microprofile.metrics.annotation.Gauge;\nimport org.eclipse.microprofile.rest.client.inject.RestClient;\nimport jakarta.inject.Inject;\n\n@ApplicationScoped\n@Path(\"/\")\npublic class IncorrectScopeJakarta {\n\n    @Inject\n    @RestClient\n    public MyService service1;\n\n    @Gauge(name = \"Return Int\", unit = MetricUnits.NONE, description = \"Test method for Gauge annotation\")\n    public int returnInt() {\n        return 2;\n    }\n}")),
				ca(uri, "Generate OpenAPI Annotations for 'IncorrectScopeJakarta'", d1, // No @Operation should be added
						te(0, 0, 21, 1, "package com.demo.rest;\n\nimport jakarta.ws.rs.Path;\nimport org.eclipse.microprofile.metrics.MetricUnits;\nimport org.eclipse.microprofile.metrics.annotation.Gauge;\nimport org.eclipse.microprofile.rest.client.inject.RestClient;\nimport jakarta.inject.Inject;\nimport jakarta.enterprise.context.RequestScoped;\n\n@RequestScoped\n@Path(\"/\")\npublic class IncorrectScopeJakarta {\n\n    @Inject\n    @RestClient\n    public MyService service1;\n\n    @Gauge(name = \"Return Int\", unit = MetricUnits.NONE, description = \"Test method for Gauge annotation\")\n    public int returnInt() {\n        return 2;\n    }\n}")));
	}
	
}