/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.intellij.quarkus.utils;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.JRadioButtonFixture;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.ContainerFixture;
import com.intellij.remoterobot.fixtures.JTextFieldFixture;
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;
import com.intellij.remoterobot.utils.Keyboard;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.NewProjectDialogFixture;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.WelcomeFrameDialogFixture;
import org.jboss.tools.intellij.quarkus.fixtures.quarkus.DownloadingOptionsDialogFixture;

import java.awt.Point;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static org.jboss.tools.intellij.quarkus.utils.HelperUtils.listOfRemoteTextToString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;

/**
 * Static utilities that assist and simplify the creation of new Quarkus project
 *
 * @author zcervink@redhat.com
 */
public class QuarkusUtils {

    public static void createNewQuarkusProject(RemoteRobot remoteRobot, BuildUtils.ToolToBuildTheProject toolToBuildTheProject, EndpointURLType endpointURLType) {
        step("Create new Quarkus project", () -> {
            final WelcomeFrameDialogFixture welcomeFrameDialogFixture = remoteRobot.find(WelcomeFrameDialogFixture.class);
            welcomeFrameDialogFixture.createNewProjectLink().click();
            final NewProjectDialogFixture newProjectDialogFixture = welcomeFrameDialogFixture.find(NewProjectDialogFixture.class, Duration.ofSeconds(20));
            NewProjectDialogUtils.selectNewProjectType(remoteRobot, "Quarkus");

            if (endpointURLType == EndpointURLType.CUSTOM) {
                ComponentFixture customEndpointURLJBRadioButton = remoteRobot.find(ComponentFixture.class, byXpath("//div[@accessiblename='Custom:' and @class='JBRadioButton' and @text='Custom:']"));
                customEndpointURLJBRadioButton.click();
                JTextFieldFixture customEndpointURLJTextField = remoteRobot.find(JTextFieldFixture.class, byXpath("//div[@class='BorderlessTextField']"));
                customEndpointURLJTextField.setText("https://code.quarkus.io");
            }

            newProjectDialogFixture.button("Next").click();

            switch (toolToBuildTheProject) {
                case GRADLE:
                    newProjectDialogFixture.toolComboBox().click();
                    new Keyboard(remoteRobot).hotKey(40);
                    break;
                case MAVEN:
                    break;
            }

            newProjectDialogFixture.button("Next").click();
            selectQuarkusExtensions(newProjectDialogFixture, 2, new int[]{0, 1, 3});
            selectQuarkusExtensions(newProjectDialogFixture, 10, new int[]{1, 2, 3});

            newProjectDialogFixture.button("Next").click();
            String newProjectName = "code-with-quarkus-" + toolToBuildTheProject.toString().toLowerCase();
            newProjectDialogFixture.projectNameJTextField().setText(newProjectName);

            GlobalUtils.projectPath = newProjectDialogFixture.projectLocationJTextField().getText();
            newProjectDialogFixture.button("Finish").click();
        });
    }

