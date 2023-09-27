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

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import com.redhat.devtools.intellij.quarkus.lang.QuarkusIconProvider;
import com.redhat.devtools.intellij.qute.lang.QuteLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * MicroProfile language file type.
 */
public class MicroProfileFileType extends LanguageFileType {
    private static final Icon QUARKUS_ICON = IconLoader.findIcon("/quarkus_icon_rgb_16px_default.png", QuarkusIconProvider.class);

    @NotNull
    public static final MicroProfileFileType INSTANCE = new MicroProfileFileType();

    private MicroProfileFileType() {
        super(MicroProfileLanguage.INSTANCE);
    }

    @Override
    public @NotNull String getName() {
        return "MicroProfile";
    }

    @Override
    public @NotNull
    String getDescription() {
        return "MicroProfile";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "properties";
    }

    @Override
    public @Nullable Icon getIcon() {
        return QUARKUS_ICON;
    }
}
