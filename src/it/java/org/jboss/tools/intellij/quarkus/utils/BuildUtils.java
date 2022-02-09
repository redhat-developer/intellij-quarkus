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
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException;
import org.jboss.tools.intellij.quarkus.fixtures.mainIdeWindow.ToolWindowsPaneFixture;
import java.time.Duration;

import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Static utilities that assist and simplify the project build process and other related activities
 *
 * @author zcervink@redhat.com
 */
public class BuildUtils {

    private static String lastBuildStatusTreeText;

    public static void buildTheProject(RemoteRobot remoteRobot, ToolToBuildTheProject toolToBuildTheProject) {
        step("Build the project", () -> {
            switch (toolToBuildTheProject) {
                case MAVEN:
                    step("Open the Maven tab and build the project", () -> {
                        final ToolWindowsPaneFixture toolWindowsPaneFixture = remoteRobot.find(ToolWindowsPaneFixture.class);
                        waitFor(Duration.ofSeconds(10), Duration.ofSeconds(1), "The 'Maven' stripe button is not available.", () -> isStripeButtonAvailable(toolWindowsPaneFixture, "Maven"));
                        toolWindowsPaneFixture.stripeButton("Maven").click();
                        toolWindowsPaneFixture.mavenTabTree().findText("code-with-quarkus").doubleClick();
                        toolWindowsPaneFixture.mavenTabTree().findText("Lifecycle").doubleClick();
                        toolWindowsPaneFixture.mavenTabTree().findText("install").doubleClick();
                    });
                    break;
                case GRADLE:
                    step("Open the Gradle tab and build the project", () -> {
                        final ToolWindowsPaneFixture toolWindowsPaneFixture = remoteRobot.find(ToolWindowsPaneFixture.class);
                        toolWindowsPaneFixture.stripeButton("Gradle").click();
                        //tree seems to be load asynchronously let's sync
                        waitFor(() -> toolWindowsPaneFixture.gradleTabTree().hasText("Tasks"));
                        toolWindowsPaneFixture.gradleTabTree().findText("Tasks").doubleClick();
                        toolWindowsPaneFixture.gradleTabTree().findText("build").doubleClick();
                        toolWindowsPaneFixture.gradleTabTree().findAllText("build").get(1).doubleClick();
                    });
                    break;
            }

            waitUntilTheBuildHasFinished(remoteRobot, 300);
        });
    }

    public static void testIfBuildIsSuccessful(RemoteRobot remoteRobot) {
        step("Test if the build is successful", () -> {
            final ToolWindowsPaneFixture toolWindowsPaneFixture = remoteRobot.find(ToolWindowsPaneFixture.class);
            String theRunConsoleOutput = HelperUtils.listOfRemoteTextToString(toolWindowsPaneFixture.theRunConsole().findAllText());
            assertTrue(theRunConsoleOutput.contains("BUILD SUCCESS"), "The build should be successful, but is not.");
        });
    }

    private static void waitUntilTheBuildHasFinished(RemoteRobot remoteRobot, int timeoutInSeconds) {
        step("Wait until the build has finished", () -> {
            waitFor(Duration.ofSeconds(300), Duration.ofSeconds(3), "The build did not finish in 5 minutes.", () -> didTheBuildStatusTreeTextStopChanging(remoteRobot));
        });
    }

    private static boolean didTheBuildStatusTreeTextStopChanging(RemoteRobot remoteRobot) {
        String updatedBuildStatusTreeText = getBuildStatusTreeText(remoteRobot);

        if (lastBuildStatusTreeText != null && lastBuildStatusTreeText.equals(updatedBuildStatusTreeText)) {
            lastBuildStatusTreeText = null;
            return true;
        } else {
            lastBuildStatusTreeText = updatedBuildStatusTreeText;
            return false;
        }
    }

    private static String getBuildStatusTreeText(RemoteRobot remoteRobot) {
        final ToolWindowsPaneFixture toolWindowsPaneFixture = remoteRobot.find(ToolWindowsPaneFixture.class);
        String buildStatusTreeText = HelperUtils.listOfRemoteTextToString(toolWindowsPaneFixture.theBuildStatusTree().findAllText());
        return buildStatusTreeText;
    }

    private static boolean isStripeButtonAvailable(ToolWindowsPaneFixture toolWindowsPaneFixture, String label) {
        try {
            toolWindowsPaneFixture.stripeButton(label);
        } catch (WaitForConditionTimeoutException e) {
            return false;
        }
        return true;
    }

    public enum ToolToBuildTheProject {
        MAVEN,
        GRADLE
    }
}