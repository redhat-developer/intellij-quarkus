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
import com.redhat.devtools.intellij.qute.lang.QuteFileType;
import com.redhat.devtools.intellij.qute.lang.QuteLanguage;
import org.jetbrains.annotations.NotNull;

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
}
