/*******************************************************************************
* Copyright (c) 2026 Red Hat Inc. and others.
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
package com.redhat.microprofile.psi.quarkus.flow;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.quarkus.psi.internal.providers.QuarkusConfigSourceProvider;
import com.redhat.microprofile.psi.internal.quarkus.flow.FlowConstants;
import com.redhat.microprofile.psi.quarkus.QuarkusMavenModuleImportingTestCase;
import com.redhat.microprofile.psi.quarkus.QuarkusMavenProjectName;
import org.eclipse.lsp4j.Position;
import org.junit.Test;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.saveFile;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.*;

/**
 * Tests {@link com.redhat.microprofile.psi.internal.quarkus.flow.java.FlowIdentifierDefinitionParticipant},
 * which navigates from an {@code @Identifier("...")} value on a {@code Flow}/
 * {@code WorkflowDefinition}-typed field/parameter to the matching workflow file under
 * {@code src/main/resources/flow}.
 */
public class QuarkusFlowDefinitionTest extends QuarkusMavenModuleImportingTestCase {

	@Test
	public void testFieldInjectionSpecNamingStrategy() throws Exception {
		Module javaProject = loadMavenProject(QuarkusMavenProjectName.flow_quickstart);
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
		String javaFileUri = getFileUri("src/main/java/org/acme/flow/FlowBean.java", javaProject);
		String yamlFileUri = getFileUri("src/main/resources/flow/sample.yaml", javaProject);

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE,
				FlowConstants.FLOW_NAMING_STRATEGY_PROPERTY + "=" + FlowConstants.NAMING_STRATEGY_SPEC + "\r\n",
				javaProject);

		// @Identifier("flow:for-task-name-c|ollision") --> sample.yaml
		assertJavaDefinitions(new Position(14, 30), javaFileUri, utils, //
				def(r(14, 17, 45), yamlFileUri, r(0, 0, 0, 0)));
	}

	@Test
	public void testVersionlessIdentifierResolvesToHighestVersion() throws Exception {
		Module javaProject = loadMavenProject(QuarkusMavenProjectName.flow_quickstart);
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
		String javaFileUri = getFileUri("src/main/java/org/acme/flow/FlowBean.java", javaProject);
		String yamlFileUri = getFileUri("src/main/resources/flow/multi-v2.yaml", javaProject);

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE,
				FlowConstants.FLOW_NAMING_STRATEGY_PROPERTY + "=" + FlowConstants.NAMING_STRATEGY_SPEC + "\r\n",
				javaProject);

		// @Identifier("flow:multi-ve|rsion") --> multi-v2.yaml (highest of v1.0.0/v2.0.0)
		assertJavaDefinitions(new Position(18, 25), javaFileUri, utils, //
				def(r(18, 17, 35), yamlFileUri, r(0, 0, 0, 0)));
	}

	@Test
	public void testClassNamingStrategy() throws Exception {
		Module javaProject = loadMavenProject(QuarkusMavenProjectName.flow_quickstart);
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
		String javaFileUri = getFileUri("src/main/java/org/acme/flow/FlowClassStrategyBean.java", javaProject);
		String yamlFileUri = getFileUri("src/main/resources/flow/multi-v1.yaml", javaProject);

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE,
				FlowConstants.FLOW_NAMING_STRATEGY_PROPERTY + "=" + FlowConstants.NAMING_STRATEGY_CLASS + "\r\n",
				javaProject);

		// @Identifier("flow.v1_0_0.Multi|Version") --> multi-v1.yaml
		assertJavaDefinitions(new Position(14, 30), javaFileUri, utils, //
				def(r(14, 17, 41), yamlFileUri, r(0, 0, 0, 0)));
	}

	@Test
	public void testNonFlowTypeHasNoDefinition() throws Exception {
		Module javaProject = loadMavenProject(QuarkusMavenProjectName.flow_quickstart);
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
		String javaFileUri = getFileUri("src/main/java/org/acme/flow/FlowBean.java", javaProject);

		// @Identifier("unrelated-b|ean") on a String field --> no definition
		assertJavaDefinitions(new Position(22, 25), javaFileUri, utils);
	}

	@Test
	public void testParameterInjectionPointDefinition() throws Exception {
		Module javaProject = loadMavenProject(QuarkusMavenProjectName.flow_quickstart);
		IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
		String javaFileUri = getFileUri("src/main/java/org/acme/flow/FlowParameterBean.java", javaProject);
		String yamlFileUri = getFileUri("src/main/resources/flow/sample.yaml", javaProject);

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE,
				FlowConstants.FLOW_NAMING_STRATEGY_PROPERTY + "=" + FlowConstants.NAMING_STRATEGY_SPEC + "\r\n",
				javaProject);

		// @Identifier("flow:for-task-name-c|ollision") Flow exprFor (constructor parameter) --> sample.yaml
		assertJavaDefinitions(new Position(15, 55), javaFileUri, utils, //
				def(r(15, 42, 70), yamlFileUri, r(0, 0, 0, 0)));
	}
}
