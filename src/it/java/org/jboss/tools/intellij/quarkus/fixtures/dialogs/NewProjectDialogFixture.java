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
package org.jboss.tools.intellij.quarkus.fixtures.dialogs;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.data.RemoteComponent;
import com.intellij.remoterobot.fixtures.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;

/**
 * New Project dialog fixture
 *
 * @author zcervink@redhat.com
 */
@DefaultXpath(by = "MyDialog type", xpath = "//div[@accessiblename='New Project' and @class='MyDialog']")
@FixtureName(name = "New Project Dialog")
public class NewProjectDialogFixture extends CommonContainerFixture {
    public NewProjectDialogFixture(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
    }

    public ComponentFixture toolComboBox() {
        return find(ComponentFixture.class, byXpath("//div[@accessiblename='Tool:' and @class='ComboBox']"));
    }

    public JTextFieldFixture projectNameJTextField() {
        return find(JTextFieldFixture.class, byXpath("//div[@accessiblename='Project name:' and @class='JTextField']"));
    }

    public JTextFieldFixture projectLocationJTextField() {
        return find(JTextFieldFixture.class, byXpath("//div[@accessiblename='Project location:' and @class='JTextField']"));
    }

    public ComponentFixture extensionCategoriesJBList() {
        List<ComponentFixture> allJBLists = findAll(ComponentFixture.class, byXpath("//div[@class='JBList']"));
        return allJBLists.get(0);
    }

    public ComponentFixture projectTypeJBList() {
        return find(ComponentFixture.class, byXpath("JBList", "//div[@class='JBList']"));
    }

    public ComponentFixture extensionsTable() {
        return find(ComponentFixture.class, byXpath("//div[@class='ExtensionsTable']"));
    }

    public ComponentFixture theFrameworksTree() {
        return find(ComponentFixture.class, byXpath("//div[@accessiblename='Additional Libraries and Frameworks:' and @class='FrameworksTree']"));
    }

    public JTextFieldFixture projectNameField() {
        return find(JTextFieldFixture.class, byXpath("//div[@accessiblename='Project name:' and @class='JTextField']"));
    }
}