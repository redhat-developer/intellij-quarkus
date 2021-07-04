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
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.ContainerFixture;
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;
import com.intellij.remoterobot.utils.Keyboard;
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.TipOfTheDayDialog;
import com.redhat.devtools.intellij.commonUiTestLibrary.fixtures.dialogs.WelcomeFrameDialog;
import org.jboss.tools.intellij.quarkus.fixtures.mainIdeWindow.CustomHeaderMenuBar;
import org.jboss.tools.intellij.quarkus.fixtures.mainIdeWindow.IdeStatusBar;
import org.jboss.tools.intellij.quarkus.fixtures.mainIdeWindow.LinuxIdeMenuBar;
import org.jboss.tools.intellij.quarkus.fixtures.popups.SearchEverywherePopup;

import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static com.redhat.devtools.intellij.commonUiTestLibrary.utils.HelperUtils.listOfRemoteTextToString;

/**
 * Static utilities that assist and simplify manipulation with the IDE and with the project
 *
 * @author zcervink@redhat.com
 */
public class GlobalUtils {

    public static String projectPath = "";

    public static void closeTheTipOfTheDayDialogIfItAppears(RemoteRobot remoteRobot) {
        step("Close the 'Tip of the Day' Dialog", () -> {
            try {
                final TipOfTheDayDialog tipOfTheDayDialogFixture = remoteRobot.find(TipOfTheDayDialog.class, Duration.ofSeconds(20));
                tipOfTheDayDialogFixture.button("Close").click();
            } catch (WaitForConditionTimeoutException e) {
                e.printStackTrace();
            }
        });
    }

    public static void waitUntilTheProjectImportIsComplete(RemoteRobot remoteRobot) {
        step("Wait until the project import is complete", () -> {
            waitFor(Duration.ofSeconds(300), Duration.ofSeconds(5), "The project import did not finish in 5 minutes.", () -> didTheProjectImportFinish(remoteRobot));
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
            waitFor(Duration.ofSeconds(300), Duration.ofSeconds(15), "The background tasks did not finish in 5 minutes.", () -> didAllTheBgTasksFinish(remoteRobot));
        });
    }

    public static void closeTheProject(RemoteRobot remoteRobot) {
        step("Close the project that is currently open", () -> {
            ComponentFixture cf = remoteRobot.find(ComponentFixture.class, byXpath("//div[@class='IdeFrameImpl']"));
            if (remoteRobot.isMac()) {
                cf.runJs("robot.click(component, new Point(15, 10), MouseButton.LEFT_BUTTON, 1);");
            } else if (remoteRobot.isWin()) {
                CustomHeaderMenuBar chmb = remoteRobot.find(CustomHeaderMenuBar.class, Duration.ofSeconds(10));
                chmb.mainMenuItem("File").click();
                List<ContainerFixture> allHeavyWeightWindows = remoteRobot.findAll(ContainerFixture.class, byXpath("//div[@class='HeavyWeightWindow']"));
                ContainerFixture lastHeavyWeightWindow = allHeavyWeightWindows.get(allHeavyWeightWindows.size() - 1);
                ComponentFixture closeProjectButtonFixture = lastHeavyWeightWindow.find(ComponentFixture.class, byXpath("//div[@accessiblename='Close Project' and @text='Close Project']"));
                closeProjectButtonFixture.click();
            } else {
                LinuxIdeMenuBar limb = remoteRobot.find(LinuxIdeMenuBar.class, Duration.ofSeconds(10));
                limb.mainMenuItem("File").click();
                List<ContainerFixture> allHeavyWeightWindows = remoteRobot.findAll(ContainerFixture.class, byXpath("//div[@class='HeavyWeightWindow']"));
                ContainerFixture lastHeavyWeightWindow = allHeavyWeightWindows.get(allHeavyWeightWindows.size() - 1);
                ComponentFixture closeProjectButtonFixture = lastHeavyWeightWindow.find(ComponentFixture.class, byXpath("//div[@accessiblename='Close Project' and @text='Close Project']"));
                closeProjectButtonFixture.click();
            }

            final WelcomeFrameDialog welcomeFrameDialogFixture = remoteRobot.find(WelcomeFrameDialog.class, Duration.ofSeconds(10));
            welcomeFrameDialogFixture.runJs("const horizontal_offset = component.getWidth()/2;\n" +
                    "robot.click(component, new Point(horizontal_offset, 10), MouseButton.LEFT_BUTTON, 1);");
        });
    }

