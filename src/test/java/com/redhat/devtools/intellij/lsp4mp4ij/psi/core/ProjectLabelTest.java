/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiMicroProfileUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4mp.commons.ProjectLabelInfoEntry;
import org.junit.Assert;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Project label tests
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/ProjectLabelTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/ProjectLabelTest.java</a>
 */
public class ProjectLabelTest extends MavenModuleImportingTestCase {

    public void testGetProjectLabelInfoOnlyMaven() throws Exception {
        Module maven = createMavenModule(new File("projects/lsp4mp/projects/maven/empty-maven-project"));
        var project = maven.getProject();
        List<ProjectLabelInfoEntry> projectLabelEntries = ProjectLabelManager.getInstance(project).getProjectLabelInfo(PsiUtilsLSImpl.getInstance(project));
        assertProjectLabelInfoContainsProject(projectLabelEntries, maven);
        assertLabels(projectLabelEntries, maven, "maven");
    }

	/*@Test
	public void getProjectLabelInfoOnlyGradle() throws Exception {
		IJavaProject gradle = BasePropertiesManagerTest.loadGradleProject(GradleProjectName.empty_gradle_project);
		List<ProjectLabelInfoEntry> projectLabelEntries = ProjectLabelManager.getInstance().getProjectLabelInfo();
		assertProjectLabelInfoContainsProject(projectLabelEntries, gradle);
		assertLabels(projectLabelEntries, gradle, "gradle");
	}*/

    public void testGetProjectLabelQuarkusMaven() throws Exception {
        Module quarkusMaven = createMavenModule(new File("projects/lsp4mp/projects/maven/using-vertx"));
        var project = quarkusMaven.getProject();
        List<ProjectLabelInfoEntry> projectLabelEntries = ProjectLabelManager.getInstance(project).getProjectLabelInfo(PsiUtilsLSImpl.getInstance(project));
        assertProjectLabelInfoContainsProject(projectLabelEntries, quarkusMaven);
        assertLabels(projectLabelEntries, quarkusMaven, "microprofile", "maven", "quarkus");
    }

	/*@Test
	public void getProjectLabelQuarkusGradle() throws Exception {
		IJavaProject quarkusGradle = BasePropertiesManagerTest
				.loadGradleProject(GradleProjectName.quarkus_gradle_project);
		List<ProjectLabelInfoEntry> projectLabelEntries = ProjectLabelManager.getInstance().getProjectLabelInfo();
		assertProjectLabelInfoContainsProject(projectLabelEntries, quarkusGradle);
		assertLabels(projectLabelEntries, quarkusGradle, "microprofile", "gradle");
	}

	@Test
	public void getProjectLabelMultipleProjects() throws Exception {
		IJavaProject quarkusMaven = BasePropertiesManagerTest.loadMavenProject(MicroProfileMavenProjectName.using_vertx);
		IJavaProject quarkusGradle = BasePropertiesManagerTest
				.loadGradleProject(GradleProjectName.quarkus_gradle_project);
		IJavaProject maven = BasePropertiesManagerTest.loadMavenProject(MicroProfileMavenProjectName.empty_maven_project);
		IJavaProject gradle = BasePropertiesManagerTest.loadGradleProject(GradleProjectName.empty_gradle_project);
		List<ProjectLabelInfoEntry> projectLabelEntries = ProjectLabelManager.getInstance().getProjectLabelInfo();

		assertProjectLabelInfoContainsProject(projectLabelEntries, quarkusMaven, quarkusGradle, maven, gradle);
		assertLabels(projectLabelEntries, quarkusMaven, "microprofile", "maven");
		assertLabels(projectLabelEntries, quarkusGradle, "microprofile", "gradle");
		assertLabels(projectLabelEntries, maven, "maven");
		assertLabels(projectLabelEntries, gradle, "gradle");
	}*/

    public void testProjectNameMaven() throws Exception {
        List<Module> modules = createMavenModules(Arrays.asList(
                new File("projects/lsp4mp/projects/maven/using-vertx"),
                new File("projects/lsp4mp/projects/maven/empty-maven-project"),
                new File("projects/lsp4mp/projects/maven/folder-name-different-maven")
        ));
        Module quarkusMaven = modules.stream().filter(m -> m.getName().equals("using-vertx")).findFirst().get();
        var project = quarkusMaven.getProject();
        Module maven = modules.stream().filter(m -> m.getName().equals("empty-maven-project")).findFirst().get();
        Module folderNameDifferent = modules.stream().filter(m -> m.getName().equals("mostly.empty")).findFirst().get();
        List<ProjectLabelInfoEntry> projectLabelEntries = ProjectLabelManager.getInstance(project).getProjectLabelInfo(PsiUtilsLSImpl.getInstance(project));
        assertName(projectLabelEntries, quarkusMaven, "using-vertx");
        assertName(projectLabelEntries, maven, "empty-maven-project");
        assertName(projectLabelEntries, folderNameDifferent, "mostly.empty");
    }

