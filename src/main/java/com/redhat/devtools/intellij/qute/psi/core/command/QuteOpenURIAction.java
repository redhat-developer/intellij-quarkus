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

public class QuteOpenURIAction extends QuteAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        String url = getURL(e);
        Project project = e.getProject();
        if (url != null && project != null) {
            LSPIJUtils.openInEditor(url, null, project);
        }
    }
}
