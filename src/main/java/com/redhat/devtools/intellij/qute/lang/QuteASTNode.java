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
package com.redhat.devtools.intellij.qute.lang;

import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.redhat.qute.parser.template.ASTVisitor;
import com.redhat.qute.parser.template.Node;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class QuteASTNode extends CompositeElement {
    private final Node node;

    public QuteASTNode(Node node) {
        super(QuteElementTypes.fromNode(node));
        this.node = node;
        int end = node.getStart();
        for(Node child : getChildren(node)) {
            if (end != -1 && end < child.getStart()) {
                rawAddChildrenWithoutNotifications(new LeafPsiElement(new QuteElementType("TEXT"), child.getOwnerTemplate().getText(end, child.getStart())));
            }
            rawAddChildrenWithoutNotifications(getNode(child));
            end = child.getEnd();
        }
        if (end < node.getEnd()) {
            rawAddChildrenWithoutNotifications(new LeafPsiElement(QuteElementTypes.QUTE_CONTENT, node.getOwnerTemplate().getText(end, node.getEnd())));
        }
    }

    @NotNull
    public static TreeElement getNode(Node child) {
        return getChildren(child).isEmpty()?new LeafPsiElement(new QuteElementType(child.getNodeName()), child.getOwnerTemplate().getText(child.getStart(), child.getEnd())):new QuteASTNode(child);
    }

    private static List<Node> getChildren(Node node) {
        List<Node> children = new ArrayList<>();
        node.accept(new ASTVisitor() {
            @Override
            public boolean preVisit2(Node child) {
                if (child.getParent() == node) {
                    children.add(child);
                }
                return child.getParent() == node || child == node;
            }
        });
        return children;
    }
}