    public void testProjectNameSameFolderName() throws Exception {
        List<Module> modules = createMavenModules(Arrays.asList(
                new File("projects/lsp4mp/projects/maven/empty-maven-project"),
                new File("projects/lsp4mp/projects/maven/folder/empty-maven-project")
        ));
        Module empty1 = modules.stream().filter(m -> m.getName().equals("empty-maven-project")).findFirst().get();
        var project = empty1.getProject();
        Module empty2 = modules.stream().filter(m -> m.getName().equals("other-empty-maven-project")).findFirst().get();
        List<ProjectLabelInfoEntry> projectLabelEntries = ProjectLabelManager.getInstance(project).getProjectLabelInfo(PsiUtilsLSImpl.getInstance(project));
        assertName(projectLabelEntries, empty1, "empty-maven-project");
        assertName(projectLabelEntries, empty2, "other-empty-maven-project");
    }

	/*@Test
	public void projectNameGradle() throws Exception {
		IJavaProject quarkusGradle = BasePropertiesManagerTest.loadGradleProject(GradleProjectName.quarkus_gradle_project);
		IJavaProject gradle = BasePropertiesManagerTest.loadGradleProject(GradleProjectName.empty_gradle_project);
		IJavaProject renamedGradle = BasePropertiesManagerTest.loadGradleProject(GradleProjectName.renamed_quarkus_gradle_project);
		List<ProjectLabelInfoEntry> projectLabelEntries = ProjectLabelManager.getInstance().getProjectLabelInfo();
		assertName(projectLabelEntries, quarkusGradle, "quarkus-gradle-project");
		assertName(projectLabelEntries, gradle, "empty-gradle-project");
		assertName(projectLabelEntries, renamedGradle, "my-gradle-project");
	}*/

    private static void assertProjectLabelInfoContainsProject(List<ProjectLabelInfoEntry> projectLabelEntries,
                                                              Module... javaProjects) {
        List<String> actualProjectPaths = projectLabelEntries.stream().map(ProjectLabelInfoEntry::getUri)
                .collect(Collectors.toList());
        for (Module javaProject : javaProjects) {
            assertContains(actualProjectPaths, PsiMicroProfileUtils.getProjectURI(javaProject));
        }
    }

    private static void assertLabels(List<ProjectLabelInfoEntry> projectLabelEntries, Module javaProject,
                                     String... expectedLabels) {
        String javaProjectPath = PsiMicroProfileUtils.getProjectURI(javaProject);
        List<String> actualLabels = getLabelsFromProjectPath(projectLabelEntries, javaProjectPath);
        Assert.assertEquals(
                "Test project labels size for '" + javaProjectPath + "' with labels ["
                        + String.join(",", actualLabels) + "]",
                expectedLabels.length, actualLabels.size());
        for (String expectedLabel : expectedLabels) {
            assertContains(actualLabels, expectedLabel);
        }
    }

    private static void assertName(List<ProjectLabelInfoEntry> projectLabelEntries, Module javaProject,
                                   String expectedName) {
        String javaProjectPath = PsiMicroProfileUtils.getProjectURI(javaProject);
        String actualName = null;
        for (ProjectLabelInfoEntry entry : projectLabelEntries) {
            if (entry.getUri().equals(javaProjectPath)) {
                actualName = entry.getName();
                break;
            }
        }
        Assert.assertEquals("Test project name in label", expectedName, actualName);
    }

    private static List<String> getLabelsFromProjectPath(List<ProjectLabelInfoEntry> projectLabelEntries, String projectPath) {
        for (ProjectLabelInfoEntry entry : projectLabelEntries) {
            if (entry.getUri().equals(projectPath)) {
                return entry.getLabels();
            }
        }
        return Collections.emptyList();
    }

    private static void assertContains(List<String> list, String strToFind) {
        for (String str : list) {
            if (str.equals(strToFind)) {
                return;
            }
        }
        Assert.fail("Expected List to contain <\"" + strToFind + "\">.");
    }
}
