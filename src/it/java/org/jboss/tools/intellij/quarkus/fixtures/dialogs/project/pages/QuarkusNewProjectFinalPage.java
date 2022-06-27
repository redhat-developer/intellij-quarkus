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
package org.jboss.tools.intellij.quarkus.fixtures.dialogs.project.pages;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.data.RemoteComponent;
import com.intellij.remoterobot.fixtures.DefaultXpath;
import com.intellij.remoterobot.fixtures.FixtureName;
import com.intellij.remoterobot.fixtures.JTextFieldFixture;
import com.redhat.devtools.intellij.commonuitest.UITestRunner;
import com.redhat.devtools.intellij.commonuitest.fixtures.dialogs.project.pages.AbstractNewProjectFinalPage;
import org.jboss.tools.intellij.quarkus.utils.XPathDefinitions;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;

/**
 * New project dialog fixture
 *
 * @author zcervink@redhat.com
 */
@DefaultXpath(by = "MyDialog type", xpath = XPathDefinitions.DIALOG_ROOT_PANE)
@FixtureName(name = "New Project Dialog")
public class QuarkusNewProjectFinalPage extends AbstractNewProjectFinalPage {
    public QuarkusNewProjectFinalPage(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
    }

    @Override
    public void setProjectName(String projectName) {
        if (UITestRunner.getIdeaVersionInt() >= 20221) {
            List<JTextFieldFixture> projectSettingsComponents = findAll(JTextFieldFixture.class, byXpath(XPathDefinitions.PROJECT_SETTINGS_COMPONENTS));
            JTextFieldFixture projectNameTextField = projectSettingsComponents.get(1);
            projectNameTextField.setText(projectName);
        } else {
            super.setProjectName(projectName);
        }
    }
}