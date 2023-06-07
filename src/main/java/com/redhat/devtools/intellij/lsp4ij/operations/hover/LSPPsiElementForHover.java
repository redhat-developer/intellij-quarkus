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
package com.redhat.devtools.intellij.lsp4ij.operations.hover;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.impl.FakePsiElement;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import org.jetbrains.annotations.NotNull;

/**
 * {@link PsiElement implementation which stores the editor and the target offset in the given editor.}
 */
public class LSPPsiElementForHover extends FakePsiElement {
    private final Project project;
    private final PsiFile file;
    private Editor editor;

    private int targetOffset;

    public LSPPsiElementForHover(Editor editor, PsiFile file, int targetOffset) {
        this.editor = editor;
        this.file = file;
        this.project = LSPIJUtils.getProject(file.getVirtualFile()).getProject();
        this.targetOffset = targetOffset;
    }

    public int getTargetOffset() {
        return targetOffset;
    }

    public Editor getEditor() {
        return editor;
    }

    @NotNull
    @Override
    public Project getProject() throws PsiInvalidElementAccessException {
        return project;
    }

    @Override
    public PsiFile getContainingFile() {
        return file;
    }

    @Override
    public PsiElement getParent() {
        return null;
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
