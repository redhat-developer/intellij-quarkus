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
import com.intellij.remoterobot.fixtures.ContainerFixture;
import com.intellij.remoterobot.utils.Keyboard;
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException;
import org.apache.commons.io.FileUtils;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.IdeFatalErrorsDialogFixture;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.TipOfTheDayDialogFixture;
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.WelcomeFrameDialogFixture;
import org.jboss.tools.intellij.quarkus.fixtures.mainIdeWindow.CustomHeaderMenuBarFixture;
import org.jboss.tools.intellij.quarkus.fixtures.mainIdeWindow.IdeStatusBarFixture;
import org.jboss.tools.intellij.quarkus.fixtures.mainIdeWindow.LinuxIdeMenuBarFixture;
import org.jboss.tools.intellij.quarkus.fixtures.popups.SearchEverywherePopupFixture;

import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static org.jboss.tools.intellij.quarkus.utils.HelperUtils.listOfRemoteTextToString;

/**
 * Static utilities that assist and simplify manipulation with the IDE and with the project
 *
 * @author zcervink@redhat.com
 */
public class GlobalUtils {

    public static String projectPath = "";

    public static RemoteRobot getRemoteRobotConnection(int port) throws InterruptedException {
        RemoteRobot remoteRobot = new RemoteRobot("http://127.0.0.1:" + port);
        for (int i = 0; i < 60; i++) {
            try {
                remoteRobot.find(WelcomeFrameDialogFixture.class);
            } catch (Exception ex) {
                Thread.sleep(1000);
            }
        }
        return remoteRobot;
    }

    public static void closeTheTipOfTheDayDialogIfItAppears(RemoteRobot remoteRobot) {
        step("Close the 'Tip of the Day' Dialog", () -> {
            try {
                final TipOfTheDayDialogFixture tipOfTheDayDialogFixture = remoteRobot.find(TipOfTheDayDialogFixture.class, Duration.ofSeconds(20));
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
                CustomHeaderMenuBarFixture chmb = remoteRobot.find(CustomHeaderMenuBarFixture.class, Duration.ofSeconds(10));
                chmb.mainMenuItem("File").click();
                List<ContainerFixture> allHeavyWeightWindows = remoteRobot.findAll(ContainerFixture.class, byXpath("//div[@class='HeavyWeightWindow']"));
                ContainerFixture lastHeavyWeightWindow = allHeavyWeightWindows.get(allHeavyWeightWindows.size() - 1);
                ComponentFixture closeProjectButtonFixture = lastHeavyWeightWindow.find(ComponentFixture.class, byXpath("//div[@accessiblename='Close Project' and @text='Close Project']"));
                closeProjectButtonFixture.click();
            } else {
                LinuxIdeMenuBarFixture limb = remoteRobot.find(LinuxIdeMenuBarFixture.class, Duration.ofSeconds(10));
                limb.mainMenuItem("File").click();
                List<ContainerFixture> allHeavyWeightWindows = remoteRobot.findAll(ContainerFixture.class, byXpath("//div[@class='HeavyWeightWindow']"));
                ContainerFixture lastHeavyWeightWindow = allHeavyWeightWindows.get(allHeavyWeightWindows.size() - 1);
                ComponentFixture closeProjectButtonFixture = lastHeavyWeightWindow.find(ComponentFixture.class, byXpath("//div[@accessiblename='Close Project' and @text='Close Project']"));
                closeProjectButtonFixture.click();
            }

            final WelcomeFrameDialogFixture welcomeFrameDialogFixture = remoteRobot.find(WelcomeFrameDialogFixture.class, Duration.ofSeconds(10));
            welcomeFrameDialogFixture.runJs("const horizontal_offset = component.getWidth()/2;\n" +
                    "robot.click(component, new Point(horizontal_offset, 10), MouseButton.LEFT_BUTTON, 1);");
        });
    }