    public static void tryToCreateNewQuarkusProjectWithInvalidCustomEndpointURL(RemoteRobot remoteRobot) {
        step("Try to create new Quarkus project with invalid custom endpoint URL", () -> {
            final WelcomeFrameDialogFixture welcomeFrameDialogFixture = remoteRobot.find(WelcomeFrameDialogFixture.class);
            welcomeFrameDialogFixture.createNewProjectLink().click();
            final NewProjectDialogFixture newProjectDialogFixture = welcomeFrameDialogFixture.find(NewProjectDialogFixture.class, Duration.ofSeconds(20));
            NewProjectDialogUtils.selectNewProjectType(remoteRobot, "Quarkus");

            ComponentFixture customEndpointURLJBRadioButton = remoteRobot.find(ComponentFixture.class, byXpath("//div[@accessiblename='Custom:' and @class='JBRadioButton' and @text='Custom:']"));
            customEndpointURLJBRadioButton.click();
            JTextFieldFixture customEndpointURLJTextField = remoteRobot.find(JTextFieldFixture.class, byXpath("//div[@class='BorderlessTextField']"));

            final String[] endpointURLs = {"https://invalid.url"};
            for (String url : endpointURLs) {
                customEndpointURLJTextField.setText(url);
                newProjectDialogFixture.button("Next").click();

                if (remoteRobot.isMac()) {
                    ContainerFixture jDialog = remoteRobot.find(ContainerFixture.class, byXpath("//div[@class='JDialog']"));
                    waitFor(Duration.ofSeconds(10), Duration.ofSeconds(1), "The 2 JEditorPane elements did not appear in 10 seconds.", () -> jDialog.findAll(ComponentFixture.class, byXpath("//div[@class='JEditorPane']")).size() == 2);
                    List<ComponentFixture> jEditorPanes = jDialog.findAll(ComponentFixture.class, byXpath("//div[@class='JEditorPane']"));
                    String text1 = listOfRemoteTextToString(jEditorPanes.get(0).findAllText());
                    String text2 = listOfRemoteTextToString(jEditorPanes.get(1).findAllText());
                    assertTrue(text1.toLowerCase(Locale.ROOT).contains("cannot save settings"), "The dialog should contain the 'Cannot Save Settings' message.");
                    assertTrue(text2.toLowerCase(Locale.ROOT).contains("invalid custom quarkus code endpoint url"), "The dialog should contain the 'Invalid custom Quarkus Code endpoint URL' message.");
                    ComponentFixture popupOkButton = jDialog.find(ComponentFixture.class, byXpath("//div[@accessiblename='OK' and @class='JButton' and @name='OK' and @text='OK']"), Duration.ofSeconds(10));
                    popupOkButton.click();
                } else {
                    ContainerFixture myDialog = remoteRobot.find(ContainerFixture.class, byXpath("//div[@accessiblename='Cannot Save Settings' and @class='MyDialog']"));
                    List<RemoteText> dialogMsgRemoteText = myDialog.find(ComponentFixture.class, byXpath("//div[@class='JTextPane']")).findAllText();
                    String dialogMsg = listOfRemoteTextToString(dialogMsgRemoteText);
                    assertTrue(dialogMsg.toLowerCase(Locale.ROOT).contains("invalid custom quarkus code endpoint url"), "The dialog should contain the 'Invalid custom Quarkus Code endpoint URL' message but is '" + dialogMsg + "'");
                    myDialog.find(ComponentFixture.class, byXpath("//div[@accessiblename='OK' and @class='JButton' and @text='OK']")).click();
                }
            }

            newProjectDialogFixture.button("Cancel").click();
        });
    }

    public static void selectQuarkusExtensions(NewProjectDialogFixture newProjectDialogFixture, int categoryIndex, int[] extensionsIndexes) {
        step("Select Quarkus extensions", () -> {
            List<String> extensionCategoriesRenderedText = newProjectDialogFixture.extensionCategoriesJBList().findAllText()
                    .stream()
                    .map(RemoteText::getText)
                    .collect(Collectors.toList());

            newProjectDialogFixture.extensionCategoriesJBList().findText(extensionCategoriesRenderedText.get(categoryIndex)).click();

            for (int index : extensionsIndexes) {
                Point positionInTheExtensionsTable = new Point(10, 10 + index * 24);
                newProjectDialogFixture.extensionsTable().click(positionInTheExtensionsTable);
            }
        });
    }

    public static String createNewJavaProjectWithQuarkusFramework(RemoteRobot remoteRobot, String projectName) {
        final WelcomeFrameDialogFixture welcomeFrameDialogFixture = remoteRobot.find(WelcomeFrameDialogFixture.class);
        welcomeFrameDialogFixture.createNewProjectLink().click();
        final NewProjectDialogFixture newProjectDialogFixture = welcomeFrameDialogFixture.find(NewProjectDialogFixture.class, Duration.ofSeconds(20));
        NewProjectDialogUtils.selectNewProjectType(remoteRobot, "Java");

        newProjectDialogFixture.theFrameworksTree().findText("Quarkus").click();
        Point quarkusCheckboxLocation = newProjectDialogFixture.theFrameworksTree().findText("Quarkus").getPoint();
        quarkusCheckboxLocation.x = 10;
        newProjectDialogFixture.theFrameworksTree().click(quarkusCheckboxLocation);

        JRadioButtonFixture theDownloadRadioButton = newProjectDialogFixture.find(JRadioButtonFixture.class, byXpath("//div[@accessiblename='Download' and @class='JRadioButton' and @text='Download']"), Duration.ofSeconds(10));
        theDownloadRadioButton.click();
        newProjectDialogFixture.button("Configure...").click();

        DownloadingOptionsDialogFixture dodf = newProjectDialogFixture.find(DownloadingOptionsDialogFixture.class);
        String runtimeJarName = dodf.filesToDownload().findAllText().get(0).getText();
        dodf.button("Cancel").click();
        newProjectDialogFixture.button("Next").click();

        newProjectDialogFixture.projectNameField().setText(projectName);
        newProjectDialogFixture.button("Finish").click();

        return runtimeJarName;
    }

    public enum EndpointURLType {
        DEFAULT,
        CUSTOM
    }
}