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

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.java.JavaLanguage;
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
 * Qute-specific support for resolving variable positions in the Debug Adapter.
 * <p>
 * This class computes the exact text range of Qute expressions in order to
 * evaluate variables during debugging sessions.
 * <p>
 * It supports both:
 * <ul>
 *   <li>Red Hat Qute PSI tokens</li>
 *   <li>JetBrains Quarkus Qute PSI elements</li>
 * </ul>
 * and handles Qute code embedded (injected) inside Java source files.
 */
public class QuteDebugAdapterVariableSupport extends DebugAdapterVariableSupport {

    /**
     * JetBrains Qute identifier token name.
     */
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

    /**
     * Computes the text range of the Qute variable located at the given offset.
     * <p>
     * This method supports both standalone Qute files and Qute code injected
     * inside Java source files.
     *
     * @param project             the current project
     * @param file                the virtual file being debugged
     * @param document            the associated document
     * @param offset              caret offset in the document
     * @param sideEffectsAllowed  whether PSI side effects are allowed
     * @return the resolved text range, or {@code null} if not applicable
     */
    @Override
    protected @Nullable TextRange getTextRange(@NotNull Project project,
                                               @NotNull VirtualFile file,
                                               @NotNull Document document,
                                               int offset,
                                               boolean sideEffectsAllowed) {

        PsiFile psiFile = LSPIJUtils.getPsiFile(file, project);
        if (psiFile == null) {
            return null;
        }

        boolean injected = psiFile.getLanguage().is(JavaLanguage.INSTANCE);

        // Retrieve the PSI element at the given offset, handling injected Qute code
        PsiElement element = injected
                ? InjectedLanguageManager.getInstance(project)
                .findInjectedElementAt(psiFile, offset)
                : psiFile.findElementAt(offset);

        if (element == null || !QuteLanguage.isQuteLanguage(element.getLanguage())) {
            return null;
        }

        TextRange textRange = getTextRange(element);

        // Convert injected text range back to host document coordinates
        if (injected && textRange != null) {
            return InjectedLanguageManager.getInstance(project)
                    .injectedToHost(element, textRange);
        }

        return textRange;
    }

    /**
     * Computes the text range of a Qute expression based on its PSI structure.
     * <p>
     * This method supports:
     * <ul>
     *   <li>Red Hat Qute tokens</li>
     *   <li>JetBrains Qute PSI tree structures</li>
     * </ul>
     *
     * @param element the PSI element at the caret position
     * @return the text range representing the variable or expression,
     *         or {@code null} if it cannot be resolved
     */
    private static @Nullable TextRange getTextRange(@NotNull PsiElement element) {

        // Red Hat Qute token
        if (element instanceof QuteToken quteToken) {
            return quteToken.getTextRangeInExpression();
        }

        // JetBrains Qute PSI structure
        IElementType tokenType = getTokenType(element);

        if (isTokenType(tokenType, QUTE_JETBRAINS_IDENTIFIER)) {
            PsiElement parent = element.getParent();
            tokenType = getTokenType(parent);

            if (isTokenType(tokenType, QUTE_JETBRAINS_IDENTIFIER_EXPR)) {
                // Object part:
                //   item
                //   uri:item
                parent = parent.getParent();
                tokenType = getTokenType(parent);

                if (isTokenType(tokenType, QUTE_JETBRAINS_REFERENCE_EXPR)) {
                    PsiElement namespaceParent = parent.getParent();
                    tokenType = getTokenType(namespaceParent);

                    if (isTokenType(tokenType, QUTE_JETBRAINS_NAMESPACE_EXPR)) {
                        // Namespace-qualified reference: uri:item
                        return new TextRange(
                                namespaceParent.getTextRange().getStartOffset(),
                                element.getTextRange().getEndOffset()
                        );
                    }
                }

                // Simple object reference: item
                return element.getTextRange();

            } else if (isTokenType(tokenType, QUTE_JETBRAINS_QUALIFIER)) {
                // Property or method part:
                //   item.foo
                //   item.bar()
                parent = parent.getParent();
                tokenType = getTokenType(parent);

                if (isTokenType(tokenType, QUTE_JETBRAINS_REFERENCE_EXPR)) {
                    PsiElement namespaceParent = parent.getParent();
                    tokenType = getTokenType(namespaceParent);

                    if (isTokenType(tokenType, QUTE_JETBRAINS_NAMESPACE_EXPR)) {
                        parent = namespaceParent;
                    }

                    PsiElement callExprParent = parent.getParent();
                    tokenType = getTokenType(callExprParent);

                    if (isTokenType(tokenType, QUTE_JETBRAINS_CALL_EXPR)) {
                        // Method call: item.bar()
                        return callExprParent.getTextRange();
                    }
                }

                // Property access: item.foo
                return parent.getTextRange();
            }
        }

        return null;
    }

    /**
     * Returns the element type of the given PSI element.
     */
    private static @Nullable IElementType getTokenType(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }
        return element.getNode() != null ? element.getNode().getElementType() : null;
    }

    /**
     * Checks whether the given token type matches the expected token name.
     *
     * @param tokenType the PSI element type
     * @param tokenName the expected token name
     * @return {@code true} if the token type matches
     */
    public static boolean isTokenType(@Nullable IElementType tokenType,
                                      @NotNull String tokenName) {
        return tokenType != null && tokenName.equals(tokenType.toString());
    }
}
