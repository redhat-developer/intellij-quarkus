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
package org.jboss.tools.intellij.quarkus.fixtures.dialogs.project.pages;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.data.RemoteComponent;
import com.intellij.remoterobot.fixtures.ComboBoxFixture;
import com.intellij.remoterobot.fixtures.CommonContainerFixture;
import com.intellij.remoterobot.fixtures.DefaultXpath;
import com.intellij.remoterobot.fixtures.FixtureName;
import org.jboss.tools.intellij.quarkus.utils.BuildTool;
import org.jboss.tools.intellij.quarkus.utils.XPathDefinitions;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;

/**
 * New project dialog fixture
 *
 * @author zcervink@redhat.com
 */
@DefaultXpath(by = "MyDialog type", xpath = XPathDefinitions.DIALOG_ROOT_PANE)
@FixtureName(name = "New Project Dialog")
public class QuarkusNewProjectSecondPage extends CommonContainerFixture {
    public QuarkusNewProjectSecondPage(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
    }

    /**
     * Set the build tool
     */
    public void setBuildTool(BuildTool buildTool) {
        ComboBoxFixture comboBoxFixture = comboBox(byXpath(XPathDefinitions.SET_BUILD_TOOL_COMBO_BOX), Duration.ofSeconds(10));
        if (!comboBoxFixture.selectedText().contains(buildTool.toString())) {
            comboBoxFixture.click(); // extra click needed due to an issue - https://github.com/JetBrains/intellij-ui-test-robot/issues/112
            comboBoxFixture.selectItem(buildTool.toString());
        }
    }

    /**
     * Set java version
     */
    public void setJavaVersion(String javaVersion){
        ComboBoxFixture comboBoxFixture = comboBox(byXpath(XPathDefinitions.JAVA_VERSION_COMBO_BOX), Duration.ofSeconds(10));
        if (!comboBoxFixture.selectedText().contains(javaVersion)) {
            comboBoxFixture.click(); // extra click needed due to an issue - https://github.com/JetBrains/intellij-ui-test-robot/issues/112
            comboBoxFixture.selectItem(javaVersion);
        }
    }
}