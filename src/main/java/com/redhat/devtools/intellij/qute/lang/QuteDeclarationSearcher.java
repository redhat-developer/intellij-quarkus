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
package com.redhat.devtools.intellij.qute.lang;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.pom.PomDeclarationSearcher;
import com.intellij.pom.PomTarget;
import com.intellij.psi.DelegatePsiTarget;
import com.intellij.psi.PsiElement;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

public class QuteDeclarationSearcher extends PomDeclarationSearcher {
    @Override
    public void findDeclarationsAt(@NotNull PsiElement element, int offsetInElement, Consumer<PomTarget> consumer) {
        Language lang = LanguageUtil.getLanguageForPsi(element.getProject(), element.getContainingFile().getVirtualFile());
        if (lang == QuteLanguage.INSTANCE) {
            consumer.consume(new DelegatePsiTarget(element));
        }
    }
}
