/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.run;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.redhat.devtools.intellij.qute.lang.QuteLanguage;
import com.redhat.devtools.intellij.qute.lang.psi.QuteElementType;
import com.redhat.devtools.intellij.qute.lang.psi.QuteElementTypes;
import com.redhat.devtools.intellij.qute.lang.psi.QuteToken;
import com.redhat.devtools.intellij.qute.lang.psi.QuteTokenType;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.dap.client.variables.providers.DebugVariablePositionProvider;
import com.redhat.devtools.lsp4ij.dap.client.variables.providers.HighlighterDebugVariablePositionProvider;
import com.redhat.devtools.lsp4ij.dap.descriptors.DebugAdapterVariableSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * Qute debug adapter variable support.
 */
public class QuteDebugAdapterVariableSupport extends DebugAdapterVariableSupport {

    @Override
    public @NotNull Collection<DebugVariablePositionProvider> getDebugVariablePositionProvider() {
        return Collections.singletonList(new QuteDebugVariablePositionProvider());
    }

    @Override
    protected @Nullable TextRange getTextRange(@NotNull Project project, @NotNull VirtualFile file, @NotNull Document document, int offset, boolean sideEffectsAllowed) {
        PsiFile psiFile = LSPIJUtils.getPsiFile(file, project);
        if (psiFile == null) {
            return null;
        }
        var element = psiFile.findElementAt(offset);
        if (element == null || !QuteLanguage.INSTANCE.equals(element.getLanguage())) {
            return null;
        }
        if (element instanceof QuteToken quteToken) {
            return quteToken.getTextRangeInExpression();
        }
        return null;
    }

}


