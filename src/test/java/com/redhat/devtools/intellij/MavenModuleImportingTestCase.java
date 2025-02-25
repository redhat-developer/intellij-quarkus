/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij;

import com.intellij.maven.testFramework.MavenImportingTestCase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.*;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class MavenModuleImportingTestCase extends MavenImportingTestCase {

  protected TestFixtureBuilder<IdeaProjectTestFixture> myProjectBuilder;

  protected void setUpFixtures() throws Exception {
    myProjectBuilder = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getName());
    final JavaTestFixtureFactory factory = JavaTestFixtureFactory.getFixtureFactory();
    myProjectBuilder.addModule(JavaModuleFixtureBuilder.class);
    myTestFixture = factory.createCodeInsightFixture(myProjectBuilder.getFixture());
    myTestFixture.setUp();
    LanguageLevelProjectExtension.getInstance(myTestFixture.getProject()).setLanguageLevel(LanguageLevel.JDK_17);
  }

  private static int counter = 0;

  /**
   * Create a new module into the test project from existing project folder.
   *
   * @param projectDirs the project folders
   * @return the created modules
   */
  protected List<Module> createMavenModules(List<File> projectDirs) throws Exception {
    Project project = myTestFixture.getProject();
    for(File projectDir : projectDirs) {
      File moduleDir = new File(project.getBasePath(), projectDir.getName() + counter++);
      FileUtils.copyDirectory(projectDir, moduleDir);
      VirtualFile pomFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(moduleDir).findFileByRelativePath("pom.xml");
      BuildersKt.runBlocking(
          EmptyCoroutineContext.INSTANCE,
          (scope, continuation) -> importProjectAsync(pomFile,continuation)
      );
    }

    Module[] modules = ModuleManager.getInstance(project).getModules();
    for(Module module : modules) {
      setupJdkForModule(module.getName());
    }
    return Arrays.stream(modules).skip(1).toList();
  }

  protected Module createMavenModule(File projectDir) throws Exception {
    List<Module> modules = createMavenModules(Collections.singletonList(projectDir));
    return modules.get(modules.size() - 1);
  }

  /**
   * Create a new module into the test project from existing in memory POM.
   *
   * @param name the new module name
   * @param xml the project POM
   * @return the created module
   */
  protected Module createMavenModule(String name, String xml) throws Exception {
    Module module = myTestFixture.getModule();
    File moduleDir = new File(module.getModuleFilePath()).getParentFile();
    VirtualFile pomFile = createPomFile(LocalFileSystem.getInstance().findFileByIoFile(moduleDir), xml);
    BuildersKt.runBlocking(
        EmptyCoroutineContext.INSTANCE,
        (scope, continuation) -> importProjectAsync(pomFile,continuation)
    );
    Module[] modules = ModuleManager.getInstance(myTestFixture.getProject()).getModules();
    if (modules.length > 0) {
      module = modules[modules.length - 1];
      setupJdkForModule(module.getName());
    }
    return module;
  }

  protected IPsiUtils getJDTUtils() {
    return PsiUtilsLSImpl.getInstance(myProject);
  }
}
