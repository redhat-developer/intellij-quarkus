/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.completion;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.util.ui.UIUtil;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.quarkus.json.ApplicationYamlJsonSchemaManager;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import org.jetbrains.yaml.schema.YamlJsonSchemaHighlightingInspection;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * Project label tests
 *
 */
public class MavenApplicationYamlCompletionTest extends MavenModuleImportingTestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		((CodeInsightTestFixture)myTestFixture).enableInspections(YamlJsonSchemaHighlightingInspection.class);
	}

	@Test
	public void testApplicationYamlFile() throws Exception {
		Module module = createMavenModule(new File("projects/maven/hibernate-orm-resteasy-yaml"));
		((CodeInsightTestFixture)myTestFixture).setTestDataPath(ModuleUtilCore.getModuleDirPath(module));
		var psiFile = ((CodeInsightTestFixture)myTestFixture).configureByFile("src/main/resources/application.yaml");
		UIUtil.dispatchAllInvocationEvents();
		((CodeInsightTestFixture)myTestFixture).checkHighlighting();
	}
}
