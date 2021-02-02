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
import org.assertj.swing.core.MouseButton;
import org.jboss.tools.intellij.quarkus.fixtures.mainIdeWindow.ProjectToolWindowFixture;
import org.jboss.tools.intellij.quarkus.fixtures.mainIdeWindow.ToolWindowsPaneFixture;

import java.time.Duration;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Static utilities that assist and simplify working with the Project tool window
 *
 * @author zcervink@redhat.com
 */
public class ProjectToolWindowUtils {

    public static boolean isAProjectFilePresent(RemoteRobot remoteRobot, String... path) {
        try {
            navigateThroughTheProjectTree(remoteRobot, ActionToPerform.HIGHLIGHT, path);
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
    }

    private static void navigateThroughTheProjectTree(RemoteRobot remoteRobot, ActionToPerform action, String... pathArray) {
        step("Navigate through the project tree", () -> {
            for (int i = 0; i < pathArray.length; i++) {
                final ProjectToolWindowFixture projectToolWindowFixture = remoteRobot.find(ProjectToolWindowFixture.class);
                String pathItem = pathArray[i];

                // for last item perform different action
                if (i == pathArray.length - 1) {
                    switch (action) {
                        case OPEN:
                            projectToolWindowFixture.projectViewTree().findText(pathItem).doubleClick();
                            break;
                        case OPEN_CONTEXT_MENU:
                            projectToolWindowFixture.projectViewTree().findText(pathItem).click(MouseButton.RIGHT_BUTTON);
                            break;
                        case HIGHLIGHT:
                            projectToolWindowFixture.projectViewTree().findText(pathItem).click();
                            break;
                    }
                } else {
                    projectToolWindowFixture.projectViewTree().findText(pathItem).doubleClick();
                }
            }
        });
    }

    private enum ActionToPerform {
        OPEN, OPEN_CONTEXT_MENU, HIGHLIGHT
    }
}