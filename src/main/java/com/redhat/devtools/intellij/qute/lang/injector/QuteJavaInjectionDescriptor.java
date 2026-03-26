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

import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Describes an annotation that supports Qute template injection in Java code.
 * Provides a way to retrieve the template data language based on the PsiElement.
 */
public class QuteJavaInjectionDescriptor {

    private final @NotNull String annotationName;
    private final @Nullable Function<PsiElement, Language> templateLanguageProvider;

    /**
     * Creates a descriptor for a Qute-injectable annotation.
     *
     * @param annotationName the fully qualified name of the annotation
     */
    public QuteJavaInjectionDescriptor(@NotNull String annotationName) {
        this(annotationName, null);
    }

    /**
     * Creates a descriptor for a Qute-injectable annotation.
     *
     * @param annotationName the fully qualified name of the annotation
     * @param templateLanguageProvider function to determine the template language from a PsiElement (e.g., literal)
     */
    public QuteJavaInjectionDescriptor(@NotNull String annotationName,
                                       @Nullable Function<PsiElement, Language> templateLanguageProvider) {
        this.annotationName = annotationName;
        this.templateLanguageProvider = templateLanguageProvider;
    }

    /**
     * @return the fully qualified name of the annotation
     */
    public @NotNull String getAnnotationName() {
        return annotationName;
    }

    /**
     * Determines the template language for the given element (literal) using the provider.
     *
     * @param element the PsiElement to inspect
     * @return the Language to use for Qute injection, or null if none
     */
    public @Nullable Language getTemplateDataLanguage(@NotNull PsiElement element) {
        return templateLanguageProvider != null ? templateLanguageProvider.apply(element) : null;
    }
}
