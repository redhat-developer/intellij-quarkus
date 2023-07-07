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

import com.intellij.openapi.module.LoadedModuleDescription;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.LSP4MPMavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileMavenProjectName;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.config.java.MicroProfileConfigErrorCode;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.providers.MicroProfileConfigSourceProvider;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.codeaction.MicroProfileCodeActionFactory;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsSettings;
import org.eclipse.lsp4mp.commons.codeaction.MicroProfileCodeActionId;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.saveFile;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.*;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.config.java.MicroProfileConfigASTValidator.setDataForUnassigned;

/**
 * JDT Quarkus manager test for hover in Java file.
 * 
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/JavaHoverTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/JavaHoverTest.java</a>
 *
 */
public class MicroProfileConfigJavaDiagnosticsTest extends LSP4MPMavenModuleImportingTestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Test
	public void testImproperDefaultValues() throws Exception {
		Module javaProject = loadMavenProject(MicroProfileMavenProjectName.config_quickstart);
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		String javaFileUri = getFileUri("src/main/java/org/acme/config/DefaultValueResource.java", javaProject);
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

		Diagnostic d5 = d(35, 54, 58, "'AB' does not match the expected type of 'char'.", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		assertJavaDiagnostics(diagnosticsParams, utils, //
				d1, d2, d3, d4, d5);
	}

	@Test
	public void testImproperDefaultValuesList() throws Exception {
		Module javaProject = loadMavenProject(MicroProfileMavenProjectName.config_quickstart);
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		System.err.println(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/config/DefaultValueListResource.java"));
		String javaFileUri = getFileUri("src/main/java/org/acme/config/DefaultValueListResource.java", javaProject);
		diagnosticsParams.setUris(Arrays.asList(javaFileUri));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d1 = d(10, 53, 58, "'foo' does not match the expected type of 'List<Integer>'.", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		Diagnostic d2 = d(19, 53, 65, "'12,13\\,14' does not match the expected type of 'int[]'.", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		Diagnostic d3 = d(31, 53, 60, "'AB,CD' does not match the expected type of 'char[]'.", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		Diagnostic d4 = d(34, 53, 59, "',,,,' does not match the expected type of 'char[]'.", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		Diagnostic d5 = d(37, 54, 56, "'defaultValue=\"\"' will behave as if no default value is set, and will not be treated as an empty 'List<String>'.", DiagnosticSeverity.Warning,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.EMPTY_LIST_NOT_SUPPORTED);

		assertJavaDiagnostics(diagnosticsParams, utils, //
				d1, d2, d3, d4, d5);
	}

	@Test
	public void testNoValueAssignedWithIgnore() throws Exception {
		Module javaProject = loadMavenProject(MicroProfileMavenProjectName.config_quickstart);
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		String javaFileUri = getFileUri("src/main/java/org/acme/config/DefaultValueResource.java", javaProject);
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

		Diagnostic d4 = d(35, 54, 58, "'AB' does not match the expected type of 'char'.", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		assertJavaDiagnostics(diagnosticsParams, utils, //
				d1, d2, d3, d4);
	}

	@Test
	public void testUnassignedWithConfigproperties() throws Exception {
		Module javaProject = loadMavenProject(MicroProfileMavenProjectName.microprofile_configproperties);
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		String javaFileUri = getFileUri("src/main/java/org/acme/Details.java", javaProject);
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
		Module javaProject = loadMavenProject(MicroProfileMavenProjectName.config_quickstart);
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		saveFile(MicroProfileConfigSourceProvider.MICROPROFILE_CONFIG_PROPERTIES_FILE, "", javaProject);

		String propertiesFileUri = getFileUri("src/main/resources/META-INF/microprofile-config.properties", javaProject);

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		String javaFileUri = getFileUri("src/main/java/org/acme/config/UnassignedValue.java", javaProject);

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

		String lineSeparator = CodeStyleSettingsManager.getInstance(javaProject.getProject()).getMainProjectCodeStyle().
				getLineSeparator();
		if (lineSeparator == null) {
			lineSeparator = System.lineSeparator();
		}

		MicroProfileJavaCodeActionParams codeActionParams1 = createCodeActionParams(javaFileUri, d1, false);
		assertJavaCodeAction(codeActionParams1, utils, //
				ca(javaFileUri, "Insert 'defaultValue' attribute", MicroProfileCodeActionId.ConfigPropertyInsertDefaultValue, d1, //
						te(0, 0, 18, 0, "package org.acme.config;\n\nimport org.eclipse.microprofile.config.inject.ConfigProperty;\n\nimport io.quarkus.arc.config.ConfigProperties;\n\npublic class UnassignedValue {\n\n    @ConfigProperty(name = \"foo\", defaultValue = \"\")\n    private String foo;\n\n    @ConfigProperties(prefix = \"server\")\n    private class Server {\n\n        @ConfigProperty(name = \"url\")\n        private String url;\n    }\n}\n")),
				ca(propertiesFileUri, "Insert 'foo' property in 'META-INF/microprofile-config.properties'", MicroProfileCodeActionId.AssignValueToProperty, d1, //
						te(0, 0, 0, 0, "foo=" + lineSeparator)));

		MicroProfileJavaCodeActionParams codeActionParams2 = createCodeActionParams(javaFileUri, d2, false);
		assertJavaCodeAction(codeActionParams2, utils, //
				ca(javaFileUri, "Insert 'defaultValue' attribute", MicroProfileCodeActionId.ConfigPropertyInsertDefaultValue, d2, //
						te(0, 0, 18, 0, "package org.acme.config;\n\nimport org.eclipse.microprofile.config.inject.ConfigProperty;\n\nimport io.quarkus.arc.config.ConfigProperties;\n\npublic class UnassignedValue {\n\n    @ConfigProperty(name = \"foo\")\n    private String foo;\n\n    @ConfigProperties(prefix = \"server\")\n    private class Server {\n\n        @ConfigProperty(name = \"url\", defaultValue = \"\")\n        private String url;\n    }\n}\n")),
				ca(propertiesFileUri, "Insert 'server.url' property in 'META-INF/microprofile-config.properties'", MicroProfileCodeActionId.AssignValueToProperty, d2, //
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

		MicroProfileJavaCodeActionParams codeActionParams1_1 = createCodeActionParams(javaFileUri, d1_1);
		codeActionParams1_1.setCommandConfigurationUpdateSupported(true);
		assertJavaCodeAction(codeActionParams1_1, utils, //
				MicroProfileCodeActionFactory.createAddToUnassignedExcludedCodeAction("foo", d1_1),
				ca(javaFileUri, "Insert 'defaultValue' attribute", MicroProfileCodeActionId.ConfigPropertyInsertDefaultValue, d1_1, //
						te(0, 0, 18, 0, "package org.acme.config;\n\nimport org.eclipse.microprofile.config.inject.ConfigProperty;\n\nimport io.quarkus.arc.config.ConfigProperties;\n\npublic class UnassignedValue {\n\n    @ConfigProperty(name = \"foo\", defaultValue = \"\")\n    private String foo;\n\n    @ConfigProperties(prefix = \"server\")\n    private class Server {\n\n        @ConfigProperty(name = \"url\")\n        private String url;\n    }\n}\n")),
				ca(propertiesFileUri, "Insert 'foo' property in 'META-INF/microprofile-config.properties'", MicroProfileCodeActionId.AssignValueToProperty, d1_1, //
						te(0, 0, 0, 0, "foo=" + lineSeparator)));

		MicroProfileJavaCodeActionParams codeActionParams2_1 = createCodeActionParams(javaFileUri, d2_1);
		codeActionParams2_1.setCommandConfigurationUpdateSupported(true);
		assertJavaCodeAction(codeActionParams2_1, utils, //
				MicroProfileCodeActionFactory.createAddToUnassignedExcludedCodeAction("server.url", d2_1),
				ca(javaFileUri, "Insert 'defaultValue' attribute", MicroProfileCodeActionId.ConfigPropertyInsertDefaultValue, d2_1, //
						te(0, 0, 18, 0, "package org.acme.config;\n\nimport org.eclipse.microprofile.config.inject.ConfigProperty;\n\nimport io.quarkus.arc.config.ConfigProperties;\n\npublic class UnassignedValue {\n\n    @ConfigProperty(name = \"foo\")\n    private String foo;\n\n    @ConfigProperties(prefix = \"server\")\n    private class Server {\n\n        @ConfigProperty(name = \"url\", defaultValue = \"\")\n        private String url;\n    }\n}\n")),
				ca(propertiesFileUri, "Insert 'server.url' property in 'META-INF/microprofile-config.properties'", MicroProfileCodeActionId.AssignValueToProperty, d2_1, //
						te(0, 0, 0, 0, "server.url=" + lineSeparator)));

	}


	@Test
	public void testEmptyNameKeyValue() throws Exception {
		Module javaProject = loadMavenProject(MicroProfileMavenProjectName.microprofile_configproperties);
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		String javaFileUri = getFileUri("src/main/java/org/acme/EmptyKey.java", javaProject);
		diagnosticsParams.setUris(Arrays.asList(javaFileUri));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d1 = d(5, 25, 27, "The member 'name' can't be empty.", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.EMPTY_KEY);

		assertJavaDiagnostics(diagnosticsParams, utils, d1);
	}
}
