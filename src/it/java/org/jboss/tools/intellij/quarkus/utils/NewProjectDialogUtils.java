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
import org.jboss.tools.intellij.quarkus.fixtures.dialogs.NewProjectDialogFixture;

/**
 * Static utilities that assist and simplify the creation of new project
 *
 * @author zcervink@redhat.com
 */
public class NewProjectDialogUtils {

    public static void selectNewProjectType(RemoteRobot remoteRobot, String projectType) {
        NewProjectDialogFixture newProjectDialogFixture = remoteRobot.find(NewProjectDialogFixture.class);
        newProjectDialogFixture.projectTypeJBList().findText(projectType).click();
    }
}