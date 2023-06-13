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
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.RootPolicy;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.io.ByteSequence;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.lang.QuarkusIconProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * Qute language file type.
 */
public class QuteFileType extends LanguageFileType {
    private static final Icon QUARKUS_ICON = IconLoader.findIcon("/quarkus_icon_rgb_16px_default.png", QuarkusIconProvider.class);

    @NotNull
    public static final QuteFileType QUTE = new QuteFileType();

    private QuteFileType() {
        super(QuteLanguage.INSTANCE);
    }

    @Override
    public @NotNull String getName() {
        return "Qute_";
    }

    @Override
    public @NotNull
    String getDescription() {
        return "Qute";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "html";
    }

    @Override
    public @Nullable Icon getIcon() {
        return QUARKUS_ICON;
    }
}
