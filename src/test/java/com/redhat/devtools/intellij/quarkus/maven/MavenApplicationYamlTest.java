/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.maven;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.redhat.devtools.intellij.quarkus.search.ProjectLabelManager;
import com.redhat.devtools.intellij.quarkus.search.PsiUtilsImpl;
import com.redhat.microprofile.commons.MicroProfileJavaProjectLabelsParams;
import com.redhat.microprofile.commons.ProjectLabelInfoEntry;
import org.jetbrains.yaml.schema.YamlJsonSchemaHighlightingInspection;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;

/**
 * Project label tests
 *
 */
public class MavenApplicationYamlTest extends MavenImportingTestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		((CodeInsightTestFixture)myTestFixture).enableInspections(YamlJsonSchemaHighlightingInspection.class);
	}

	@Test
	public void testApplicationYamlFile() throws Exception {
		Module module = createMavenModule("hibernate-orm-resteasy-yaml", new File("projects/maven/hibernate-orm-resteasy-yaml"));
		((CodeInsightTestFixture)myTestFixture).setTestDataPath(ModuleUtilCore.getModuleDirPath(module));
		((CodeInsightTestFixture)myTestFixture).configureByFile("src/main/resources/application.yaml");
		((CodeInsightTestFixture)myTestFixture).checkHighlighting();
	}

}
