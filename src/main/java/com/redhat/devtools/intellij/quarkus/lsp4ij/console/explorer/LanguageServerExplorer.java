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

import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.treeStructure.Tree;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageServersRegistry;
import com.redhat.devtools.intellij.quarkus.lsp4ij.console.LSPConsoleToolWindowPanel;
import com.redhat.devtools.intellij.quarkus.lsp4ij.console.explorer.actions.RestartServerAction;
import com.redhat.devtools.intellij.quarkus.lsp4ij.console.explorer.actions.StopServerAction;
import com.redhat.devtools.intellij.quarkus.lsp4ij.lifecycle.LanguageServerLifecycleManager;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;

/**
 * Language server explorer which shows language servers and their process.
 *
 * @author Angelo ZERR
 */
public class LanguageServerExplorer extends SimpleToolWindowPanel implements Disposable {

    private final LSPConsoleToolWindowPanel panel;

    private final Tree tree;
    private final LanguageServerExplorerLifecycleListener listener;
    private boolean disposed;

    public LanguageServerExplorer(LSPConsoleToolWindowPanel panel) {
        super(true, false);
        this.panel = panel;
        listener = new LanguageServerExplorerLifecycleListener(this);
        LanguageServerLifecycleManager.getInstance(panel.getProject())
                .addLanguageServerLifecycleListener(listener);
        tree = buildTree();
        this.setContent(tree);
    }

    private void onLanguageServerSelected(LanguageServerTreeNode treeNode) {
        if (isDisposed()) {
            return;
        }
        panel.selectDetail(treeNode);
    }

    private void onLanguageServerProcessSelected(LanguageServerProcessTreeNode processTreeNode) {
        if (isDisposed()) {
            return;
        }
        panel.selectConsole(processTreeNode);
    }

    /**
     * Builds the Language server tree
     *
     * @return Tree object of all language servers
     */
    private Tree buildTree() {

        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Language servers");

        Tree tree = new Tree(top);
        tree.setRootVisible(false);

        // Fill tree will all language server definitions
        LanguageServersRegistry.getInstance().getAllDefinitions()
                .forEach(serverDefinition -> top.add(new LanguageServerTreeNode(serverDefinition)));

        tree.setCellRenderer(new LanguageServerTreeRenderer());

        tree.addTreeSelectionListener(l -> {
            if(isDisposed()) {
                return;
            }
            TreePath selectionPath = tree.getSelectionPath();
            Object selectedItem = selectionPath != null ? selectionPath.getLastPathComponent() : null;
            if (selectedItem instanceof LanguageServerTreeNode) {
                LanguageServerTreeNode node = (LanguageServerTreeNode) selectedItem;
                onLanguageServerSelected(node);
            } else if (selectedItem instanceof LanguageServerProcessTreeNode) {
                LanguageServerProcessTreeNode node = (LanguageServerProcessTreeNode) selectedItem;
                onLanguageServerProcessSelected(node);
            }
        });

        DataProvider newDataProvider = new LanguageServerExplorerTreeDataProvider(tree);
        DataManager.registerDataProvider(tree, newDataProvider);

        tree.addMouseListener(new PopupHandler() {
            @Override
            public void invokePopup(Component comp, int x, int y) {
                final TreePath path = tree.getSelectionPath();
                if (path != null) {
                    DefaultActionGroup group = null;
                    Object node = path.getLastPathComponent();
                    if (node instanceof LanguageServerProcessTreeNode) {
                        LanguageServerProcessTreeNode processTreeNode = (LanguageServerProcessTreeNode) node;
                        switch (processTreeNode.getServerStatus()) {
                            case started:
                                // Stop language server action
                                group = new DefaultActionGroup();
                                AnAction stopServerAction = ActionManager.getInstance().getAction(StopServerAction.ACTION_ID);
                                group.add(stopServerAction);
                                break;
                            case stopped:
                                // Restart language server action
                                group = new DefaultActionGroup();
                                AnAction restartServerAction = ActionManager.getInstance().getAction(RestartServerAction.ACTION_ID);
                                group.add(restartServerAction);
                                break;
                        }
                    }

                    if (group != null) {
                        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, group);
                        menu.getComponent().show(comp, x, y);
                    }
                }
            }
        });
        tree.putClientProperty(AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED, true);

        ((DefaultTreeModel) tree.getModel()).reload(top);
        return tree;
    }

    public Tree getTree() {
        return tree;
    }

    @Override
    public void dispose() {
        this.disposed = true;
        LanguageServerLifecycleManager.getInstance(panel.getProject())
                .removeLanguageServerLifecycleListener(listener);
    }

    public boolean isDisposed() {
        return disposed || getProject().isDisposed() || listener.isDisposed();
    }

    public void showMessage(LanguageServerProcessTreeNode processTreeNode, String message) {
        panel.showMessage(processTreeNode, message);
    }

    public DefaultTreeModel getTreeModel() {
        return (DefaultTreeModel) tree.getModel();
    }

    public void selectAndExpand(DefaultMutableTreeNode treeNode) {
        var treePath = new TreePath(treeNode.getPath());
        tree.setSelectionPath(treePath);
        if (!tree.isExpanded(treePath)) {
            tree.expandPath(treePath);
        }
    }

    public Project getProject() {
        return panel.getProject();
    }
}
