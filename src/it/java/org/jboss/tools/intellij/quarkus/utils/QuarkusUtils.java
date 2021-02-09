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
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;
import com.intellij.remoterobot.utils.Keyboard;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.NewProjectDialogFixture;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.WelcomeFrameDialogFixture;
import org.jboss.tools.intellij.quarkus.fixtures.quarkus.DownloadingOptionsDialogFixture;

import java.awt.*;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;

/**
 * Static utilities that assist and simplify the creation of new Quarkus project
 *
 * @author zcervink@redhat.com
 */
public class QuarkusUtils {

    public static void createNewQuarkusProject(RemoteRobot remoteRobot, BuildUtils.ToolToBuildTheProject toolToBuildTheProject) {
        step("Create new Quarkus project", () -> {
            final WelcomeFrameDialogFixture welcomeFrameDialogFixture = remoteRobot.find(WelcomeFrameDialogFixture.class);
            welcomeFrameDialogFixture.createNewProjectLink().click();
            final NewProjectDialogFixture newProjectDialogFixture = welcomeFrameDialogFixture.find(NewProjectDialogFixture.class, Duration.ofSeconds(20));

            NewProjectDialogUtils.selectNewProjectType(remoteRobot, "Quarkus");
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
}