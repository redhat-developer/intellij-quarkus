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

package org.jboss.tools.intellij.quarkus.utils;

/**
 * XPath definitions
 *
 * @author zcervink@redhat.com
 */
public class XPathDefinitions {
    public static final String PROJECT_IMPORT_POPUP_MINIMIZE_BUTTON = "//div[@text='Background']";
    public static final String DIALOG_ROOT_PANE = "//div[@class='DialogRootPane']";
    public static final String CUSTOM_ENDPOINT_URL_RADIO_BUTTON = "//div[@accessiblename='Custom:' and @class='JBRadioButton' and @text='Custom:']";
    public static final String CUSTOM_ENDPOINT_URL_TEXT_FIELD = "//div[@class='BorderlessTextField']";
    public static final String SET_BUILD_TOOL_COMBO_BOX = "//div[@accessiblename='Tool:' and @class='ComboBox']";
    public static final String PROJECT_SETTINGS_COMPONENTS = "//div[@class='NamePathComponent']/*";
    public static final String JAVA_VERSION_COMBO_BOX = "//div[@accessiblename='Java version:' and @class='ComboBox']";
}
