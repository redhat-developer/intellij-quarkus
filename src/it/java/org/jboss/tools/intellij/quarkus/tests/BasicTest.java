/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.intellij.quarkus.tests;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.JButtonFixture;
import com.intellij.remoterobot.fixtures.JTextFieldFixture;
import com.intellij.remoterobot.fixtures.JTreeFixture;
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException;
import com.redhat.devtools.intellij.commonuitest.UITestRunner;
import com.redhat.devtools.intellij.commonuitest.fixtures.dialogs.FlatWelcomeFrame;
import com.redhat.devtools.intellij.commonuitest.fixtures.dialogs.project.NewProjectDialogWizard;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.MainIdeWindow;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.idestatusbar.IdeStatusBar;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.toolwindowspane.BuildView;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.toolwindowspane.ToolWindowPane;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.toolwindowspane.ToolWindowsPane;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.toolwindowspane.buildtoolpane.GradleBuildToolPane;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.toolwindowspane.buildtoolpane.MavenBuildToolPane;
import com.redhat.devtools.intellij.commonuitest.utils.project.CreateCloseUtils;
import com.redhat.devtools.intellij.commonuitest.utils.screenshot.ScreenshotUtils;
import org.apache.commons.io.FileUtils;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.project.pages.QuarkusNewProjectFinalPage;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.project.pages.QuarkusNewProjectFirstPage;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.project.pages.QuarkusNewProjectSecondPage;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.project.pages.QuarkusNewProjectThirdPage;
import org.jboss.tools.intellij.quarkus.utils.BuildTool;
import org.jboss.tools.intellij.quarkus.utils.EndpointURLType;
import org.jboss.tools.intellij.quarkus.utils.XPathDefinitions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Basic Quarkus tests
 *
 * @author zcervink@redhat.com
 */
public class BasicTest extends AbstractQuarkusTest {
    private final String NEW_QUARKUS_MAVEN_PROJECT_NAME = "code-with-quarkus-maven";
    private final String NEW_QUARKUS_GRADLE_PROJECT_NAME = "code-with-quarkus-gradle";
    private final String JAVA_VERSION_FOR_QUARKUS_PROJECT = "17";

