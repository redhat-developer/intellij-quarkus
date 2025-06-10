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
import com.intellij.remoterobot.fixtures.ComboBoxFixture;
import com.intellij.remoterobot.fixtures.DefaultXpath;
import com.intellij.remoterobot.fixtures.FixtureName;
import com.redhat.devtools.intellij.commonuitest.UITestRunner;
import com.redhat.devtools.intellij.commonuitest.fixtures.dialogs.project.pages.NewProjectFirstPage;
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
public class QuarkusNewProjectFirstPage extends NewProjectFirstPage {
    public QuarkusNewProjectFirstPage(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
    }

    @Override
    public ComboBoxFixture getProjectJdkComboBox() {
        if (UITestRunner.getIdeaVersionInt() >= 20233) {
            return comboBox(byXpath(XPathDefinitions.PROJECT_SDK_COMBOBOX_PROJECT_WIZARD), Duration.ofSeconds(10));
        }
        throw new UnsupportedOperationException("Get Project SDK ComboBox is not supported yet for IDEA version " + UITestRunner.getIdeaVersionInt());
    }
}