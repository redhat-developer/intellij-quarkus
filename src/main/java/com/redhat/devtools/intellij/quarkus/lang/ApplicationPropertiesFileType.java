/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.lang;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class ApplicationPropertiesFileType extends LanguageFileType {
    public static final String EXTENSION = "properties";

    public static final ApplicationPropertiesFileType INSTANCE = new ApplicationPropertiesFileType();

    private ApplicationPropertiesFileType() {
        super(ApplicationPropertiesLanguage.INSTANCE);
    }
    @NotNull
    @Override
    public String getName() {
        return "Quarkus properties";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Quarkus properties file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return ApplicationPropertiesFileType.EXTENSION;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return null;
    }
}
