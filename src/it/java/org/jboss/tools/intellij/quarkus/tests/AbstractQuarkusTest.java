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
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException;
import com.redhat.devtools.intellij.commonuitest.UITestRunner;
import com.redhat.devtools.intellij.commonuitest.fixtures.dialogs.FlatWelcomeFrame;
import com.redhat.devtools.intellij.commonuitest.fixtures.dialogs.project.NewProjectDialogWizard;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.MainIdeWindow;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.idestatusbar.IdeStatusBar;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.toolwindowspane.ProjectExplorer;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.toolwindowspane.ToolWindowPane;
import com.redhat.devtools.intellij.commonuitest.utils.constants.ProjectLocation;
import com.redhat.devtools.intellij.commonuitest.utils.project.CreateCloseUtils;
import com.redhat.devtools.intellij.commonuitest.utils.runner.IntelliJVersion;
import com.redhat.devtools.intellij.commonuitest.utils.screenshot.ScreenshotUtils;
import com.redhat.devtools.intellij.commonuitest.utils.testextension.ScreenshotAfterTestFailExtension;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.project.pages.QuarkusNewProjectFinalPage;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.project.pages.QuarkusNewProjectFirstPage;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.project.pages.QuarkusNewProjectSecondPage;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.project.pages.QuarkusNewProjectThirdPage;
import org.jboss.tools.intellij.quarkus.utils.BuildTool;
import org.jboss.tools.intellij.quarkus.utils.EndpointURLType;
import org.jboss.tools.intellij.quarkus.utils.XPathDefinitions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;

/**
 * Abstract test class
 *
 * @author zcervink@redhat.com
 */
@ExtendWith(ScreenshotAfterTestFailExtension.class)
public abstract class AbstractQuarkusTest {
    protected static RemoteRobot remoteRobot;
    private static boolean intelliJHasStarted = false;

    AbstractQuarkusTest() {
    }

    @BeforeAll
    protected static void startIntelliJ() {
        if (!intelliJHasStarted) {
            remoteRobot = UITestRunner.runIde(IntelliJVersion.COMMUNITY_V_2023_3, 8580);
            intelliJHasStarted = true;
            Runtime.getRuntime().addShutdownHook(new CloseIntelliJBeforeQuit());

            FlatWelcomeFrame flatWelcomeFrame = remoteRobot.find(FlatWelcomeFrame.class, Duration.ofSeconds(10));
            flatWelcomeFrame.disableNotifications();
            flatWelcomeFrame.preventTipDialogFromOpening();
        }
    }

    private static class CloseIntelliJBeforeQuit extends Thread {
        @Override
        public void run() {
            UITestRunner.closeIde();
        }
    }

    @AfterEach
    protected void clearWorkspace() {
        CreateCloseUtils.closeProject(remoteRobot);
        FlatWelcomeFrame flatWelcomeFrame = remoteRobot.find(FlatWelcomeFrame.class, Duration.ofSeconds(10));
        flatWelcomeFrame.clearExceptions();
        flatWelcomeFrame.clearWorkspace();
    }

    protected void createQuarkusProject(RemoteRobot remoteRobot, String projectName, BuildTool buildTool, EndpointURLType endpointURLType, String javaVersion) throws IOException {
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
        quarkusNewProjectFirstPage.setProjectSdkIfAvailable(javaVersion);
        newProjectDialogWizard.next();

        QuarkusNewProjectSecondPage quarkusNewProjectSecondPage = newProjectDialogWizard.find(QuarkusNewProjectSecondPage.class, Duration.ofSeconds(10));
        quarkusNewProjectSecondPage.setBuildTool(buildTool);
        quarkusNewProjectSecondPage.setJavaVersion(javaVersion);
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
        // wait for project to open
        waitFor(Duration.ofSeconds(5), Duration.ofSeconds(1), "main ide window to open", this::isMainIdeWindowOpen);

        ScreenshotUtils.takeScreenshot(remoteRobot, "after wait for main window to open");

        waitFor(Duration.ofSeconds(600), Duration.ofSeconds(10), "the project explorer to finish initializing.", this::didProjectExplorerFinishInit);
        ScreenshotUtils.takeScreenshot(remoteRobot, "after waiting");
    }

    private Boolean isMainIdeWindowOpen() {
        try {
            return remoteRobot.find(MainIdeWindow.class, Duration.ofSeconds(5)).isShowing();
        } catch (WaitForConditionTimeoutException e) {
            return false;
        }
    }

    private boolean didProjectExplorerFinishInit() {
        try {
            ToolWindowPane toolWinPane = remoteRobot.find(ToolWindowPane.class, Duration.ofSeconds(10));
            ProjectExplorer projectExplorer = toolWinPane.find(ProjectExplorer.class, Duration.ofSeconds(10));
            return projectExplorer.projectViewTree().findAllText().stream().noneMatch(remoteText -> remoteText.getText().contains("loading"));
        } catch (WaitForConditionTimeoutException e) {
            return true;
        }
    }

}