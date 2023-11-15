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
import com.redhat.devtools.intellij.quarkus.lang.QuarkusIcons;
import org.jetbrains.annotations.NotNull;

public class QuarkusOpenDevUIAction extends QuarkusDevAction {
    public static final String ACTION_ID = "com.redhat.devtools.intellij.quarkus.run.QuarkusOpenDevUIAction";

    public QuarkusOpenDevUIAction() {
        super("Open DevUI","Launches the DevUI in a browser", QuarkusIcons.Quarkus);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TelemetryService.instance().action(TelemetryService.UI_PREFIX + "openDevUI").send();
        BrowserUtil.browse(QuarkusRunContext.getContext(e).getDevUIURL(), PlatformDataKeys.PROJECT.getData(e.getDataContext()));
    }
}
