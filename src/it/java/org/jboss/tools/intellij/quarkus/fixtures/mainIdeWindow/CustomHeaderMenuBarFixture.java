/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
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
import com.intellij.remoterobot.fixtures.*;
import org.jetbrains.annotations.NotNull;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;

/**
 * Top menu fixture for Windows
 *
 * @author zcervink@redhat.com
 */
@DefaultXpath(by = "MainFrameHeader type", xpath = "//div[@class='CustomHeaderMenuBar']")
@FixtureName(name = "Main Frame Header")
public class CustomHeaderMenuBarFixture extends CommonContainerFixture {
    public CustomHeaderMenuBarFixture(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
    }

    public ComponentFixture mainMenuItem(String label) {
        return find(ComponentFixture.class, byXpath("//div[@accessiblename='" + label + "' and @class='ActionMenu' and @text='" + label + "']"));
    }
}