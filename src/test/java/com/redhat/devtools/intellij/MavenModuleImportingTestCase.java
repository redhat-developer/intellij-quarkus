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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.builders.ModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import com.intellij.testFramework.fixtures.ModuleFixture;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import org.apache.commons.io.FileUtils;
import org.jetbrains.idea.maven.MavenImportingTestCase;
import org.jetbrains.idea.maven.execution.MavenRunner;
import org.jetbrains.idea.maven.execution.MavenRunnerParameters;
import org.jetbrains.idea.maven.execution.MavenRunnerSettings;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public abstract class MavenModuleImportingTestCase extends MavenImportingTestCase {

  protected TestFixtureBuilder<IdeaProjectTestFixture> myProjectBuilder;

  protected void setUpFixtures() throws Exception {
    //myTestFixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getName()).getFixture();
    //myTestFixture.setUp();
    myProjectBuilder = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getName());
    final JavaTestFixtureFactory factory = JavaTestFixtureFactory.getFixtureFactory();
    ModuleFixtureBuilder moduleBuilder = myProjectBuilder.addModule(JavaModuleFixtureBuilder.class);
    myTestFixture = factory.createCodeInsightFixture(myProjectBuilder.getFixture());
    myTestFixture.setUp();
    LanguageLevelProjectExtension.getInstance(myTestFixture.getProject()).setLanguageLevel(LanguageLevel.JDK_1_6);
  }
  protected Module createJavaModule(final String name) throws Exception {
    ModuleFixture moduleFixture = myProjectBuilder.addModule(JavaModuleFixtureBuilder.class).getFixture();
    moduleFixture.setUp();
    Module module = myProjectBuilder.addModule(JavaModuleFixtureBuilder.class).getFixture().getModule();
    return module;
  }

  protected void executeGoal(VirtualFile dir, String goal) throws Exception {
    MavenRunnerParameters rp = new MavenRunnerParameters(true, dir.getPath(), (String)null, Collections.singletonList(goal), Collections.emptyList());
    MavenRunnerSettings rs = new MavenRunnerSettings();
    Semaphore wait = new Semaphore(1);
    wait.acquire();
    MavenRunner.getInstance(myProject).run(rp, rs, () -> {
      wait.release();
    });
    boolean tryAcquire = wait.tryAcquire(1, TimeUnit.MINUTES);
    assertTrue( "Maven execution failed", tryAcquire);
  }


  /**
   * Create a new module into the test project from existing project folder.
   *
   * @param name the new module name
   * @param projectDir the project folder
   * @return the created module
   */
  protected Module createMavenModule(String name, File projectDir, boolean processResources) throws Exception {
    Module module = null;
    Project project = myTestFixture.getProject();
    File moduleDir = new File(project.getBasePath());
    FileUtils.copyDirectory(projectDir, moduleDir);
    VirtualFile pomFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(moduleDir).findFileByRelativePath("pom.xml");
    importProject(pomFile);
    Module[] modules = ModuleManager.getInstance(myTestFixture.getProject()).getModules();
    for(Module module1 : modules) {
      setupJdkForModule(module1.getName());
    }
    if (modules.length > 0) {
      module = modules[modules.length - 1];
      QuarkusModuleUtil.ensureQuarkusLibrary(module);
      if (processResources) {
        executeGoal(pomFile.getParent(), "resources:resources");
      }
    }
    return module;
  }

  protected Module createMavenModule(String name, File projectDir) throws Exception {
    return createMavenModule(name, projectDir, false);
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
    importProject(pomFile);
    Module[] modules = ModuleManager.getInstance(myTestFixture.getProject()).getModules();
    if (modules.length > 0) {
      module = modules[modules.length - 1];
      setupJdkForModule(module.getName());
    }
    return module;
  }
}
