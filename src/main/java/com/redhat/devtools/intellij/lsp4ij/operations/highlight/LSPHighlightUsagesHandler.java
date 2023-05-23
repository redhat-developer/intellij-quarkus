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
package com.redhat.devtools.intellij.lsp4ij.operations.highlight;

import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Logger;

public class LSPHighlightUsagesHandler extends HighlightUsagesHandlerBase<PsiElement> {
    private static final Logger LOGGER = Logger.getLogger(LSPHighlightUsagesHandler.class.getName());
    private final List<PsiElement> targets;

    public LSPHighlightUsagesHandler(Editor editor, PsiFile file, List<PsiElement> targets) {
        super(editor, file);
        this.targets = targets;
    }

    @Override
    public @NotNull List<PsiElement> getTargets() {
        return targets;
    }

    @Override
    protected void selectTargets(@NotNull List<? extends PsiElement> targets,
                                 @NotNull Consumer<? super List<? extends PsiElement>> selectionConsumer) {
        selectionConsumer.consume(targets);
    }

    @Override
    public void computeUsages(@NotNull List<? extends PsiElement> targets) {
        targets.forEach(target -> myReadUsages.add(target.getTextRange()));
    }
}
