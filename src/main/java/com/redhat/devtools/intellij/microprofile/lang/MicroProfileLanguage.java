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
package com.redhat.devtools.intellij.microprofile.lang;

import com.intellij.lang.InjectableLanguage;
import com.intellij.lang.Language;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.templateLanguages.TemplateLanguage;
import org.jetbrains.annotations.NotNull;

/**
 * Qute language.
 */
public class MicroProfileLanguage extends Language {

    @NotNull
    public static final MicroProfileLanguage INSTANCE = new MicroProfileLanguage();

    private MicroProfileLanguage() {
        super("MicroProfile");
    }

    @Override
    public @NotNull
    @NlsSafe String getDisplayName() {
        return "MicroProfile";
    }
}
