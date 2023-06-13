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
package com.redhat.devtools.intellij.qute.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implement a fake {@link PsiReference} which is required for hover and avoid having this kind of bug
 * https://github.com/redhat-developer/intellij-quarkus/issues/944
 */
public class QutePsiReference implements PsiReference {

    private final ASTNode node;

    private final TextRange rangeInElement;

    public QutePsiReference(ASTNode node) {
        this.node = node;
        this.rangeInElement = new TextRange(0, node.getPsi().getTextRange().getLength());
    }

    @Override
    public @NotNull PsiElement getElement() {
        return node.getPsi();
    }

    @Override
    public @NotNull TextRange getRangeInElement() {
        return rangeInElement;
    }

    @Override
    public @Nullable PsiElement resolve() {
        return node.getPsi();
    }

    @Override
    public @NotNull @NlsSafe String getCanonicalText() {
        return node.getPsi().getText();
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        return null;
    }

    @Override
    public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        return null;
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return false;
    }

    @Override
    public boolean isSoft() {
        return false;
    }
}
