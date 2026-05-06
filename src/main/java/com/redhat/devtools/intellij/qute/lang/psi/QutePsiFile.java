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
package com.redhat.devtools.intellij.qute.lang.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.redhat.devtools.intellij.qute.lang.QuteFileType;
import com.redhat.devtools.intellij.qute.lang.QuteLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Qute Psi file.
 */
public class QutePsiFile extends PsiFileBase {
    public QutePsiFile(FileViewProvider viewProvider) {
        super(viewProvider, QuteLanguage.INSTANCE);
    }

    @Override
    public @NotNull FileType getFileType() {
        return QuteFileType.QUTE;
    }

    @Override
    public @Nullable PsiElement findElementAt(int offset) {
        // For template language files with TemplateLanguageFileViewProvider,
        // we need to search in the Qute language tree, not the template data language tree
        FileViewProvider viewProvider = getViewProvider();

        // First, try to find element in Qute language
        PsiElement quteElement = viewProvider.findElementAt(offset, QuteLanguage.INSTANCE);

        // If found in Qute tree, return it
        if (quteElement != null) {
            return quteElement;
        }

        // Otherwise, fallback to default behavior (template data language like HTML)
        return super.findElementAt(offset);
    }
}
