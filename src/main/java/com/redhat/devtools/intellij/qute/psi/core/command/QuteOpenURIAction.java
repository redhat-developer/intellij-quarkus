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
import org.eclipse.lsp4j.Command;
import org.jetbrains.annotations.NotNull;

public class QuteOpenURIAction extends QuteAction {

    @Override
    protected void commandPerformed(@NotNull Command command, @NotNull AnActionEvent e) {
        String url = getURL(command.getArguments());
        Project project = e.getProject();
        if (url != null && project != null) {
            LSPIJUtils.openInEditor(url, null, project);
        }
    }

}
