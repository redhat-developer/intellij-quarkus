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
import com.redhat.devtools.intellij.commonuitest.utils.build.BuildUtils;
import org.jboss.tools.intellij.quarkus.utils.BuildTool;
import org.jboss.tools.intellij.quarkus.utils.EndpointURLType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Basic Gradle Quarkus tests
 */
public class GradleTest extends AbstractQuarkusTest {
    private static final String NEW_QUARKUS_GRADLE_PROJECT_NAME = "code-with-quarkus-gradle";
    private static final String NEW_QUARKUS_GRADLE_KOTLIN_PROJECT_NAME = "code-with-quarkus-gradle-kotlin";
    private static final String JAVA_VERSION = "17";

    @Test
    public void createBuildQuarkusGradleTest() throws IOException {
        runTest(NEW_QUARKUS_GRADLE_PROJECT_NAME, BuildTool.GRADLE);
    }

    @Test
    public void createBuildQuarkusGradleKotlinTest() throws IOException {
        runTest(NEW_QUARKUS_GRADLE_KOTLIN_PROJECT_NAME, BuildTool.GRADLE_WITH_KOTLIN);
    }

    private void runTest(String projectName, BuildTool buildTool) throws IOException {
        createQuarkusProject(remoteRobot, projectName, buildTool, EndpointURLType.DEFAULT, JAVA_VERSION);
        BuildUtils.buildMavenProjectAndWaitForFinish(remoteRobot, projectName, "verify");
        BuildView buildView = remoteRobot.find(BuildView.class, Duration.ofSeconds(10));
        assertTrue(buildView.isBuildSuccessful(), "The build should be successful but is not.");
    }

}