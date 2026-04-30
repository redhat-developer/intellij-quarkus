/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.lang;

import com.intellij.lang.properties.PropertiesLanguage;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import com.redhat.devtools.intellij.quarkus.lang.QuarkusIconProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * File type for Qute configuration files (.qute).
 *
 * Uses the Properties language for syntax highlighting and editing.
 */
public class QuteConfigFileType extends LanguageFileType {

    private static final Icon QUARKUS_ICON = IconLoader.findIcon("/quarkus_icon_rgb_16px_default.png", QuarkusIconProvider.class);

    public static final QuteConfigFileType INSTANCE = new QuteConfigFileType();

    private QuteConfigFileType() {
        super(PropertiesLanguage.INSTANCE);
    }

    @Override
    public @NotNull String getName() {
        return "Qute Configuration";
    }

    @Override
    public @NotNull String getDescription() {
        return "Qute Template Configuration";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "";
    }

    @Override
    public @Nullable Icon getIcon() {
        return QUARKUS_ICON;
    }
}
