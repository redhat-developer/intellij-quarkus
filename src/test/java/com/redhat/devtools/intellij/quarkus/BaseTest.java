package com.redhat.devtools.intellij.quarkus;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.util.ui.UIUtil;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.idea.maven.model.MavenExplicitProfiles;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.project.MavenProjectsTree;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public abstract class BaseTest extends UsefulTestCase {
    private IdeaProjectTestFixture myFixture;
    private MavenProjectsManager mavenProjectsManager;
    private MavenProjectsTree mavenProjectsTree;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getName()).getFixture();
        myFixture.setUp();
        FileUtil.ensureExists(new File(FileUtil.getTempDirectory()));
        mavenProjectsManager = MavenProjectsManager.getInstance(myFixture.getProject());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        //myFixture.tearDown();
    }

    protected VirtualFile createProjectSubFile(String relativePath) throws IOException {
        File f = new File(myFixture.getProject().getBasePath(), relativePath);
        f.getParentFile().mkdirs();
        f.createNewFile();
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(f);
    }


    protected Module createModule(final String name) {
        try {
            return WriteCommandAction.writeCommandAction(myFixture.getProject()).compute(() -> {
                VirtualFile f = createProjectSubFile(name + "/" + name + ".iml");
                Module module = ModuleManager.getInstance(myFixture.getProject()).newModule(f.getPath(), StdModuleTypes.JAVA.getId());
                PsiTestUtil.addContentRoot(module, f.getParent());
                return module;
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected VirtualFile createPomFile(final VirtualFile dir,
                                        @Language(value = "XML", prefix = "<project>", suffix = "</project>") String xml) {
        VirtualFile f = dir.findChild("pom.xml");
        if (f == null) {
            try {
                f = WriteAction.computeAndWait(() -> {
                    VirtualFile res = dir.createChildData(null, "pom.xml");
                    return res;
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        setFileContent(f, createPomXml(xml), true);
        return f;
    }

    private static void setFileContent(final VirtualFile file, final String content, final boolean advanceStamps) {
        try {
            WriteAction.runAndWait(() -> {
                if (advanceStamps) {
                    file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8), -1, file.getTimeStamp() + 4000);
                } else {
                    file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8), file.getModificationStamp(), file.getTimeStamp());
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static String createPomXml(@NonNls @Language(value = "XML", prefix = "<project>", suffix = "</project>") String xml) {
        return "<?xml version=\"1.0\"?>" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">" +
                "  <modelVersion>4.0.0</modelVersion>" +
                xml +
                "</project>";
    }

    protected void initProjectsManager(boolean enableEventHandling) {
        mavenProjectsManager.initForTests();
        mavenProjectsTree = mavenProjectsManager.getProjectsTreeForTests();
        if (enableEventHandling) mavenProjectsManager.listenForExternalChanges();
    }


    private void doImportProjects(VirtualFile file, boolean failOnReadingError) {
        initProjectsManager(false);

        readProjects(file);

        UIUtil.invokeAndWaitIfNeeded((Runnable) () -> {
            mavenProjectsManager.waitForResolvingCompletion();
            mavenProjectsManager.scheduleImportInTests(Collections.singletonList(file));
            mavenProjectsManager.importProjects();
        });

        if (failOnReadingError) {
            for (MavenProject each : mavenProjectsTree.getProjects()) {
                assertFalse("Failed to import Maven project: " + each.getProblems(), each.hasReadingProblems());
            }
        }
    }

    protected void waitForReadingCompletion() {
        UIUtil.invokeAndWaitIfNeeded((Runnable) () -> {
            try {
                mavenProjectsManager.waitForReadingCompletion();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }


    protected void readProjects(VirtualFile file) {
        mavenProjectsManager.resetManagedFilesAndProfilesInTests(Collections.singletonList(file), new MavenExplicitProfiles(Collections.emptyList()));
        waitForReadingCompletion();
    }


    protected Module createModule(String name, String xml) {
        Module module = createModule(name);
        VirtualFile pomFile = createPomFile(module.getModuleFile().getParent(), xml);
        doImportProjects(pomFile, true);
        return module;
    }
}
