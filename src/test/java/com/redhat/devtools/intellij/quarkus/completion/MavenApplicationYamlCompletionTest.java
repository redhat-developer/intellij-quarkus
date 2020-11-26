/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.completion;

import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.redhat.devtools.intellij.quarkus.maven.MavenImportingTestCase;
import org.jetbrains.yaml.schema.YamlJsonSchemaHighlightingInspection;
import org.junit.Test;

import java.awt.AWTEvent;
import java.io.File;

/**
 * Project label tests
 *
 */
public class MavenApplicationYamlCompletionTest extends MavenImportingTestCase {
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
		pumpEvents(5000);
		((CodeInsightTestFixture)myTestFixture).checkHighlighting();
	}

	private void pumpEvents(long timeout) {
		long start = System.currentTimeMillis();
		IdeEventQueue queue = IdeEventQueue.getInstance();

		while ((queue.peekEvent()) != null && System.currentTimeMillis() - start < timeout) {
			try {
				queue.dispatchEvent(queue.getNextEvent());
			} catch (InterruptedException e) {
				Thread.interrupt();
			}
		}
	}
}
