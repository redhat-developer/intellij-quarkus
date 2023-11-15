/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.run;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.util.IconLoader;
import com.redhat.devtools.intellij.quarkus.TelemetryService;
import org.jetbrains.annotations.NotNull;

public class QuarkusOpenAppInBrowserAction extends QuarkusDevAction {

    public static final String ACTION_ID = "com.redhat.devtools.intellij.quarkus.run.QuarkusOpenAppInBrowserAction";

    public QuarkusOpenAppInBrowserAction() {
        super("Open application","Launches the application in a browser", IconLoader.getIcon("/quarkus_icon_rgb_16px_default.png", QuarkusOpenAppInBrowserAction.class));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TelemetryService.instance().action(TelemetryService.UI_PREFIX + "openApplication").send();
        BrowserUtil.browse(QuarkusRunContext.getContext(e).getApplicationURL(), PlatformDataKeys.PROJECT.getData(e.getDataContext()));
    }
}
