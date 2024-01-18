/*******************************************************************************
 * Copyright (c) 2024 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.projectWizard;

import com.intellij.codeInsight.daemon.JavaErrorBundle;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightClassUtil;
import com.intellij.ide.starters.shared.TextValidationFunction;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.impl.PsiNameHelperImpl;

/**
 * Holds  {@link TextValidationFunction}s implementations used to validate elements of a Quarkus project.
 *
 * @see com.intellij.ide.starters.shared.ValidationFunctions
 */
public class QuarkusValidationFunctions {

    private QuarkusValidationFunctions(){}

    // Borrowed from https://github.com/JetBrains/intellij-community/blob/bfb54733f1d10f2ba9164e5a82d3fc00150bf159/java/java-impl/src/com/intellij/ide/actions/CreateClassAction.java#L63-L70
    /**
     * Validates a Resource name when creating a new Quarkus project.
     */
    public static final TextValidationFunction CHECK_CLASS_NAME = inputString -> {
        //Technically we should target a different language level, to determine restricted names
        if (!inputString.isEmpty() && !PsiNameHelperImpl.getInstance().isQualifiedName(inputString)) {
            return JavaErrorBundle.message("create.class.action.this.not.valid.java.qualified.name");
        }
        String shortName = StringUtil.getShortName(inputString);
        if (HighlightClassUtil.isRestrictedIdentifier(shortName, LanguageLevel.HIGHEST)) {
            return JavaErrorBundle.message("restricted.identifier", shortName);
        }
        return null;
    };
}
