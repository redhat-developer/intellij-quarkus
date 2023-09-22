package com.redhat.devtools.intellij.quarkus.explorer;

import com.intellij.openapi.module.Module;
import com.intellij.ui.treeStructure.Tree;
import com.redhat.devtools.intellij.quarkus.run.QuarkusOpenDevUIAction;

import javax.swing.tree.DefaultTreeModel;

public class QuarkusOpenDevUINode extends QuarkusActionNode {
    public QuarkusOpenDevUINode(QuarkusProjectNode projectNode) {
        super("Open Dev UI", projectNode);
    }

    @Override
    public String getActionId() {
        return QuarkusOpenDevUIAction.ACTION_ID;
    }
}
