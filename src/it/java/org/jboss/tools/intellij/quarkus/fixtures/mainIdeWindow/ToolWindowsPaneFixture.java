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
package org.jboss.tools.intellij.quarkus.fixtures.mainIdeWindow;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.data.RemoteComponent;
import com.intellij.remoterobot.fixtures.CommonContainerFixture;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.DefaultXpath;
import com.intellij.remoterobot.fixtures.FixtureName;
import org.jetbrains.annotations.NotNull;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;

/**
 * Global IDE fixture
 * @author zcervink@redhat.com
 */
@DefaultXpath(by = "ToolWindowsPane type", xpath = "//div[@class='ToolWindowsPane']")
@FixtureName(name = "Tool Windows Pane")
public class ToolWindowsPaneFixture extends CommonContainerFixture {
    public ToolWindowsPaneFixture(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
    }

    public ComponentFixture stripeButton(String label) {
        return find(ComponentFixture.class, byXpath("//div[@accessiblename='" + label + "' and @class='StripeButton' and @text='" + label + "']"));
    }

    public ComponentFixture mavenTabTree() {
        return find(ComponentFixture.class, byXpath("//div[@class='SimpleTree']"));
    }

    public ComponentFixture gradleTabTree() {
        return find(ComponentFixture.class, byXpath("//div[@class='ExternalProjectTree']"));
    }

    public ComponentFixture theRunConsole() {
        return find(ComponentFixture.class, byXpath("//div[@accessiblename='Editor' and @class='EditorComponentImpl']"));
    }

    public ComponentFixture theBuildStatusTree() {
        return find(ComponentFixture.class, byXpath("//div[@class='Tree']"));
    }
}