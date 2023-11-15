package com.redhat.devtools.intellij.quarkus.explorer;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.treeStructure.Tree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public abstract class QuarkusActionNode extends DefaultMutableTreeNode {

    private static final Icon RUNNING_ICON = new AnimatedIcon.Default();
    private final String name;

    private final QuarkusProjectNode projectNode;

    public QuarkusActionNode(String name, QuarkusProjectNode projectNode) {
        this.name = name;
        this.projectNode = projectNode;
    }

    public String getName() {
        return name;
    }

    public Module getModule() {
        return projectNode.getModule();
    }

    public QuarkusProjectNode getProjectNode() {
        return projectNode;
    }

    public String getDisplayName() {
        return name;
    }

    public Icon getIcon() {
        return AllIcons.Actions.InlayGear;
    }

    public void refreshNode(boolean nodeStructureChanged) {
        invokeLater(() -> {
            var tree = projectNode.getTree();
            if (nodeStructureChanged) {
                ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(this);
            } else {
                ((DefaultTreeModel) tree.getModel()).nodeChanged(this);
            }
            var treePath = new TreePath(this.getPath());
            tree.expandPath(treePath);
        });
    }

    private static void invokeLater(Runnable runnable) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            runnable.run();
        } else {
            ApplicationManager.getApplication().invokeLater(runnable);
        }
    }

    public abstract String getActionId();
}
