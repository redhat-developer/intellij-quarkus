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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.treeStructure.Tree;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageServersRegistry;
import com.redhat.devtools.intellij.quarkus.lsp4ij.console.LSPConsoleToolWindowPanel;
import com.redhat.devtools.intellij.quarkus.lsp4ij.lifecycle.LanguageServerLifecycleListener;
import com.redhat.devtools.intellij.quarkus.lsp4ij.lifecycle.LanguageServerLifecycleManager;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

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

    private void onLanguageServerSelected(LanguageServerProcessTreeNode processTreeNode) {
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
            TreePath selectionPath = tree.getSelectionPath();
            Object selectedItem = selectionPath != null ? selectionPath.getLastPathComponent() : null;
            if (selectedItem instanceof LanguageServerProcessTreeNode) {
                LanguageServerProcessTreeNode node = (LanguageServerProcessTreeNode) selectedItem;
                onLanguageServerSelected(node);
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
