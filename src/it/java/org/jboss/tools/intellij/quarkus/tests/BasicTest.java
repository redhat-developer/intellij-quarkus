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
import com.intellij.remoterobot.fixtures.JRadioButtonFixture;
import com.intellij.remoterobot.fixtures.JTextFieldFixture;
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException;
import com.redhat.devtools.intellij.commonuitest.fixtures.dialogs.FlatWelcomeFrame;
import com.redhat.devtools.intellij.commonuitest.fixtures.dialogs.information.TipDialog;
import com.redhat.devtools.intellij.commonuitest.fixtures.dialogs.project.NewProjectDialogWizard;
import com.redhat.devtools.intellij.commonuitest.fixtures.dialogs.project.pages.JavaNewProjectFinalPage;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.MainIdeWindow;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.idestatusbar.IdeStatusBar;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.toolwindowspane.BuildView;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.toolwindowspane.ProjectExplorer;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.toolwindowspane.ToolWindowsPane;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.toolwindowspane.buildtoolpane.MavenBuildToolPane;
import com.redhat.devtools.intellij.commonuitest.utils.project.CreateCloseUtils;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.DownloadingOptionsDialog;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.project.pages.JavaNewProjectFirstPage;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.project.pages.QuarkusNewProjectFinalPage;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.project.pages.QuarkusNewProjectFirstPage;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.project.pages.QuarkusNewProjectSecondPage;
import org.jboss.tools.intellij.quarkus.utils.Enums;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
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
    private final String NEW_QUARKUS_RUNTIME_DOWNLOAD_PROJECT_NAME = "quarkus-runtime-download";

    @AfterEach
    public void finishTestRun() {
        CreateCloseUtils.closeProject(remoteRobot);
        FlatWelcomeFrame flatWelcomeFrame = remoteRobot.find(FlatWelcomeFrame.class, Duration.ofSeconds(10));
        flatWelcomeFrame.clearExceptions();
        flatWelcomeFrame.clearWorkspace();
    }

    @Test
    public void createBuildQuarkusMavenTest() {
        createQuarkusProject(remoteRobot, NEW_QUARKUS_MAVEN_PROJECT_NAME, Enums.BuildTool.MAVEN, Enums.EndpointURLType.DEFAULT);
        ToolWindowsPane toolWindowsPane = remoteRobot.find(ToolWindowsPane.class, Duration.ofSeconds(10));
        toolWindowsPane.openMavenBuildToolPane();
        MavenBuildToolPane mavenBuildToolPane = toolWindowsPane.find(MavenBuildToolPane.class, Duration.ofSeconds(10));
        mavenBuildToolPane.buildProject("install");
        boolean isBuildSuccessful = toolWindowsPane.find(BuildView.class, Duration.ofSeconds(10)).isBuildSuccessful();
        assertTrue(isBuildSuccessful, "The build should be successful but is not.");
    }

    @Test
    public void downloadQuarkusRuntimeTest() {
        String runtimeJarName = createJavaProjectWithQuarkus(remoteRobot, NEW_QUARKUS_RUNTIME_DOWNLOAD_PROJECT_NAME);
        ToolWindowsPane toolWindowsPane = remoteRobot.find(ToolWindowsPane.class, Duration.ofSeconds(10));
        toolWindowsPane.openProjectExplorer();
        ProjectExplorer projectExplorer = toolWindowsPane.find(ProjectExplorer.class, Duration.ofSeconds(10));
        boolean isRuntimeAvailable = projectExplorer.isItemPresent(NEW_QUARKUS_RUNTIME_DOWNLOAD_PROJECT_NAME, "lib", runtimeJarName);
        assertTrue(isRuntimeAvailable, "The Quarkus runtime has not been downloaded.");
    }

    private void createQuarkusProject(RemoteRobot remoteRobot, String projectName, Enums.BuildTool buildTool, Enums.EndpointURLType endpointURLType) {
        remoteRobot.find(FlatWelcomeFrame.class, Duration.ofSeconds(10)).createNewProject();
        NewProjectDialogWizard newProjectDialogWizard = remoteRobot.find(NewProjectDialogWizard.class, Duration.ofSeconds(10));
        QuarkusNewProjectFirstPage quarkusNewProjectFirstPage = newProjectDialogWizard.find(QuarkusNewProjectFirstPage.class, Duration.ofSeconds(10));
        quarkusNewProjectFirstPage.selectNewProjectType("Quarkus");

        if (endpointURLType == Enums.EndpointURLType.CUSTOM) {
            ComponentFixture customEndpointURLJBRadioButton = remoteRobot.findAll(ComponentFixture.class, byXpath("//div[@accessiblename='Custom:' and @class='JBRadioButton' and @text='Custom:']")).get(0);
            customEndpointURLJBRadioButton.click();
            JTextFieldFixture customEndpointURLJTextField = remoteRobot.findAll(JTextFieldFixture.class, byXpath("//div[@class='BorderlessTextField']")).get(0);
            customEndpointURLJTextField.setText("https://code.quarkus.io");
        }
        newProjectDialogWizard.next();

        QuarkusNewProjectSecondPage quarkusNewProjectSecondPage = newProjectDialogWizard.find(QuarkusNewProjectSecondPage.class, Duration.ofSeconds(10));
        quarkusNewProjectSecondPage.setBuildTool(buildTool);
        newProjectDialogWizard.next();
        newProjectDialogWizard.next();
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            Thread.interrupted();
            throw new RuntimeException(e);
        }

        QuarkusNewProjectFinalPage quarkusNewProjectFinalPage = newProjectDialogWizard.find(QuarkusNewProjectFinalPage.class, Duration.ofSeconds(10));
        quarkusNewProjectFinalPage.setProjectName(projectName);
        newProjectDialogWizard.finish();

        minimizeProjectImportPopupIfItAppears();

        IdeStatusBar ideStatusBar = remoteRobot.find(IdeStatusBar.class, Duration.ofSeconds(10));
        ideStatusBar.waitUntilProjectImportIsComplete();
        MainIdeWindow mainIdeWindow = remoteRobot.find(MainIdeWindow.class, Duration.ofSeconds(5));
        mainIdeWindow.maximizeIdeWindow();
        ideStatusBar.waitUntilAllBgTasksFinish();
    }

    private String createJavaProjectWithQuarkus(RemoteRobot remoteRobot, String projectName) {
        FlatWelcomeFrame flatWelcomeFrameFixture = remoteRobot.find(FlatWelcomeFrame.class);
        flatWelcomeFrameFixture.createNewProject();
        NewProjectDialogWizard newProjectDialogWizard = remoteRobot.find(NewProjectDialogWizard.class, Duration.ofSeconds(10));
        JavaNewProjectFirstPage javaNewProjectFirstPage = newProjectDialogWizard.find(JavaNewProjectFirstPage.class, Duration.ofSeconds(10));
        javaNewProjectFirstPage.selectNewProjectType("Java");
        javaNewProjectFirstPage.frameworksTree().findText("Quarkus").click();

        Point quarkusCheckboxLocation = javaNewProjectFirstPage.frameworksTree().findText("Quarkus").getPoint();
        quarkusCheckboxLocation.x = 10;
        javaNewProjectFirstPage.frameworksTree().click(quarkusCheckboxLocation);

        JRadioButtonFixture downloadRadioButton = javaNewProjectFirstPage.find(JRadioButtonFixture.class, byXpath("//div[@accessiblename='Download' and @class='JRadioButton' and @text='Download']"), Duration.ofSeconds(10));
        downloadRadioButton.click();
        javaNewProjectFirstPage.button("Configure...").click();

        DownloadingOptionsDialog downloadingOptionsDialogFixture = remoteRobot.find(DownloadingOptionsDialog.class);
        String runtimeJarName = downloadingOptionsDialogFixture.filesToDownload().findAllText().get(0).getText();
        downloadingOptionsDialogFixture.button("Cancel").click();
        newProjectDialogWizard.next();

        JavaNewProjectFinalPage javaNewProjectFinalPage = newProjectDialogWizard.find(JavaNewProjectFinalPage.class, Duration.ofSeconds(10));
        javaNewProjectFinalPage.setProjectName(projectName);
        newProjectDialogWizard.finish();

        IdeStatusBar ideStatusBar = remoteRobot.find(IdeStatusBar.class, Duration.ofSeconds(10));
        ideStatusBar.waitUntilProjectImportIsComplete();
        MainIdeWindow mainIdeWindow = remoteRobot.find(MainIdeWindow.class, Duration.ofSeconds(5));
        mainIdeWindow.maximizeIdeWindow();
        ideStatusBar.waitUntilAllBgTasksFinish();

        return runtimeJarName;
    }

    private void minimizeProjectImportPopupIfItAppears() {
        try {
            remoteRobot.find(JButtonFixture.class, byXpath("//div[@text='Background']"), Duration.ofSeconds(30)).click();
        } catch (WaitForConditionTimeoutException e) {
            e.printStackTrace();
        }
    }
}