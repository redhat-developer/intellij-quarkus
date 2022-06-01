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

import com.intellij.lang.InjectableLanguage;
import com.intellij.lang.Language;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.templateLanguages.TemplateLanguage;
import org.jetbrains.annotations.NotNull;

public class QuteLanguage extends Language implements TemplateLanguage, InjectableLanguage {
    public static final QuteLanguage INSTANCE = new QuteLanguage();

    private QuteLanguage() {
        super("Qute_");
    }

    @Override
    public @NotNull
    @NlsSafe String getDisplayName() {
        return "Qute";
    }
}
