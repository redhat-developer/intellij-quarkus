/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.config.java;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.config.java.MicroProfileConfigErrorCode;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsSettings;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.assertJavaDiagnostics;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.d;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.fixURI;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.config.java.MicroProfileConfigASTValidator.setDataForUnassigned;

/**
 * JDT Quarkus manager test for hover in Java file.
 * 
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/JavaHoverTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/JavaHoverTest.java</a>
 *
 */
public class MicroProfileConfigJavaDiagnosticsTest extends MavenModuleImportingTestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Test
	public void testImproperDefaultValues() throws Exception {
		Module javaProject = createMavenModule("config-quickstart", new File("projects/maven/config-quickstart"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/config/DefaultValueResource.java").toURI());
		diagnosticsParams.setUris(Arrays.asList(javaFileUri.toString()));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		diagnosticsParams.setUris(Arrays.asList(javaFileUri));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d1 = d(8, 53, 58, "'foo' does not match the expected type of 'int'.", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		Diagnostic d2 = d(11, 53, 58, "'bar' does not match the expected type of 'Integer'.", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		Diagnostic d3 = d(17, 53, 58, "'128' does not match the expected type of 'byte'.", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);
		Diagnostic d4 = d(32, 27, 38,
				"The property 'greeting9' is not assigned a value in any config file, and must be assigned at runtime.",
				DiagnosticSeverity.Warning, MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.NO_VALUE_ASSIGNED_TO_PROPERTY);
		setDataForUnassigned("greeting9", d4);

		assertJavaDiagnostics(diagnosticsParams, utils, //
				d1, d2, d3, d4);
	}

	@Test
	public void testNoValueAssignedWithIgnore() throws Exception {
		Module javaProject = createMavenModule("config-quickstart", new File("projects/maven/config-quickstart"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/config/DefaultValueResource.java").toURI());
		diagnosticsParams.setSettings(new MicroProfileJavaDiagnosticsSettings(Arrays.asList("greeting?")));
		diagnosticsParams.setUris(Arrays.asList(javaFileUri));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d1 = d(8, 53, 58, "'foo' does not match the expected type of 'int'.", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		Diagnostic d2 = d(11, 53, 58, "'bar' does not match the expected type of 'Integer'.", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		Diagnostic d3 = d(17, 53, 58, "'128' does not match the expected type of 'byte'.", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		assertJavaDiagnostics(diagnosticsParams, utils, //
				d1, d2, d3);
	}

	@Test
	public void testUnassignedWithConfigproperties() throws Exception {
		Module javaProject = createMavenModule("microprofile-config", new File("projects/maven/microprofile-configproperties"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/Details.java").toURI());
		diagnosticsParams.setUris(Arrays.asList(javaFileUri));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d = d(13, 32, 46,
				"The property 'server.old.location' is not assigned a value in any config file, and must be assigned at runtime.",
				DiagnosticSeverity.Warning, MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.NO_VALUE_ASSIGNED_TO_PROPERTY);
		setDataForUnassigned("server.old.location", d);

		assertJavaDiagnostics(diagnosticsParams, utils, d);
	}

	@Test
	public void testEmptyNameKeyValue() throws Exception {
		Module javaProject = createMavenModule("microprofile-config", new File("projects/maven/microprofile-configproperties"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/EmptyKey.java").toURI());
		diagnosticsParams.setUris(Arrays.asList(javaFileUri));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d1 = d(5, 25, 27, "The member 'name' can't be empty.", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.EMPTY_KEY);

		assertJavaDiagnostics(diagnosticsParams, utils, d1);
	}
}