    @AfterEach
    public void finishTestRun() {
        CreateCloseUtils.closeProject(remoteRobot);
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("closeProject_output:");
        System.out.println(Files.exists(Paths.get(CreateCloseUtils.PROJECT_LOCATION)));
        System.out.println(Files.exists(Paths.get(CreateCloseUtils.PROJECT_LOCATION + File.separator + "code-with-quarkus")));
        System.out.println(Files.exists(Paths.get(CreateCloseUtils.PROJECT_LOCATION + File.separator + "code-with-quarkus-gradle")));
        System.out.println(Files.exists(Paths.get(CreateCloseUtils.PROJECT_LOCATION + File.separator + "code-with-quarkus-maven")));
        System.out.println(Files.exists(Paths.get(System.getProperty("user.home") + File.separator + "IdeaProjects" + File.separator + "code-with-quarkus")));
        System.out.println(Files.exists(Paths.get(System.getProperty("user.home") + File.separator + "IdeaProjects" + File.separator + "code-with-quarkus-gradle")));
        System.out.println(Files.exists(Paths.get(System.getProperty("user.home") + File.separator + "IdeaProjects" + File.separator + "code-with-quarkus-maven")));
        ScreenshotUtils.takeScreenshot(remoteRobot, "closeProjectScreenshot");

        try {
            String pathToDirToMakeEmpty = CreateCloseUtils.PROJECT_LOCATION;
            FileUtils.cleanDirectory(new File(pathToDirToMakeEmpty));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("clearPROJECT_LOCATION_output:");
        System.out.println(Files.exists(Paths.get(CreateCloseUtils.PROJECT_LOCATION)));
        System.out.println(Files.exists(Paths.get(CreateCloseUtils.PROJECT_LOCATION + File.separator + "code-with-quarkus")));
        System.out.println(Files.exists(Paths.get(CreateCloseUtils.PROJECT_LOCATION + File.separator + "code-with-quarkus-gradle")));
        System.out.println(Files.exists(Paths.get(CreateCloseUtils.PROJECT_LOCATION + File.separator + "code-with-quarkus-maven")));
        System.out.println(Files.exists(Paths.get(System.getProperty("user.home") + File.separator + "IdeaProjects" + File.separator + "code-with-quarkus")));
        System.out.println(Files.exists(Paths.get(System.getProperty("user.home") + File.separator + "IdeaProjects" + File.separator + "code-with-quarkus-gradle")));
        System.out.println(Files.exists(Paths.get(System.getProperty("user.home") + File.separator + "IdeaProjects" + File.separator + "code-with-quarkus-maven")));
        ScreenshotUtils.takeScreenshot(remoteRobot, "clearPROJECT_LOCATIONScreenshot");

        try {
            String pathToDirToMakeEmpty = System.getProperty("user.home") + File.separator + "IdeaProjects";
            FileUtils.cleanDirectory(new File(pathToDirToMakeEmpty));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("clearIdeaProjects_output:");
        System.out.println(Files.exists(Paths.get(System.getProperty("user.home") + File.separator + "IdeaProjects")));
        System.out.println(Files.exists(Paths.get(CreateCloseUtils.PROJECT_LOCATION + File.separator + "code-with-quarkus")));
        System.out.println(Files.exists(Paths.get(CreateCloseUtils.PROJECT_LOCATION + File.separator + "code-with-quarkus-gradle")));
        System.out.println(Files.exists(Paths.get(CreateCloseUtils.PROJECT_LOCATION + File.separator + "code-with-quarkus-maven")));
        System.out.println(Files.exists(Paths.get(System.getProperty("user.home") + File.separator + "IdeaProjects" + File.separator + "code-with-quarkus")));
        System.out.println(Files.exists(Paths.get(System.getProperty("user.home") + File.separator + "IdeaProjects" + File.separator + "code-with-quarkus-gradle")));
        System.out.println(Files.exists(Paths.get(System.getProperty("user.home") + File.separator + "IdeaProjects" + File.separator + "code-with-quarkus-maven")));
        ScreenshotUtils.takeScreenshot(remoteRobot, "clearIdeaProjectsScreenshot");

        try {
            FlatWelcomeFrame flatWelcomeFrame = remoteRobot.find(FlatWelcomeFrame.class, Duration.ofSeconds(10));
            flatWelcomeFrame.clearExceptions();
            flatWelcomeFrame.clearWorkspace();
        } catch (Exception e) {
            ScreenshotUtils.takeScreenshot(remoteRobot, "clearWorkspaceScreenshotError");
            throw new RuntimeException(e);
        }
        System.out.println("clearWorkspace_output:");
        System.out.println(Files.exists(Paths.get(CreateCloseUtils.PROJECT_LOCATION)));
        System.out.println(Files.exists(Paths.get(CreateCloseUtils.PROJECT_LOCATION + File.separator + "code-with-quarkus")));
        System.out.println(Files.exists(Paths.get(CreateCloseUtils.PROJECT_LOCATION + File.separator + "code-with-quarkus-gradle")));
        System.out.println(Files.exists(Paths.get(CreateCloseUtils.PROJECT_LOCATION + File.separator + "code-with-quarkus-maven")));
        System.out.println(Files.exists(Paths.get(System.getProperty("user.home") + File.separator + "IdeaProjects" + File.separator + "code-with-quarkus")));
        System.out.println(Files.exists(Paths.get(System.getProperty("user.home") + File.separator + "IdeaProjects" + File.separator + "code-with-quarkus-gradle")));
        System.out.println(Files.exists(Paths.get(System.getProperty("user.home") + File.separator + "IdeaProjects" + File.separator + "code-with-quarkus-maven")));
        ScreenshotUtils.takeScreenshot(remoteRobot, "clearWorkspaceScreenshot");
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    }

    @Test
    public void createBuildQuarkusMavenTest() {
        createQuarkusProject(remoteRobot, NEW_QUARKUS_MAVEN_PROJECT_NAME, BuildTool.MAVEN, EndpointURLType.DEFAULT);
        ToolWindowPane toolWindowPane = remoteRobot.find(ToolWindowPane.class, Duration.ofSeconds(10));
        toolWindowPane.openMavenBuildToolPane();
        MavenBuildToolPane mavenBuildToolPane = toolWindowPane.find(MavenBuildToolPane.class, Duration.ofSeconds(10));
        mavenBuildToolPane.buildProject("install");
        boolean isBuildSuccessful = toolWindowPane.find(BuildView.class, Duration.ofSeconds(10)).isBuildSuccessful();
        assertTrue(isBuildSuccessful, "The build should be successful but is not.");
    }

    @Test
    public void createBuildQuarkusGradleTest() {
        createQuarkusProject(remoteRobot, NEW_QUARKUS_GRADLE_PROJECT_NAME, BuildTool.GRADLE, EndpointURLType.DEFAULT);
        ToolWindowPane toolWindowPane = remoteRobot.find(ToolWindowPane.class, Duration.ofSeconds(10));
        toolWindowPane.openGradleBuildToolPane();
        GradleBuildToolPane gradleBuildToolPane = toolWindowPane.find(GradleBuildToolPane.class, Duration.ofSeconds(10));

        // ISSUE #126: https://github.com/redhat-developer/intellij-common-ui-test-library/issues/126
        // gradleBuildToolPane.buildProject();
        buildGradleProject(gradleBuildToolPane);

        boolean isBuildSuccessful = toolWindowPane.find(BuildView.class, Duration.ofSeconds(10)).isBuildSuccessful();
        assertTrue(isBuildSuccessful, "The build should be successful but is not.");
    }

    private void createQuarkusProject(RemoteRobot remoteRobot, String projectName, BuildTool buildTool, EndpointURLType endpointURLType) {
        remoteRobot.find(FlatWelcomeFrame.class, Duration.ofSeconds(10)).createNewProject();
        NewProjectDialogWizard newProjectDialogWizard = remoteRobot.find(NewProjectDialogWizard.class, Duration.ofSeconds(10));
        QuarkusNewProjectFirstPage quarkusNewProjectFirstPage = newProjectDialogWizard.find(QuarkusNewProjectFirstPage.class, Duration.ofSeconds(10));
        quarkusNewProjectFirstPage.selectNewProjectType("Quarkus");

        if (endpointURLType == EndpointURLType.CUSTOM) {
            ComponentFixture customEndpointURLJBRadioButton = remoteRobot.findAll(ComponentFixture.class, byXpath(XPathDefinitions.CUSTOM_ENDPOINT_URL_RADIO_BUTTON)).get(0);
            customEndpointURLJBRadioButton.click();
            JTextFieldFixture customEndpointURLJTextField = remoteRobot.findAll(JTextFieldFixture.class, byXpath(XPathDefinitions.CUSTOM_ENDPOINT_URL_TEXT_FIELD)).get(0);
            customEndpointURLJTextField.setText("https://code.quarkus.io");
        }
        newProjectDialogWizard.next();

        QuarkusNewProjectSecondPage quarkusNewProjectSecondPage = newProjectDialogWizard.find(QuarkusNewProjectSecondPage.class, Duration.ofSeconds(10));
        quarkusNewProjectSecondPage.setBuildTool(buildTool);
        quarkusNewProjectSecondPage.setJavaVersion(JAVA_VERSION_FOR_QUARKUS_PROJECT);
        newProjectDialogWizard.next();
        newProjectDialogWizard.find(QuarkusNewProjectThirdPage.class, Duration.ofSeconds(10)); // wait for third page to be loaded
        newProjectDialogWizard.next();
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            Thread.interrupted();
            throw new RuntimeException(e);
        }

        QuarkusNewProjectFinalPage quarkusNewProjectFinalPage = newProjectDialogWizard.find(QuarkusNewProjectFinalPage.class, Duration.ofSeconds(10));
        quarkusNewProjectFinalPage.setProjectName(projectName);

        System.out.println("===============================================================");
        System.out.println(quarkusNewProjectFinalPage.getProjectLocation());
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

//        quarkusNewProjectFinalPage.setProjectLocation(CreateCloseUtils.PROJECT_LOCATION);

        System.out.println(quarkusNewProjectFinalPage.getProjectLocation());
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        System.out.println(quarkusNewProjectFinalPage.getProjectLocation());
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        System.out.println("===============================================================");
        ScreenshotUtils.takeScreenshot(remoteRobot, "lastProjectScreenshot");
        newProjectDialogWizard.finish();

        minimizeProjectImportPopupIfItAppears();

        IdeStatusBar ideStatusBar = remoteRobot.find(IdeStatusBar.class, Duration.ofSeconds(10));
        ideStatusBar.waitUntilProjectImportIsComplete();
        MainIdeWindow mainIdeWindow = remoteRobot.find(MainIdeWindow.class, Duration.ofSeconds(5));
        mainIdeWindow.maximizeIdeWindow();
        ideStatusBar.waitUntilAllBgTasksFinish(500);
    }

    private void minimizeProjectImportPopupIfItAppears() {
        try {
            remoteRobot.find(JButtonFixture.class, byXpath(XPathDefinitions.PROJECT_IMPORT_POPUP_MINIMIZE_BUTTON), Duration.ofSeconds(30)).click();
        } catch (WaitForConditionTimeoutException e) {
            e.printStackTrace();
        }
    }

    private void buildGradleProject(GradleBuildToolPane gradleBuildToolPane) {
        gradleBuildToolPane.find(JTreeFixture.class, JTreeFixture.Companion.byType(), Duration.ofSeconds(30));
        gradleBuildToolPane.expandAll();
        gradleBuildToolPane.gradleTaskTree().findAllText("build").get(1).doubleClick();
        if (UITestRunner.getIdeaVersionInt() >= 20221) {
            remoteRobot.find(ToolWindowPane.class).find(BuildView.class).waitUntilBuildHasFinished();
        } else {
            remoteRobot.find(ToolWindowsPane.class).find(BuildView.class).waitUntilBuildHasFinished();
        }
        remoteRobot.find(IdeStatusBar.class, Duration.ofSeconds(10)).waitUntilAllBgTasksFinish();
    }
}