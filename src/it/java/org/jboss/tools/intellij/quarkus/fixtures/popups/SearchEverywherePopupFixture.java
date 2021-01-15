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
package org.jboss.tools.intellij.quarkus.fixtures.popups;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.data.RemoteComponent;
import com.intellij.remoterobot.fixtures.CommonContainerFixture;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.DefaultXpath;
import com.intellij.remoterobot.fixtures.FixtureName;
import org.jetbrains.annotations.NotNull;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;

/**
 * Search everywhere popup fixture
 *
 * @author zcervink@redhat.com
 */
@DefaultXpath(by = "SearchEverywhereUI type", xpath = "//div[@accessiblename='Search everywhere' and @class='SearchEverywhereUI']")
@FixtureName(name = "Search Everywhere Popup")
public class SearchEverywherePopupFixture extends CommonContainerFixture {
    public SearchEverywherePopupFixture(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
        super(remoteRobot, remoteComponent);
    }

    public ComponentFixture popupTab(String label) {
        return find(ComponentFixture.class, byXpath("//div[@accessiblename='" + label + "' and @class='SETab' and @text='" + label + "']"));
    }

    public ComponentFixture searchField() {
        return find(ComponentFixture.class, byXpath("//div[@class='SearchField']"));
    }

    public ComponentFixture searchResultsJBList() {
        return find(ComponentFixture.class, byXpath("//div[@class='JBList']"));
    }
}