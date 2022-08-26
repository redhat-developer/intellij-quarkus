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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class ASTRewriteCorrectionProposal extends ChangeCorrectionProposal {
    private final PsiElement element;

    private final List<DocumentEvent> events = new ArrayList<>();
    private final PsiFile sourceCU;

    public ASTRewriteCorrectionProposal(String name, String kind, PsiElement element, int relevance, PsiFile sourceCU) {
        super(name, kind, relevance);
        this.element = element;
        this.sourceCU = sourceCU;
    }

    public PsiElement getElement() {
        return element;
    }

    public abstract void performUpdate();

    @Override
    public final Change getChange() {
        performUpdate();
        Document document = getElement().getContainingFile().getViewProvider().getDocument();
        CodeStyleManager.getInstance(getElement().getProject()).reformatText(getElement().getContainingFile(),
                0, document.getTextLength());
        return new Change(sourceCU.getViewProvider().getDocument(), document);
    }
}
