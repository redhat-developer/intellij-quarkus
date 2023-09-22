package com.redhat.devtools.intellij.quarkus.explorer;

import com.intellij.openapi.module.Module;
import com.intellij.ui.treeStructure.Tree;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProject;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.redhat.devtools.intellij.quarkus.lang.QuarkusIcons;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunContext;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

public class QuarkusProjectNode extends DefaultMutableTreeNode {

    private final QuarkusRunContext runContext;

    private final Tree tree;

    public QuarkusProjectNode(Module module,Tree tree) {
        this.runContext = new QuarkusRunContext(module);
        this.tree = tree;
    }

    public String getDisplayName() {
        return getModule().getName();
    }

    public Icon getIcon() {
        return QuarkusIcons.Quarkus;
    }

    public Module getModule() {
        return runContext.getMicroProfileProject().getJavaProject();
    }

    public QuarkusRunContext getRunContext() {
        return runContext;
    }

    public Tree getTree() {
        return tree;
    }
}
