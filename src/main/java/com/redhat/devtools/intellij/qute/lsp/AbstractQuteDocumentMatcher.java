/*******************************************************************************
 * Copyright (c) 2023 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.lsp;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.qute.lang.QuteLanguage;
import com.redhat.devtools.lsp4ij.AbstractDocumentMatcher;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils;
import org.jetbrains.annotations.NotNull;

import static com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils.hasQuteSupport;
import static com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils.maybeBinaryQuteTemplate;

/**
 * Base class for Qute document matcher which checks that the file belongs to a Qute project.
 */
public class AbstractQuteDocumentMatcher extends AbstractDocumentMatcher {

    @Override
    public boolean match(@NotNull VirtualFile file,
                         @NotNull Project fileProject) {
        Module module = LSPIJUtils.getModule(file, fileProject);
        if (hasQuteSupport(module)) {
            return match(file, module);
        }
        return false;
    }

    protected boolean match(@NotNull VirtualFile file,
                            @NotNull Module module) {
        return true;
    }
}