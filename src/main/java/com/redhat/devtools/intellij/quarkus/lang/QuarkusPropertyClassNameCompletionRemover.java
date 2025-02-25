/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.lang;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import org.jetbrains.annotations.NotNull;

/**
 * IntelliJ provides content assist for properties by default scanning class names
 * for property values. This pseudo contributor will make sure it is disabled.
 */
public class QuarkusPropertyClassNameCompletionRemover extends CompletionContributor {
    private static final PatternCondition<PsiElement> APPLICATION_PROPERTIES = new PatternCondition<PsiElement>("isQuarkusConfigurationFile") {
        @Override
        public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext context) {
            return QuarkusModuleUtil.isQuarkusPropertiesFile(psiElement.getContainingFile().getOriginalFile().getVirtualFile(), psiElement.getProject());
        }
    };

    public QuarkusPropertyClassNameCompletionRemover() {
        this.extend(CompletionType.BASIC, PlatformPatterns.psiElement(PropertyValueImpl.class).with(APPLICATION_PROPERTIES), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                result.stopHere();
            }
        });
    }
}
