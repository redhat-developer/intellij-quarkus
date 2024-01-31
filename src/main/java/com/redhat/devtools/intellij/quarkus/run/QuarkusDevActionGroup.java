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

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuarkusDevActionGroup extends ActionGroup {
    private static final AnAction OPEN_DEV_UI_ACTION = new QuarkusOpenDevUIAction();
    private static final AnAction OPEN_APP_ACTION = new QuarkusOpenAppInBrowserAction();

    public QuarkusDevActionGroup() {
        super("Quarkus", "", IconLoader.getIcon("/quarkus_icon_rgb_16px_default.png",
                QuarkusDevActionGroup.class));
        setPopup(true);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Object context = QuarkusRunContext.getContext(e);
        e.getPresentation().setEnabledAndVisible(context != null);
    }

    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        return new AnAction[] { OPEN_DEV_UI_ACTION, OPEN_APP_ACTION};
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
