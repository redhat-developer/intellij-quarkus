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
package com.redhat.devtools.intellij.qute.psi.core.command;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.commands.LSPCommand;
import com.redhat.devtools.lsp4ij.usages.LSPUsageType;
import com.redhat.devtools.lsp4ij.usages.LSPUsagesManager;
import org.jetbrains.annotations.NotNull;

import java.awt.event.MouseEvent;

public class QuteShowReferencesAction extends QuteAction {

    @Override
    protected void commandPerformed(@NotNull LSPCommand command, @NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        // Call "Find Usages" in popup mode.
        LSPUsagesManager.getInstance(project).findShowUsagesInPopup(locations,
                LSPUsageType.References,
                dataContext,
                (MouseEvent) e.getInputEvent());
    }

}
