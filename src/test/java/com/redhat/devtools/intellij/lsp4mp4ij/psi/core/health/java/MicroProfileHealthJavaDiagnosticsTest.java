/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.health.java;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.health.MicroProfileHealthConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.health.java.MicroProfileHealthErrorCode;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.assertJavaCodeAction;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.assertJavaDiagnostics;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.ca;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.d;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.te;

/**
 * Java diagnostics and code action for MicroProfile Health.
 *
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/health/JavaDiagnosticsMicroProfileHealthTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/health/JavaDiagnosticsMicroProfileHealthTest.java</a>
 *
 */
public class MicroProfileHealthJavaDiagnosticsTest extends MavenModuleImportingTestCase {
	@Test
	public void testImplementHealthCheck() throws Exception {

		Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-health-quickstart"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/java/org/acme/health/DontImplementHealthCheck.java");

		String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();
		diagnosticsParams.setUris(Arrays.asList(uri));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d = d(9, 13, 37,
				"The class `org.acme.health.DontImplementHealthCheck` using the @Liveness, @Readiness, or @Health annotation should implement the HealthCheck interface.",
				DiagnosticSeverity.Warning, MicroProfileHealthConstants.DIAGNOSTIC_SOURCE,
				MicroProfileHealthErrorCode.ImplementHealthCheck);
		assertJavaDiagnostics(diagnosticsParams, PsiUtilsLSImpl.getInstance(myProject), //
				d);

		/*String uri = javaFile.getUrl();*/
		MicroProfileJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
		codeActionParams.setResourceOperationSupported(true);
		codeActionParams.setCommandConfigurationUpdateSupported(true);
		assertJavaCodeAction(codeActionParams, utils, //
				ca(uri, "Let 'DontImplementHealthCheck' implement '@HealthCheck'", d, //
						te(0, 0, 17, 0, "package org.acme.health;\n\nimport javax.enterprise.context.ApplicationScoped;\n\nimport org.eclipse.microprofile.health.HealthCheck;\nimport org.eclipse.microprofile.health.HealthCheckResponse;\nimport org.eclipse.microprofile.health.Liveness;\n\n@Liveness\n@ApplicationScoped\npublic class DontImplementHealthCheck implements HealthCheck {\n\n    public HealthCheckResponse call() {\n        return null;\n    }\n\n} \n")));
	}

	@Test
	public void testHealthAnnotationMissing() throws Exception {

		Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-health-quickstart"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/java/org/acme/health/ImplementHealthCheck.java");
		String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();
		diagnosticsParams.setUris(Arrays.asList(uri));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d = d(5, 13, 33,
				"The class `org.acme.health.ImplementHealthCheck` implementing the HealthCheck interface should use the @Liveness, @Readiness, or @Health annotation.",
				DiagnosticSeverity.Warning, MicroProfileHealthConstants.DIAGNOSTIC_SOURCE,
				MicroProfileHealthErrorCode.HealthAnnotationMissing);
		assertJavaDiagnostics(diagnosticsParams, PsiUtilsLSImpl.getInstance(myProject), //
				d);

		MicroProfileJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
		assertJavaCodeAction(codeActionParams, utils, //
				ca(uri, "Insert @Health", d, //
						te(0, 0, 15, 0, "package org.acme.health;\n\nimport org.eclipse.microprofile.health.Health;\nimport org.eclipse.microprofile.health.HealthCheck;\nimport org.eclipse.microprofile.health.HealthCheckResponse;\n\n@Health\npublic class ImplementHealthCheck implements HealthCheck {\n\n    @Override\n    public HealthCheckResponse call() {\n        return null;\n    }\n\n}\n")),
				ca(uri, "Insert @Liveness", d, //
						te(0, 0, 15, 0, "package org.acme.health;\n\nimport org.eclipse.microprofile.health.HealthCheck;\nimport org.eclipse.microprofile.health.HealthCheckResponse;\nimport org.eclipse.microprofile.health.Liveness;\n\n@Liveness\npublic class ImplementHealthCheck implements HealthCheck {\n\n    @Override\n    public HealthCheckResponse call() {\n        return null;\n    }\n\n}\n")), //
				ca(uri, "Insert @Readiness", d, //
						te(0, 0, 15, 0, "package org.acme.health;\n\nimport org.eclipse.microprofile.health.HealthCheck;\nimport org.eclipse.microprofile.health.HealthCheckResponse;\nimport org.eclipse.microprofile.health.Readiness;\n\n@Readiness\npublic class ImplementHealthCheck implements HealthCheck {\n\n    @Override\n    public HealthCheckResponse call() {\n        return null;\n    }\n\n}\n")) //
		);
	}

	private MicroProfileJavaCodeActionParams createCodeActionParams(String uri, Diagnostic d) {
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Range range = d.getRange();
		CodeActionContext context = new CodeActionContext();
		context.setDiagnostics(Arrays.asList(d));
		MicroProfileJavaCodeActionParams codeActionParams = new MicroProfileJavaCodeActionParams(textDocument, range,
				context);
		codeActionParams.setResourceOperationSupported(true);
		return codeActionParams;
	}

	@Test
	public void testHealthAnnotationMissingv3() throws Exception {
		Module javaProject = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-health-3"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(javaProject) + "/src/main/java/org/acme/MyLivenessCheck.java");
		String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();
		diagnosticsParams.setUris(Arrays.asList(uri));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d = d(5, 13, 28,
				"The class `org.acme.MyLivenessCheck` implementing the HealthCheck interface should use the @Liveness or @Readiness annotation.",
				DiagnosticSeverity.Warning, MicroProfileHealthConstants.DIAGNOSTIC_SOURCE,
				MicroProfileHealthErrorCode.HealthAnnotationMissing);
		assertJavaDiagnostics(diagnosticsParams, utils, //
				d);

		//String uri = javaFile.getLocation().toFile().toURI().toString();
		MicroProfileJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
		assertJavaCodeAction(codeActionParams, utils, //
				ca(uri, "Insert @Liveness", d, //
						te(0, 0, 15, 0, "package org.acme;\n\nimport org.eclipse.microprofile.health.HealthCheck;\nimport org.eclipse.microprofile.health.HealthCheckResponse;\nimport org.eclipse.microprofile.health.Liveness;\n\n@Liveness\npublic class MyLivenessCheck implements HealthCheck {\n\n    @Override\n    public HealthCheckResponse call() {\n        return HealthCheckResponse.up(\"alive\");\n    }\n\n}\n")), //
				ca(uri, "Insert @Readiness", d, //
						te(0, 0, 15, 0, "package org.acme;\n\nimport org.eclipse.microprofile.health.HealthCheck;\nimport org.eclipse.microprofile.health.HealthCheckResponse;\nimport org.eclipse.microprofile.health.Readiness;\n\n@Readiness\npublic class MyLivenessCheck implements HealthCheck {\n\n    @Override\n    public HealthCheckResponse call() {\n        return HealthCheckResponse.up(\"alive\");\n    }\n\n}\n")), //
				ca(uri, "Generate OpenAPI Annotations for 'MyLivenessCheck'", d, //
						te(0, 0, 15, 0, "package org.acme;\n\nimport org.eclipse.microprofile.health.HealthCheck;\nimport org.eclipse.microprofile.health.HealthCheckResponse;\nimport org.eclipse.microprofile.openapi.annotations.Operation;\n\npublic class MyLivenessCheck implements HealthCheck {\n\n    @Operation(summary = \"\", description = \"\")\n    @Override\n    public HealthCheckResponse call() {\n        return HealthCheckResponse.up(\"alive\");\n    }\n\n}\n")) //
		);
	}
}
