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

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.redhat.devtools.intellij.qute.lang.QuteLanguage;
import com.redhat.devtools.intellij.qute.lang.psi.QuteToken;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.dap.client.variables.providers.DebugVariablePositionProvider;
import com.redhat.devtools.lsp4ij.dap.descriptors.DebugAdapterVariableSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * Qute debug adapter variable support.
 */
public class QuteDebugAdapterVariableSupport extends DebugAdapterVariableSupport {

    public static final String QUTE_JETBRAINS_IDENTIFIER = "QuteTokenType.IDENTIFIER";
    private static final String QUTE_JETBRAINS_IDENTIFIER_EXPR = "QuteElementType.IDENTIFIER_EXPR";
    private static final String QUTE_JETBRAINS_REFERENCE_EXPR = "QuteElementType.REFERENCE_EXPR";
    private static final String QUTE_JETBRAINS_NAMESPACE_EXPR = "QuteElementType.NAMESPACE_EXPR";
    private static final String QUTE_JETBRAINS_QUALIFIER = "QuteElementType.QUALIFIER";
    private static final String QUTE_JETBRAINS_CALL_EXPR = "QuteElementType.CALL_EXPR";

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
        if (element == null || !QuteLanguage.isQuteLanguage(element.getLanguage())) {
            return null;
        }
        // It is a Qute element
        if (element instanceof QuteToken quteToken) {
            // Qute Token coming from RedHat Quarkus
            return quteToken.getTextRangeInExpression();
        }
        // Qute Token coming from JetBrains Quarkus
        var tokenType = getTokenType(element);
        if (isTokenType(tokenType, QUTE_JETBRAINS_IDENTIFIER)) {
            var parent = element.getParent();
            tokenType = getTokenType(parent);
            if (isTokenType(tokenType, QUTE_JETBRAINS_IDENTIFIER_EXPR)) {
                // Object part:
                // item
                // or uri:Item
                parent = parent.getParent();
                tokenType = getTokenType(parent);
                if (isTokenType(tokenType, QUTE_JETBRAINS_REFERENCE_EXPR)) {
                    var nsParent = parent.getParent();
                    tokenType = getTokenType(nsParent);
                    if (isTokenType(tokenType, QUTE_JETBRAINS_NAMESPACE_EXPR)) {
                        // uri:Item
                        return new TextRange(nsParent.getTextRange().getStartOffset(), element.getTextRange().getEndOffset());
                    }
                }
                // item
                return element.getTextRange();
            } else if (isTokenType(tokenType, QUTE_JETBRAINS_QUALIFIER)) {
                // Property or Method part
                // item.foo
                // item.bar()
                parent = parent.getParent();
                tokenType = getTokenType(parent);
                if (isTokenType(tokenType, QUTE_JETBRAINS_REFERENCE_EXPR)) {
                    var nsParent = parent.getParent();
                    tokenType = getTokenType(nsParent);
                    if (isTokenType(tokenType, QUTE_JETBRAINS_NAMESPACE_EXPR)) {
                        parent = nsParent;
                    }
                    var callExprParent = parent.getParent();
                    tokenType = getTokenType(callExprParent);
                    if (isTokenType(tokenType, QUTE_JETBRAINS_CALL_EXPR)) {
                        // Method part
                        // item.bar()
                        return callExprParent.getTextRange();
                    }
                }
                // Property part
                // item.foo
                return parent.getTextRange();
            }
        }
        return null;
    }

    private static @Nullable IElementType getTokenType(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }
        var node = element.getNode();
        return node != null ? node.getElementType() : null;
    }

    public static boolean isTokenType(@Nullable IElementType tokenType, @NotNull String tokenName) {
        return tokenType != null && tokenName.equals(tokenType.toString());
    }

}


