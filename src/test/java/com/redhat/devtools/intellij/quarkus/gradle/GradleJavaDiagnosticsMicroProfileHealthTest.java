/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.gradle;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.search.PsiUtilsImpl;
import com.redhat.devtools.intellij.quarkus.search.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.quarkus.search.internal.health.MicroProfileHealthConstants;
import com.redhat.devtools.intellij.quarkus.search.internal.health.java.MicroProfileHealthErrorCode;
import org.apache.commons.io.FileUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static com.redhat.devtools.intellij.quarkus.module.MicroProfileForJavaAssert.assertJavaDiagnostics;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileForJavaAssert.d;

/**
 * Test the availability of the MicroProfile Metrics properties
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/health/JavaDiagnosticsMicroProfileHealthTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/health/JavaDiagnosticsMicroProfileHealthTest.java</a>
 *
 */
public class GradleJavaDiagnosticsMicroProfileHealthTest extends GradleTestCase {
	private static final IPsiUtils utils = PsiUtilsImpl.getInstance();

	@Override
	public void setUp() throws Exception {
		super.setUp();
		FileUtils.copyDirectory(new File("projects/gradle/microprofile-health-quickstart"), new File(getProjectPath()));
		importProject();
	}

	@Test
	public void testImplementHealthCheck() throws Exception {
		Module module = getModule("microprofile-health-quickstart.main");
		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/java/org/acme/health/DontImplementHealthCheck.java");

		String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();
		diagnosticsParams.setUris(Arrays.asList(uri));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d = d(9, 13, 37,
				"The class `org.acme.health.DontImplementHealthCheck` using the @Liveness, @Readiness, or @Health annotation should implement the HealthCheck interface.",
				DiagnosticSeverity.Warning, MicroProfileHealthConstants.DIAGNOSTIC_SOURCE,
				MicroProfileHealthErrorCode.ImplementHealthCheck);
		assertJavaDiagnostics(diagnosticsParams, utils, //
				d);

		/*String uri = javaFile.getUrl();
		MicroProfileJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
		assertJavaCodeAction(codeActionParams, utils, //
				ca(uri, "Let 'DontImplementHealthCheck' implement 'org.eclipse.microprofile.health.HealthCheck'", d, //
						te(2, 50, 9, 37, "\r\n\r\n" + //
								"import org.eclipse.microprofile.health.HealthCheck;\r\n" + //
								"import org.eclipse.microprofile.health.HealthCheckResponse;\r\n" + //
								"import org.eclipse.microprofile.health.Liveness;\r\n\r\n@Liveness\r\n" + //
								"@ApplicationScoped\r\n" + //
								"public class DontImplementHealthCheck implements HealthCheck")));*/
	}

	@Test
	public void testHealthAnnotationMissing() throws Exception {
		Module module = getModule("microprofile-health-quickstart.main");
		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/java/org/acme/health/ImplementHealthCheck.java");
		String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();
		diagnosticsParams.setUris(Arrays.asList(uri));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d = d(5, 13, 33,
				"The class `org.acme.health.ImplementHealthCheck` implementing the HealthCheck interface should use the @Liveness, @Readiness, or @Health annotation.",
				DiagnosticSeverity.Warning, MicroProfileHealthConstants.DIAGNOSTIC_SOURCE,
				MicroProfileHealthErrorCode.HealthAnnotationMissing);
		assertJavaDiagnostics(diagnosticsParams, utils, //
				d);

		/*MicroProfileJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
		assertJavaCodeAction(codeActionParams, utils, //
				ca(uri, "Insert @Health", d, //
						te(2, 0, 5, 0, "import org.eclipse.microprofile.health.Health;\r\n" + //
								"import org.eclipse.microprofile.health.HealthCheck;\r\n" + //
								"import org.eclipse.microprofile.health.HealthCheckResponse;\r\n\r\n" + //
								"@Health\r\n")),
				ca(uri, "Insert @Liveness", d, //
						te(3, 59, 5, 0, "\r\n" + //
								"import org.eclipse.microprofile.health.Liveness;\r\n\r\n" + //
								"@Liveness\r\n")), //
				ca(uri, "Insert @Readiness", d, //
						te(3, 59, 5, 0, "\r\n" + //
								"import org.eclipse.microprofile.health.Readiness;\r\n\r\n" + //
								"@Readiness\r\n")) //
		);*/
	}
}
