/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
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
import com.intellij.remoterobot.fixtures.CommonContainerFixture;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.DefaultXpath;
import com.intellij.remoterobot.fixtures.FixtureName;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;

/**
 * New project dialog fixture
 *
 * @author zcervink@redhat.com
 */
@DefaultXpath(by = "MyDialog type", xpath = "//div[@class='DialogRootPane']")
@FixtureName(name = "New Project Dialog")
public class QuarkusNewProjectThirdPage extends CommonContainerFixture {
    public QuarkusNewProjectThirdPage(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
    }

    /**
     * Get fixture for the extension categories list
     *
     * @return fixture for the extension categories list
     */
    public ComponentFixture extensionCategories() {
        List<ComponentFixture> allJBLists = findAll(ComponentFixture.class, byXpath("//div[@class='JBList']"));
        return allJBLists.get(0);
    }

    /**
     * Get fixture for the extension table
     *
     * @return fixture for the extension table
     */
    public ComponentFixture extensionsTable() {
        return find(ComponentFixture.class, byXpath("//div[@class='ExtensionsTable']"));
    }
}