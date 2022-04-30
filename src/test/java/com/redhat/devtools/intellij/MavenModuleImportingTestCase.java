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

import java.io.File;

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

  /**
   * Create a new module into the test project from existing project folder.
   *
   * @param name the new module name
   * @param projectDir the project folder
   * @return the created module
   */
  protected Module createMavenModule(String name, File projectDir) throws Exception {
    Module module = myTestFixture.getModule();
    File moduleDir = new File(module.getModuleFilePath()).getParentFile();
    FileUtils.copyDirectory(projectDir, moduleDir);
    VirtualFile pomFile = LocalFileSystem.getInstance().findFileByIoFile(moduleDir).findFileByRelativePath("pom.xml");
    importProject(pomFile);
    Module[] modules = ModuleManager.getInstance(myTestFixture.getProject()).getModules();
    if (modules.length > 0) {
      module = modules[modules.length - 1];
      QuarkusModuleUtil.ensureQuarkusLibrary(module);
      setupJdkForModule(module.getName());
    }
    return module;
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
