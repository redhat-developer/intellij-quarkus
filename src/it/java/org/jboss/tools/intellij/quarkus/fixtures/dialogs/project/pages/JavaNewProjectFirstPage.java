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
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.DefaultXpath;
import com.intellij.remoterobot.fixtures.FixtureName;
import com.redhat.devtools.intellij.commonuitest.fixtures.dialogs.project.pages.NewProjectFirstPage;
import org.jetbrains.annotations.NotNull;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;

/**
 * New Java project dialog first page fixture
 *
 * @author zcervink@redhat.com
 */
@DefaultXpath(by = "MyDialog type", xpath = "//div[@class='DialogRootPane']")
@FixtureName(name = "New Project Dialog")
public class JavaNewProjectFirstPage extends NewProjectFirstPage {
    public JavaNewProjectFirstPage(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
    }

    /**
     * Get fixture for the frameworks tree
     *
     * @return fixture for the frameworks tree
     */
    public ComponentFixture frameworksTree() {
        return find(ComponentFixture.class, byXpath("//div[@accessiblename='Additional Libraries and Frameworks:' and @class='FrameworksTree']"));
    }
}