    public static void clearTheWorkspace(RemoteRobot remoteRobot) {
        step("Delete all the projects in the workspace", () -> {
            // delete all the projects' links from the 'Welcome to IntelliJ IDEA' dialog
            int numberOfLinks = getNumberOfProjectLinks(remoteRobot);
            for (int i = 0; i < numberOfLinks; i++) {
                final WelcomeFrameDialogFixture welcomeFrameDialogFixture = remoteRobot.find(WelcomeFrameDialogFixture.class, Duration.ofSeconds(10));
                ComponentFixture cf = welcomeFrameDialogFixture.find(ComponentFixture.class, byXpath("//div[@accessiblename='Recent Projects' and @class='MyList']"));
                cf.runJs("const horizontal_offset = component.getWidth()-22;\n" +
                        "robot.click(component, new Point(horizontal_offset, 22), MouseButton.LEFT_BUTTON, 1);");
            }

            // delete all the files and folders in the IdeaProjects folder
            try {
                String pathToDirToMakeEmpty = System.getProperty("user.home") + File.separator + "IdeaProjects";
                boolean doesTheProjectDirExists = Files.exists(Paths.get(pathToDirToMakeEmpty));
                if (doesTheProjectDirExists) {
                    FileUtils.cleanDirectory(new File(pathToDirToMakeEmpty));
                } else {
                    Files.createDirectory(Paths.get(pathToDirToMakeEmpty));
                }
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
            final IdeStatusBarFixture ideStatusBarFixture = remoteRobot.find(IdeStatusBarFixture.class);

            try {
                ideStatusBarFixture.bgTasksIcon();
                return false;
            } catch (WaitForConditionTimeoutException timeoutException) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    private static int getNumberOfProjectLinks(RemoteRobot remoteRobot) {
        final WelcomeFrameDialogFixture welcomeFrameDialogFixture = remoteRobot.find(WelcomeFrameDialogFixture.class, Duration.ofSeconds(10));
        try {
            ComponentFixture cf = welcomeFrameDialogFixture.find(ComponentFixture.class, byXpath("//div[@accessiblename='Recent Projects' and @class='MyList']"));
            int numberOfProjectsLinks = cf.findAllText().size() / 2;    // 2 items per 1 project link (project path and project name)
            return numberOfProjectsLinks;
        } catch (WaitForConditionTimeoutException e) {
            // the list with accessible name 'Recent Projects' is not available -> 0 links in the 'Welcome to IntelliJ IDEA' dialog
            return 0;
        }
    }

    public static void waitUntilIntelliJStarts(int port) {
        waitFor(Duration.ofSeconds(300), Duration.ofSeconds(3), "The IntelliJ Idea did not start in 5 minutes.", () -> isIntelliJUIVisible(port));
    }

    private static boolean isIntelliJUIVisible(int port) {
        return isHostOnIpAndPortAccessible("127.0.0.1", port);
    }

    private static boolean isHostOnIpAndPortAccessible(String ip, int port) {
        SocketAddress sockaddr = new InetSocketAddress(ip, port);
        Socket socket = new Socket();

        try {
            socket.connect(sockaddr, 10000);
        } catch (IOException IOException) {
            return false;
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
            final SearchEverywherePopupFixture searchEverywherePopupFixture = remoteRobot.find(SearchEverywherePopupFixture.class, Duration.ofSeconds(10));
            searchEverywherePopupFixture.popupTab("All").click();
            searchEverywherePopupFixture.searchField().click();
            keyboard.enterText(cmdToInvoke);
            waitFor(Duration.ofSeconds(30), Duration.ofSeconds(1), "The search in the Search Everywhere popup did not finish in 30 seconds.", () -> didTheSearchInTheSearchEverywherePopupFinish(remoteRobot, cmdToInvoke));
            keyboard.hotKey(KeyEvent.VK_ENTER);
        });
    }

    private static boolean didTheSearchInTheSearchEverywherePopupFinish(RemoteRobot remoteRobot, String cmdToInvoke) {
        final SearchEverywherePopupFixture searchEverywherePopupFixture = remoteRobot.find(SearchEverywherePopupFixture.class, Duration.ofSeconds(10));
        String searchResultsString = listOfRemoteTextToString(searchEverywherePopupFixture.searchResultsJBList().findAllText());
        return searchResultsString.toLowerCase().contains(cmdToInvoke.toLowerCase());
    }
}