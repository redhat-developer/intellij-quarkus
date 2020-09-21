/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.intellij.quarkus.utils;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException;
import org.apache.commons.io.FileUtils;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.IdeFatalErrorsDialogFixture;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.TipOfTheDayDialogFixture;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.WelcomeFrameDialogFixture;
import org.jboss.tools.intellij.quarkus.fixtures.mainIdeWindow.IdeStatusBarFixture;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;

/**
 * Static utilities that assist and simplify manipulation with the IDE and with the project
 *
 * @author zcervink@redhat.com
 */
public class GlobalUtils {

    public static String projectPath = "";

    public static void closeTheTipOfTheDayDialog(RemoteRobot remoteRobot) {
        step("Close the 'Tip of the Day' Dialog", () -> {
            final TipOfTheDayDialogFixture tipOfTheDayDialogFixture = remoteRobot.find(TipOfTheDayDialogFixture.class, Duration.ofSeconds(20));
            tipOfTheDayDialogFixture.button("Close").click();
        });
    }

    public static void waitUntilTheProjectImportIsComplete(RemoteRobot remoteRobot) {
        step("Wait until the project import is complete", () -> {
            while (true) {
                try {
                    ComponentFixture cf = remoteRobot.find(ComponentFixture.class, byXpath("//div[@class='EngravedLabel']"));
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (WaitForConditionTimeoutException e) {
                    // the importing of the project has finished -> exit the loop
                    return;
                }
            }
        });
    }

    public static void maximizeTheIdeWindow(RemoteRobot remoteRobot) {
        step("Maximize the IDE window", () -> {
            ComponentFixture cf = remoteRobot.find(ComponentFixture.class, byXpath("//div[@class='IdeFrameImpl']"));
            cf.runJs("const horizontal_offset = component.getWidth()/2;\n" +
                    "robot.click(component, new Point(horizontal_offset, 10), MouseButton.LEFT_BUTTON, 2);");
        });
    }

    public static void waitUntilAllTheBgTasksFinish(RemoteRobot remoteRobot) {
        step("Wait until all the background tasks finish", () -> {
            final IdeStatusBarFixture ideStatusBarFixture = remoteRobot.find(IdeStatusBarFixture.class);

            byte numberOfSecondsWithNoBgTask = 0;
            final byte BREAK_WAITING_AFTER_SECONDS = 5;

            do {
                try {
                    while (true) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        ideStatusBarFixture.bgTasksIcon();

                        numberOfSecondsWithNoBgTask = 0;
                    }
                } catch (WaitForConditionTimeoutException e) {
                    numberOfSecondsWithNoBgTask++;
                }
            } while (numberOfSecondsWithNoBgTask < BREAK_WAITING_AFTER_SECONDS);
        });
    }

    public static void checkForExceptions(RemoteRobot remoteRobot) {
        step("Check for exceptions and other errors", () -> {
            final IdeStatusBarFixture ideStatusBarFixture = remoteRobot.find(IdeStatusBarFixture.class);
            try {
                ideStatusBarFixture.ideErrorsIcon();
            } catch (WaitForConditionTimeoutException e) {
                return;
            }
            ideStatusBarFixture.ideErrorsIcon().click();

            final IdeFatalErrorsDialogFixture ideFatalErrorsDialogFixture = remoteRobot.find(IdeFatalErrorsDialogFixture.class, Duration.ofSeconds(10));
            String exceptionNumberLabel = ideFatalErrorsDialogFixture.numberOfExcetionsJBLabel().findAllText().get(0).getText();
            int numberOfExceptions = Integer.parseInt(exceptionNumberLabel.substring(5));

            for (int i = 0; i < numberOfExceptions; i++) {
                String exceptionStackTrace = HelperUtils.listOfRemoteTextToString(ideFatalErrorsDialogFixture.exceptionDescriptionJTextArea().findAllText());

                if (i + 1 < numberOfExceptions) {
                    ideFatalErrorsDialogFixture.nextExceptionButton().click();
                }
            }

            ideFatalErrorsDialogFixture.button("Clear all").click();
        });
    }

    public static void closeTheProject(RemoteRobot remoteRobot) {
        step("Close the project that is currently open", () -> {
            ComponentFixture cf = remoteRobot.find(ComponentFixture.class, byXpath("//div[@class='IdeFrameImpl']"));
            if (remoteRobot.isMac()) {
                cf.runJs("robot.click(component, new Point(15, 10), MouseButton.LEFT_BUTTON, 1);");
            } else if (remoteRobot.isWin()) {
                cf.runJs("const horizontal_offset = component.getWidth()-24;\n" +
                        "robot.click(component, new Point(horizontal_offset, 18), MouseButton.LEFT_BUTTON, 1);");
            } else {
                cf.runJs("const horizontal_offset = component.getWidth()-18;\n" +
                        "robot.click(component, new Point(horizontal_offset, 18), MouseButton.LEFT_BUTTON, 1);");
            }

            final WelcomeFrameDialogFixture welcomeFrameDialogFixture = remoteRobot.find(WelcomeFrameDialogFixture.class, Duration.ofSeconds(10));
            welcomeFrameDialogFixture.runJs("const horizontal_offset = component.getWidth()/2;\n" +
                    "robot.click(component, new Point(horizontal_offset, 10), MouseButton.LEFT_BUTTON, 1);");
        });
    }

    public static void clearTheWorkspace(RemoteRobot remoteRobot) {
        step("Delete all the projects in the workspace", () -> {
            // delete all the projects' links from the 'Welcome to IntelliJ IDEA' dialog
            final WelcomeFrameDialogFixture welcomeFrameDialogFixture = remoteRobot.find(WelcomeFrameDialogFixture.class, Duration.ofSeconds(10));

            while (true) {
                try {
                    ComponentFixture cf = welcomeFrameDialogFixture.find(ComponentFixture.class, byXpath("//div[@accessiblename='Recent Projects' and @class='MyList']"));
                    cf.runJs("const horizontal_offset = component.getWidth()-22;\n" +
                            "robot.click(component, new Point(horizontal_offset, 22), MouseButton.LEFT_BUTTON, 1);");
                } catch (WaitForConditionTimeoutException e) {
                    break;
                }
            }

            // delete all the files and folders in the IdeaProjects folder
            try {
                String pathToDirToMakeEmpty = System.getProperty("user.home") + File.separator + "IdeaProjects";
                FileUtils.cleanDirectory(new File(pathToDirToMakeEmpty));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void quitIntelliJFromTheWelcomeDialog(RemoteRobot remoteRobot) {
        step("Quit IntelliJ Idea from the 'Welcome To IntelliJ IDEA' dialog", () -> {
            WelcomeFrameDialogFixture welcomeFrameDialogFixture = remoteRobot.find(WelcomeFrameDialogFixture.class);
            if (remoteRobot.isMac()) {
                welcomeFrameDialogFixture.runJs("robot.click(component, new Point(15, 10), MouseButton.LEFT_BUTTON, 1);");
            } else if (remoteRobot.isWin()) {
                welcomeFrameDialogFixture.runJs("const horizontal_offset = component.getWidth()-24;\n" +
                        "robot.click(component, new Point(horizontal_offset, 18), MouseButton.LEFT_BUTTON, 1);");
            } else {
                welcomeFrameDialogFixture.runJs("const horizontal_offset = component.getWidth()-18;\n" +
                        "robot.click(component, new Point(horizontal_offset, 18), MouseButton.LEFT_BUTTON, 1);");
            }
        });
    }
}