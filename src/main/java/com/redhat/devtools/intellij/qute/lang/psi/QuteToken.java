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

import com.intellij.psi.HintedReferenceHost;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * Qute token.
 *
 */
public class QuteToken extends LeafPsiElement implements HintedReferenceHost {

    public QuteToken(@NotNull IElementType type, CharSequence text) {
        super(type, text);
    }

    @Override
    public PsiReference @NotNull [] getReferences() {
        return getReferences(PsiReferenceService.Hints.NO_HINTS);
    }

    @Override
    public PsiReference @NotNull [] getReferences(PsiReferenceService.@NotNull Hints hints) {
        return new PsiReference[] {new QutePsiReference(getNode())};
    }

    @Override
    public boolean shouldAskParentForReferences(PsiReferenceService.@NotNull Hints hints) {
        return false;
    }
}
