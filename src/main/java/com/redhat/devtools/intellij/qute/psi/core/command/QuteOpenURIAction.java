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
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.net.MalformedURLException;
import java.net.URL;

public class QuteOpenURIAction extends QuteAction {
    private static System.Logger LOGGER = System.getLogger(QuteOpenURIAction.class.getName());

    @Override
    public void actionPerformed(AnActionEvent e) {
        try {
            String url = getURL(e);
            if (url != null) {
                VirtualFile f = VfsUtil.findFileByURL(new URL(url));
                if (f != null) {
                    FileEditorManager.getInstance(e.getProject()).openFile(f, true);
                }
            }
        } catch (MalformedURLException ex) {
            LOGGER.log(System.Logger.Level.WARNING, ex.getLocalizedMessage(), ex);
        }
    }
}
