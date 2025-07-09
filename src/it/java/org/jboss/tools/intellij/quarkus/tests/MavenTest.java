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
import com.redhat.devtools.intellij.commonuitest.fixtures.mainidewindow.toolwindowspane.buildtoolpane.MavenBuildToolPane;
import org.jboss.tools.intellij.quarkus.utils.BuildTool;
import org.jboss.tools.intellij.quarkus.utils.EndpointURLType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Basic Maven Quarkus tests
 *
 * @author zcervink@redhat.com
 */
public class MavenTest extends AbstractQuarkusTest {
    private static final String NEW_QUARKUS_MAVEN_PROJECT_NAME = "code-with-quarkus-maven";
    private static final String JAVA_VERSION_FOR_QUARKUS_PROJECT = "17";

    @Test
    public void createBuildQuarkusMavenTest() throws IOException {
        createQuarkusProject(remoteRobot, NEW_QUARKUS_MAVEN_PROJECT_NAME, BuildTool.MAVEN, EndpointURLType.DEFAULT, JAVA_VERSION_FOR_QUARKUS_PROJECT);
        ToolWindowPane toolWindowPane = remoteRobot.find(ToolWindowPane.class, Duration.ofSeconds(10));
        toolWindowPane.openMavenBuildToolPane();
        MavenBuildToolPane mavenBuildToolPane = toolWindowPane.find(MavenBuildToolPane.class, Duration.ofSeconds(10));
        mavenBuildToolPane.buildProject("verify", "code-with-quarkus");
        BuildView buildView = remoteRobot.find(BuildView.class, Duration.ofSeconds(10));
        buildView.waitUntilBuildHasFinished();
        assertTrue(buildView.isBuildSuccessful(), "The build should be successful but is not.");
    }


}