    public static void quitIntelliJFromTheWelcomeDialog(RemoteRobot remoteRobot) {
        step("Quit IntelliJ Idea from the 'Welcome To IntelliJ IDEA' dialog", () -> {
            WelcomeFrameDialog welcomeFrameDialogFixture = remoteRobot.find(WelcomeFrameDialog.class);
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

    public static IdeaVersion getTheIntelliJVersion(RemoteRobot remoteRobot) {
        WelcomeFrameDialog welcomeFrameDialogFixture = remoteRobot.find(WelcomeFrameDialog.class);
        List<ComponentFixture> jLabels = welcomeFrameDialogFixture.findAll(ComponentFixture.class, byXpath("//div[@class='JLabel']"));

        IdeaVersion ideaVersion = IdeaVersion.UNIDENTIFIED;
        for (ComponentFixture jLabel : jLabels) {
            String labelText = listOfRemoteTextToString(jLabel.findAllText()).toLowerCase(Locale.ROOT);
            if (labelText.contains("20")) {
                if (labelText.contains("2020.1")) {
                    ideaVersion = IdeaVersion.V2020_1;
                } else if (labelText.contains("2020.2")) {
                    ideaVersion = IdeaVersion.V2020_2;
                }
                break;
            }
        }
        return ideaVersion;
    }

    public static void takeScreenshot() {
        String screenshotLocation = "./build/screenshots/";
        String screenshotFilename = getTimeNowAsString("yyyy_MM_dd_HH_mm_ss");
        String filetype = "png";
        String screenshotPathname = screenshotLocation + screenshotFilename + "." + filetype;

        try {
            BufferedImage screenshotBufferedImage = getScreenshotAsBufferedImage();
            boolean doesTheScreenshotDirExists = Files.exists(Paths.get(screenshotLocation));
            if (!doesTheScreenshotDirExists) {
                Files.createDirectory(Paths.get(screenshotLocation));
            }
            File screenshotFile = new File(screenshotPathname);
            ImageIO.write(screenshotBufferedImage, filetype, screenshotFile);
        } catch (AWTException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BufferedImage getScreenshotAsBufferedImage() throws AWTException {
        Rectangle fullscreenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage screenshot = new Robot().createScreenCapture(fullscreenRect);
        return screenshot;
    }

    private static String getTimeNowAsString(String format) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }

    private static boolean didTheProjectImportFinish(RemoteRobot remoteRobot) {
        try {
            remoteRobot.find(ComponentFixture.class, byXpath("//div[@class='EngravedLabel']"));
        } catch (WaitForConditionTimeoutException e) {
            return true;
        }
        return false;
    }

    private static boolean didAllTheBgTasksFinish(RemoteRobot remoteRobot) {
        for (int i = 0; i < 5; i++) {
            final IdeStatusBar ideStatusBarFixture = remoteRobot.find(IdeStatusBar.class);
            List<RemoteText> inlineProgressPanelContent = ideStatusBarFixture.inlineProgressPanel().findAllText();
            if (!inlineProgressPanelContent.isEmpty()) {
                return false;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public static void invokeCmdUsingTheSearchEverywherePopup(RemoteRobot remoteRobot, String cmdToInvoke) {
        step("Invoke a command using the Search Everywhere popup", () -> {
            Keyboard keyboard = new Keyboard(remoteRobot);
            if (remoteRobot.isMac()) {
                keyboard.hotKey(KeyEvent.VK_META, KeyEvent.VK_O);
            } else {
                keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_N);
            }
            final SearchEverywherePopup searchEverywherePopupFixture = remoteRobot.find(SearchEverywherePopup.class, Duration.ofSeconds(10));
            searchEverywherePopupFixture.popupTab("All").click();
            searchEverywherePopupFixture.searchField().click();
            keyboard.enterText(cmdToInvoke);
            waitFor(Duration.ofSeconds(30), Duration.ofSeconds(1), "The search in the Search Everywhere popup did not finish in 30 seconds.", () -> didTheSearchInTheSearchEverywherePopupFinish(remoteRobot, cmdToInvoke));
            keyboard.hotKey(KeyEvent.VK_ENTER);
        });
    }

    private static boolean didTheSearchInTheSearchEverywherePopupFinish(RemoteRobot remoteRobot, String cmdToInvoke) {
        final SearchEverywherePopup searchEverywherePopupFixture = remoteRobot.find(SearchEverywherePopup.class, Duration.ofSeconds(10));
        String searchResultsString = listOfRemoteTextToString(searchEverywherePopupFixture.searchResultsJBList().findAllText());
        return searchResultsString.toLowerCase().contains(cmdToInvoke.toLowerCase());
    }

    public enum IdeaVersion {
        V2020_1,
        V2020_2,
        UNIDENTIFIED
    }
}