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
package org.jboss.tools.intellij.quarkus;

import com.intellij.remoterobot.RemoteRobot;
import org.jboss.tools.intellij.quarkus.utils.BuildUtils;
import org.jboss.tools.intellij.quarkus.utils.GlobalUtils;
import org.jboss.tools.intellij.quarkus.utils.ProjectToolWindowUtils;
import org.jboss.tools.intellij.quarkus.utils.QuarkusUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Basic JUnit UI tests
 *
 * @author zcervink@redhat.com
 */
public class BasicTests {

    private static RemoteRobot robot;

    @BeforeAll
    public static void connect() throws InterruptedException {
        GlobalUtils.waitUntilIntelliJStarts(8082);
        robot = GlobalUtils.getRemoteRobotConnection(8082);
        GlobalUtils.clearTheWorkspace(robot);
    }

    @AfterEach
    public void finishTheTestRun() {
        GlobalUtils.checkForExceptions(robot);
        GlobalUtils.closeTheProject(robot);
        GlobalUtils.clearTheWorkspace(robot);
    }

    @Test
    public void createAQuarkusProjectAndBuildItUsingMaven() {
        step("Create a Quarkus project and build it using maven", () -> {
            QuarkusUtils.createNewQuarkusProject(robot, BuildUtils.ToolToBuildTheProject.MAVEN);
            GlobalUtils.waitUntilTheProjectImportIsComplete(robot);
            GlobalUtils.closeTheTipOfTheDayDialogIfItAppears(robot);
            GlobalUtils.maximizeTheIdeWindow(robot);
            GlobalUtils.waitUntilAllTheBgTasksFinish(robot);
            BuildUtils.buildTheProject(robot, BuildUtils.ToolToBuildTheProject.MAVEN);
            GlobalUtils.waitUntilAllTheBgTasksFinish(robot);
            BuildUtils.testIfBuildIsSuccessful(robot);
        });
    }

    @Test
    public void testIfTheQuarkusRuntimeIsDownloaded() {
        step("Test whether the Quarkus runtime can be downloaded", () -> {
            String projectName = "java-project-with-quarkus-runtime";
            String runtimeJarName = QuarkusUtils.createNewJavaProjectWithQuarkusFramework(robot, projectName);
            GlobalUtils.waitUntilTheProjectImportIsComplete(robot);
            GlobalUtils.closeTheTipOfTheDayDialogIfItAppears(robot);
            GlobalUtils.maximizeTheIdeWindow(robot);
            GlobalUtils.waitUntilAllTheBgTasksFinish(robot);
            assertTrue(ProjectToolWindowUtils.isAProjectFilePresent(robot, projectName, "lib", runtimeJarName), "The runtime has not been downloaded.");
        });
    }
}