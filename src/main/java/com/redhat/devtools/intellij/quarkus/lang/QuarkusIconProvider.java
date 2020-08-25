/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.lang;

import com.intellij.ide.IconProvider;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class QuarkusIconProvider extends IconProvider {
    private static final Icon QUARKUS_ICON = IconLoader.findIcon("/quarkus_icon_rgb_16px_default.png", QuarkusIconProvider.class);

    @Nullable
    @Override
    public Icon getIcon(@NotNull PsiElement element, int flags) {
        if (element instanceof PropertiesFile) {
            VirtualFile file = element.getContainingFile().getVirtualFile();
            if (QuarkusModuleUtil.isQuarkusPropertiesFile(file, element.getProject())) {
                    return QUARKUS_ICON;
            }
        }
        return null;
    }
}
