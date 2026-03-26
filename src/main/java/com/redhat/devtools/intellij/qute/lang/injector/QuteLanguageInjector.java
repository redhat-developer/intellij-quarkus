/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.lang.injector;

import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.qute.lang.QuteFileViewProvider;
import com.redhat.devtools.intellij.qute.lang.QuteLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Language injector that applies Qute syntax highlighting to Java string
 * literals.
 * <p>
 * This injector enables Qute language support inside
 * {@link PsiLiteralExpression} instances, typically used in annotations.
 * <p>
 * Supported cases include:
 * <ul>
 *   <li>Java text blocks ({@code """..."""})</li>
 *   <li>Standard string literals ({@code "..."})</li>
 *   <li>Line breaks ({@code \n} and {@code \r\n})</li>
 *   <li>Empty or malformed literals (safely ignored)</li>
 * </ul>
 */
public class QuteLanguageInjector implements MultiHostInjector {

    /**
     * Computes the inner text range of a Java string literal that should
     * receive Qute language injection.
     * <p>
     * The returned range excludes surrounding quotes and, in the case of
     * text blocks, the optional newline immediately following the opening
     * {@code """}.
     *
     * @param context     the PSI context element
     * @param host        the language injection host
     * @param descriptor  Qute Java injection descriptor
     * @return the inner text range to inject into, or {@code null} if the
     *         literal is not suitable for injection
     */
    private static @Nullable TextRange getInnerRange(@NotNull PsiElement context,
                                                     @NotNull PsiLanguageInjectionHost host,
                                                     @NotNull QuteJavaInjectionDescriptor descriptor) {

        // If the literal is part of an annotation attribute, only inject
        // into the default "value" attribute
        PsiElement parent = context.getParent();
        if (parent instanceof PsiNameValuePair pair) {
            String name = pair.getName();
            if (name != null && !"value".equals(name)) {
                return null;
            }
        }

        String text = host.getText();
        if (text == null || text.length() < 2) {
            return null;
        }

        int innerStart;
        int innerEnd;

        // Java text block: """..."""
        if (text.startsWith("\"\"\"") && text.endsWith("\"\"\"") && text.length() >= 6) {
            innerStart = 3;
            innerEnd = text.length() - 3;

            // Skip the newline immediately following the opening """
            if (innerStart < text.length()) {
                char c = text.charAt(innerStart);
                if (c == '\n') {
                    innerStart++;
                } else if (c == '\r') {
                    innerStart++;
                    if (innerStart < text.length() && text.charAt(innerStart) == '\n') {
                        innerStart++;
                    }
                }
            }
        }
        // Standard string literal: "..."
        else if (text.startsWith("\"") && text.endsWith("\"")) {
            innerStart = 1;
            innerEnd = text.length() - 1;
        } else {
            // Not a supported string literal
            return null;
        }

        // Ensure the computed range is valid
        if (innerStart >= innerEnd) {
            return null;
        }

        return new TextRange(innerStart, innerEnd);
    }

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar,
                                     @NotNull PsiElement context) {

        // Only process Java string literals
        if (!(context instanceof PsiLiteralExpression literal)) {
            return;
        }

        // Ensure the literal supports language injection
        if (!(literal instanceof PsiLanguageInjectionHost host)) {
            return;
        }

        // Locate the enclosing annotation (if any)
        PsiAnnotation annotation =
                PsiTreeUtil.getParentOfType(literal, PsiAnnotation.class);

        QuteJavaInjectionDescriptor descriptor =
                QuteJavaInjectionRegistry.getInstance().getDescriptor(annotation);

        if (descriptor == null) {
            return;
        }

        TextRange innerRange = getInnerRange(context, host, descriptor);
        if (innerRange != null) {
            // Store the template language on the host for later retrieval
            var templateLanguage = descriptor.getTemplateDataLanguage(context);
            host.putUserData(QuteFileViewProvider.TEMPLATE_LANGUAGE_KEY, templateLanguage);

            // Inject Qute language into the computed text range
            registrar.startInjecting(QuteLanguage.INSTANCE)
                    .addPlace(null, null, host, innerRange)
                    .doneInjecting();
        }
    }

    @NotNull
    @Override
    public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
        // Restrict injection targets to Java string literals
        return Collections.singletonList(PsiLiteralExpression.class);
    }
}
