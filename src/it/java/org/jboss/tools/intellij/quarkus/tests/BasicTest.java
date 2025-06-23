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
import com.intellij.remoterobot.fixtures.JTextFieldFixture;
import com.redhat.devtools.intellij.commonuitest.fixtures.dialogs.FlatWelcomeFrame;
import com.redhat.devtools.intellij.commonuitest.fixtures.dialogs.project.NewProjectDialogWizard;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.MainIdeWindow;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.idestatusbar.IdeStatusBar;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.toolwindowspane.BuildView;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.toolwindowspane.ToolWindowPane;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.toolwindowspane.buildtoolpane.GradleBuildToolPane;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.toolwindowspane.buildtoolpane.MavenBuildToolPane;
import com.redhat.devtools.intellij.commonuitest.utils.constants.ProjectLocation;
import com.redhat.devtools.intellij.commonuitest.utils.project.CreateCloseUtils;
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
import java.nio.file.Path;
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
    private static final String NEW_QUARKUS_MAVEN_PROJECT_NAME = "code-with-quarkus-maven";
    private static final String NEW_QUARKUS_GRADLE_PROJECT_NAME = "code-with-quarkus-gradle";
    private static final String JAVA_VERSION_FOR_QUARKUS_PROJECT = "17";

    @AfterEach
    public void finishTestRun() {
        CreateCloseUtils.closeProject(remoteRobot);
        FlatWelcomeFrame flatWelcomeFrame = remoteRobot.find(FlatWelcomeFrame.class, Duration.ofSeconds(10));
        flatWelcomeFrame.clearExceptions();
        flatWelcomeFrame.clearWorkspace();
    }

    @Test
    public void createBuildQuarkusMavenTest() throws IOException {
        createQuarkusProject(remoteRobot, NEW_QUARKUS_MAVEN_PROJECT_NAME, BuildTool.MAVEN, EndpointURLType.DEFAULT);
        ToolWindowPane toolWindowPane = remoteRobot.find(ToolWindowPane.class, Duration.ofSeconds(10));
        toolWindowPane.openMavenBuildToolPane();
        MavenBuildToolPane mavenBuildToolPane = toolWindowPane.find(MavenBuildToolPane.class, Duration.ofSeconds(10));
        mavenBuildToolPane.buildProject("verify", "code-with-quarkus");
        boolean isBuildSuccessful = toolWindowPane.find(BuildView.class, Duration.ofSeconds(10)).isBuildSuccessful();
        assertTrue(isBuildSuccessful, "The build should be successful but is not.");
    }

    @Test
    public void createBuildQuarkusGradleTest() throws IOException {
        createQuarkusProject(remoteRobot, NEW_QUARKUS_GRADLE_PROJECT_NAME, BuildTool.GRADLE, EndpointURLType.DEFAULT);
        ToolWindowPane toolWindowPane = remoteRobot.find(ToolWindowPane.class, Duration.ofSeconds(10));
        toolWindowPane.openGradleBuildToolPane();
        GradleBuildToolPane gradleBuildToolPane = toolWindowPane.find(GradleBuildToolPane.class, Duration.ofSeconds(10));

        gradleBuildToolPane.buildProject();

        boolean isBuildSuccessful = toolWindowPane.find(BuildView.class, Duration.ofSeconds(10)).isBuildSuccessful();
        assertTrue(isBuildSuccessful, "The build should be successful but is not.");
    }

    private void createQuarkusProject(RemoteRobot remoteRobot, String projectName, BuildTool buildTool, EndpointURLType endpointURLType) throws IOException {
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
        quarkusNewProjectFirstPage.setProjectSdkIfAvailable(JAVA_VERSION_FOR_QUARKUS_PROJECT);
        newProjectDialogWizard.next();

        QuarkusNewProjectSecondPage quarkusNewProjectSecondPage = newProjectDialogWizard.find(QuarkusNewProjectSecondPage.class, Duration.ofSeconds(10));
        quarkusNewProjectSecondPage.setBuildTool(buildTool);
        quarkusNewProjectSecondPage.setJavaVersion(JAVA_VERSION_FOR_QUARKUS_PROJECT);
        newProjectDialogWizard.next();

        newProjectDialogWizard.find(QuarkusNewProjectThirdPage.class, Duration.ofSeconds(10)); // wait for third page to be loaded
        newProjectDialogWizard.next();

        QuarkusNewProjectFinalPage quarkusNewProjectFinalPage = newProjectDialogWizard.find(QuarkusNewProjectFinalPage.class, Duration.ofSeconds(10));
        quarkusNewProjectFinalPage.setProjectName(projectName);
        String quarkusProjectLocation = ProjectLocation.PROJECT_LOCATION + File.separator + projectName;
        Path quarkusProjectDir = Paths.get(quarkusProjectLocation);
        boolean doesProjectDirExists = Files.exists(quarkusProjectDir);
        if (!doesProjectDirExists) {
            Files.createDirectories(quarkusProjectDir); // create project directory with project name to prevent "Directory does not exist. It will be created by Intellij. Create/Cancel" popup
        }
        quarkusNewProjectFinalPage.setProjectLocation(quarkusProjectLocation);
        newProjectDialogWizard.finish();

        MainIdeWindow mainIdeWindow = remoteRobot.find(MainIdeWindow.class, Duration.ofSeconds(5));
        mainIdeWindow.maximizeIdeWindow();

        IdeStatusBar ideStatusBar = remoteRobot.find(IdeStatusBar.class, Duration.ofSeconds(10));
        ideStatusBar.waitUntilProjectImportIsComplete();
        ideStatusBar.waitUntilAllBgTasksFinish();

    }

}