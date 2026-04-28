/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.lsp;

import com.intellij.psi.PsiFile;
import com.redhat.devtools.lsp4ij.client.features.LSPBreadcrumbsFeature;
import org.jetbrains.annotations.NotNull;

/**
 * Qute breadcrumbs feature which disables breadcrumbs support.
 * <p>
 * Qute language server does not support breadcrumbs/document symbols properly,
 * and enabling them causes slow operations on EDT which can freeze the UI.
 * </p>
 */
public class QuteBreadcrumbsFeature extends LSPBreadcrumbsFeature {

    @Override
    public boolean isSupported(@NotNull PsiFile file) {
        // Qute does not support breadcrumbs
        return false;
    }

    @Override
    public boolean isEnabled(@NotNull PsiFile file) {
        // Qute does not support breadcrumbs
        return false;
    }
}
