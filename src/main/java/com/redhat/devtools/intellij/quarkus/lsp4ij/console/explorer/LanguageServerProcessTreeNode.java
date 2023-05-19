/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.quarkus.lsp4ij.console.explorer;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AnimatedIcon;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageServerWrapper;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

/**
 * Language server process node.
 */
public class LanguageServerProcessTreeNode extends DefaultMutableTreeNode {

    private static final Icon RUNNING_ICON = new AnimatedIcon.Default();

    private final LanguageServerWrapper languageServer;

    private final DefaultTreeModel treeModel;

    private ServerStatus serverStatus;

    private long startTime = -1;

    public LanguageServerProcessTreeNode(LanguageServerWrapper languageServer, DefaultTreeModel treeModel) {
        this.languageServer = languageServer;
        this.treeModel = treeModel;
    }

    public void setServerStatus(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
        switch(serverStatus) {
            case startingProcess:
                startTime = System.currentTimeMillis();
                super.setUserObject("starting process...");
                break;
            case startedProcess:
                super.setUserObject("process started");
                break;
            case starting:
                super.setUserObject("starting...");
                break;
            case started:
                startTime = -1;
                Long pid = languageServer.getCurrentProcessId();
                StringBuilder name = new StringBuilder("started");
                if (pid != null) {
                    name.append(" pid:");
                    name.append(pid);
                }
                super.setUserObject(name.toString());
                break;
            case stopping:
                startTime = System.currentTimeMillis();
                super.setUserObject("stopping...");
                break;
            case stopped:
                startTime = -1;
                super.setUserObject("stopped");
                break;
        }
        treeModel.reload(this);
    }

    public LanguageServerWrapper getLanguageServer() {
        return languageServer;
    }

    public ServerStatus getServerStatus() {
        return serverStatus;
    }

    public Icon getIcon() {
        switch(serverStatus) {
            case started:
                return AllIcons.Debugger.ThreadRunning;
            case stopped:
                return AllIcons.Debugger.ThreadSuspended;
            default:
                return RUNNING_ICON;
        }
    }

    public String getDisplayName() {
        return (String) super.getUserObject();
    }

    public String getElapsedTime() {
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        return StringUtil.formatDuration(duration, "\u2009");
    }
}
