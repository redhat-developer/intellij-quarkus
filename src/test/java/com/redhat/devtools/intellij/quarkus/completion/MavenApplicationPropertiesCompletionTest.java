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

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.MavenEditorTest;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileMavenProjectName;
import com.redhat.devtools.intellij.quarkus.QuarkusDeploymentSupport;
import org.junit.Test;

import java.io.File;

/**
 * Project label tests
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/ProjectLabelTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/ProjectLabelTest.java</a>
 */
public class MavenApplicationPropertiesCompletionTest extends MavenEditorTest {

	@Test
	public void testBooleanCompletion() throws Exception {
		Module module = loadMavenProject(MicroProfileMavenProjectName.config_quickstart, true);
		VirtualFile propertiesFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/resources/application.properties");
		codeInsightTestFixture.configureFromExistingVirtualFile(propertiesFile);
		codeInsightTestFixture.performEditorAction(IdeActions.ACTION_EDITOR_MOVE_LINE_END);
		codeInsightTestFixture.performEditorAction(IdeActions.ACTION_EDITOR_START_NEW_LINE);
		insertLine("quarkus.arc.auto-inject-fields=");
		LookupElement[] elements = codeInsightTestFixture.completeBasic();
		assertNotNull(elements);
		assertEquals(2, elements.length);
	}

	private void insertLine(String s) throws InterruptedException {
		for (int i = 0; i < s.length(); ++i) {
			codeInsightTestFixture.type(s.charAt(i));
		}
		Thread.sleep(1000);
	}

	protected Module loadMavenProject(String projectName) throws Exception {
		return loadMavenProject(projectName, false);
	}

	protected Module loadMavenProject(String projectName, boolean collectAndAddQuarkusDeploymentDependencies) throws Exception {
		Module module = createMavenModule(new File("projects/quarkus/projects/maven/" + projectName));
		if(collectAndAddQuarkusDeploymentDependencies) {
			QuarkusDeploymentSupport.updateClasspathWithQuarkusDeployment(module, new EmptyProgressIndicator());
		}
		return module;
	}
}
