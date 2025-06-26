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

import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.toolwindowspane.BuildView;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.toolwindowspane.ToolWindowPane;
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.toolwindowspane.buildtoolpane.GradleBuildToolPane;
import com.redhat.devtools.intellij.commonuitest.utils.screenshot.ScreenshotUtils;
import org.jboss.tools.intellij.quarkus.utils.BuildTool;
import org.jboss.tools.intellij.quarkus.utils.EndpointURLType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Basic Gradle Quarkus tests
 *
 */
public class GradleTest extends AbstractQuarkusTest {
    private static final String NEW_QUARKUS_GRADLE_PROJECT_NAME = "code-with-quarkus-gradle";
    private static final String JAVA_VERSION = "17";

    @Test
    public void createBuildQuarkusGradleTest() throws IOException {
        createQuarkusProject(remoteRobot, NEW_QUARKUS_GRADLE_PROJECT_NAME, BuildTool.GRADLE, EndpointURLType.DEFAULT, JAVA_VERSION);
        ToolWindowPane toolWindowPane = remoteRobot.find(ToolWindowPane.class, Duration.ofSeconds(10));
        toolWindowPane.openGradleBuildToolPane();
        GradleBuildToolPane gradleBuildToolPane = toolWindowPane.find(GradleBuildToolPane.class, Duration.ofSeconds(10));
        gradleBuildToolPane.buildProject();
        boolean isBuildSuccessful = toolWindowPane.find(BuildView.class, Duration.ofSeconds(10)).isBuildSuccessful();
        assertTrue(isBuildSuccessful, "The build should be successful but is not.");
    }

}