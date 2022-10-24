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
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.config.java.MicroProfileConfigErrorCode;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.providers.MicroProfileConfigSourceProvider;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileCodeActionFactory;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsSettings;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.saveFile;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.assertJavaCodeAction;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.assertJavaDiagnostics;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.ca;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.createCodeActionParams;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.d;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.fixURI;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.te;
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
	public void testCodeActionForUnassigned() throws Exception {
		Module javaProject = createMavenModule("config-quickstart", new File("projects/maven/config-quickstart"));
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		saveFile(MicroProfileConfigSourceProvider.MICROPROFILE_CONFIG_PROPERTIES_FILE, "", javaProject);

		File propertiesFile = new File(ModuleUtilCore.getModuleDirPath(javaProject),
				"src/main/resources/META-INF/microprofile-config.properties");

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		File javaFile = new File(ModuleUtilCore.getModuleDirPath(javaProject),
				"src/main/java/org/acme/config/UnassignedValue.java");
		String javaFileUri = fixURI(javaFile.toURI());
		diagnosticsParams.setUris(Arrays.asList(javaFileUri));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d1 = d(8, 24, 29,
				"The property 'foo' is not assigned a value in any config file, and must be assigned at runtime.",
				DiagnosticSeverity.Warning, MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.NO_VALUE_ASSIGNED_TO_PROPERTY);
		setDataForUnassigned("foo", d1);
		Diagnostic d2 = d(14, 25, 30,
				"The property 'server.url' is not assigned a value in any config file, and must be assigned at runtime.",
				DiagnosticSeverity.Warning, MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.NO_VALUE_ASSIGNED_TO_PROPERTY);
		setDataForUnassigned("server.url", d2);

		assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);

		String javaUri = fixURI(javaFile.toURI());
		String propertiesUri = fixURI(propertiesFile.toURI());

		String lineSeparator = CodeStyleSettingsManager.getInstance(javaProject.getProject()).getMainProjectCodeStyle().
				getLineSeparator();
		if (lineSeparator == null) {
			lineSeparator = System.lineSeparator();
		}

		MicroProfileJavaCodeActionParams codeActionParams1 = createCodeActionParams(javaUri, d1);
		assertJavaCodeAction(codeActionParams1, utils, //
				ca(javaUri, "Insert 'defaultValue' attribute", d1, //
						te(0, 0, 18, 0, "package org.acme.config;\n\nimport org.eclipse.microprofile.config.inject.ConfigProperty;\n\nimport io.quarkus.arc.config.ConfigProperties;\n\npublic class UnassignedValue {\n\n    @ConfigProperty(name = \"foo\", defaultValue = \"\")\n    private String foo;\n\n    @ConfigProperties(prefix = \"server\")\n    private class Server {\n\n        @ConfigProperty(name = \"url\")\n        private String url;\n    }\n}\n")),
				ca(propertiesUri, "Insert 'foo' property in 'META-INF/microprofile-config.properties'", d1, //
						te(0, 0, 0, 0, "foo=" + lineSeparator)));

		MicroProfileJavaCodeActionParams codeActionParams2 = createCodeActionParams(javaUri, d2);
		assertJavaCodeAction(codeActionParams2, utils, //
				ca(javaUri, "Insert 'defaultValue' attribute", d2, //
						te(0, 0, 18, 0, "package org.acme.config;\n\nimport org.eclipse.microprofile.config.inject.ConfigProperty;\n\nimport io.quarkus.arc.config.ConfigProperties;\n\npublic class UnassignedValue {\n\n    @ConfigProperty(name = \"foo\")\n    private String foo;\n\n    @ConfigProperties(prefix = \"server\")\n    private class Server {\n\n        @ConfigProperty(name = \"url\", defaultValue = \"\")\n        private String url;\n    }\n}\n")),
				ca(propertiesUri, "Insert 'server.url' property in 'META-INF/microprofile-config.properties'", d2, //
						te(0, 0, 0, 0, "server.url=" + lineSeparator)));

		// Same code actions but with exclude
		Diagnostic d1_1 = d(8, 24, 29,
				"The property 'foo' is not assigned a value in any config file, and must be assigned at runtime.",
				DiagnosticSeverity.Warning, MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.NO_VALUE_ASSIGNED_TO_PROPERTY);
		setDataForUnassigned("foo", d1_1);
		Diagnostic d2_1 = d(14, 25, 30,
				"The property 'server.url' is not assigned a value in any config file, and must be assigned at runtime.",
				DiagnosticSeverity.Warning, MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.NO_VALUE_ASSIGNED_TO_PROPERTY);
		setDataForUnassigned("server.url", d2_1);

		MicroProfileJavaCodeActionParams codeActionParams1_1 = createCodeActionParams(javaUri, d1_1);
		codeActionParams1_1.setCommandConfigurationUpdateSupported(true);
		assertJavaCodeAction(codeActionParams1_1, utils, //
				MicroProfileCodeActionFactory.createAddToUnassignedExcludedCodeAction("foo", d1_1),
				ca(javaUri, "Insert 'defaultValue' attribute", d1_1, //
						te(0, 0, 18, 0, "package org.acme.config;\n\nimport org.eclipse.microprofile.config.inject.ConfigProperty;\n\nimport io.quarkus.arc.config.ConfigProperties;\n\npublic class UnassignedValue {\n\n    @ConfigProperty(name = \"foo\", defaultValue = \"\")\n    private String foo;\n\n    @ConfigProperties(prefix = \"server\")\n    private class Server {\n\n        @ConfigProperty(name = \"url\")\n        private String url;\n    }\n}\n")),
				ca(propertiesUri, "Insert 'foo' property in 'META-INF/microprofile-config.properties'", d1_1, //
						te(0, 0, 0, 0, "foo=" + lineSeparator)));

		MicroProfileJavaCodeActionParams codeActionParams2_1 = createCodeActionParams(javaUri, d2_1);
		codeActionParams2_1.setCommandConfigurationUpdateSupported(true);
		assertJavaCodeAction(codeActionParams2_1, utils, //
				MicroProfileCodeActionFactory.createAddToUnassignedExcludedCodeAction("server.url", d2_1),
				ca(javaUri, "Insert 'defaultValue' attribute", d2_1, //
						te(0, 0, 18, 0, "package org.acme.config;\n\nimport org.eclipse.microprofile.config.inject.ConfigProperty;\n\nimport io.quarkus.arc.config.ConfigProperties;\n\npublic class UnassignedValue {\n\n    @ConfigProperty(name = \"foo\")\n    private String foo;\n\n    @ConfigProperties(prefix = \"server\")\n    private class Server {\n\n        @ConfigProperty(name = \"url\", defaultValue = \"\")\n        private String url;\n    }\n}\n")),
				ca(propertiesUri, "Insert 'server.url' property in 'META-INF/microprofile-config.properties'", d2_1, //
						te(0, 0, 0, 0, "server.url=" + lineSeparator)));

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
