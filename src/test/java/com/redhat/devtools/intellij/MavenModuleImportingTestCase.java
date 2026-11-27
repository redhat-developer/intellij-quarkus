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

import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.maven.testFramework.MavenImportingTestCase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.TestApplicationManager;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.*;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.future.FutureKt;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class MavenModuleImportingTestCase extends MavenImportingTestCase {

  protected TestFixtureBuilder<IdeaProjectTestFixture> myProjectBuilder;

  protected void setUpFixtures() throws Exception {
    myProjectBuilder = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getName());
    final JavaTestFixtureFactory factory = JavaTestFixtureFactory.getFixtureFactory();
    myProjectBuilder.addModule(JavaModuleFixtureBuilder.class);
    IdeaProjectTestFixture myFixture = factory.createCodeInsightFixture(myProjectBuilder.getFixture());
    setTestFixture(myFixture);
    // The default project's ExternalAnnotationsManager, which IntelliJ creates the first time a test reads a class of a
    // library, keeps a document listener, which the fixture's leak check reports as a leak of that test before IntelliJ
    // 2026.3, whose check ignores it. Created here, in the application that the fixture's setUp starts, it exists before
    // the check takes its snapshot at the end of setUp.
    TestApplicationManager.getInstance();
    ExternalAnnotationsManager.getInstance(ProjectManager.getInstance().getDefaultProject());
    myFixture.setUp();
    LanguageLevelProjectExtension.getInstance(myFixture.getProject()).setLanguageLevel(LanguageLevel.JDK_17);
  }

  private static int counter = 0;

  // The first import of a project downloads its dependencies
  private static final long MAVEN_IMPORT_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(10);

  /**
   * Create a new module into the test project from existing project folder.
   *
   * @param projectDirs the project folders
   * @return the created modules
   */
  protected List<Module> createMavenModules(List<File> projectDirs) throws Exception {
    Project project = getTestFixture().getProject();
    List<VirtualFile> pomFiles = new ArrayList<>();
    for(File projectDir : projectDirs) {
      File moduleDir = new File(project.getBasePath(), projectDir.getName() + counter++);
      FileUtils.copyDirectory(projectDir, moduleDir);
      VirtualFile pomFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(moduleDir.toPath()).findFileByRelativePath("pom.xml");
      pomFiles.add(pomFile);
    }

    importProjectsOffEdt(pomFiles.toArray(VirtualFile[]::new));

    Module[] modules = ModuleManager.getInstance(project).getModules();
    for(Module module : modules) {
      setupJdkForModule(module.getName());
    }

    return Arrays.stream(modules).toList();
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
    Module module = getTestFixture().getModule();
    File moduleDir = new File(module.getModuleFilePath()).getParentFile();
    VirtualFile pomFile = createPomFile(LocalFileSystem.getInstance().findFileByIoFile(moduleDir), xml);
    List<VirtualFile> pomFiles = new ArrayList<>();
    pomFiles.add(pomFile);
    importProjectsOffEdt(pomFiles.toArray(VirtualFile[]::new));
    Module[] modules = ModuleManager.getInstance(getTestFixture().getProject()).getModules();
    if (modules.length > 0) {
      module = modules[modules.length - 1];
      setupJdkForModule(module.getName());
    }
    IndexingTestUtil.waitUntilIndexesAreReady(getTestFixture().getProject());
    return module;
  }

  /**
   * Imports the given POM files with the non-suspending importProjects method, on a pooled thread, while the test's
   * thread, the EDT, dispatches its events. The Kotlin suspending function importProjectsAsync(file: VirtualFile) blocks
   * the tests from IntelliJ 2024.2 on, and importProjects blocks them on the EDT from IntelliJ 2025.1 on: the Maven sync
   * waits there for a write action on the EDT (edtWriteAction).
   *
   * @param pomFiles the POM files to import
   */
  private void importProjectsOffEdt(VirtualFile... pomFiles) {
    Future<?> imported = ApplicationManager.getApplication().executeOnPooledThread(() -> importProjects(pomFiles));
    PlatformTestUtil.waitForFuture(imported, MAVEN_IMPORT_TIMEOUT_MILLIS);
  }

  protected IPsiUtils getJDTUtils() {
    return PsiUtilsLSImpl.getInstance(getProject());
  }
}
