/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.lsp4ij.operations.diagnostics;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.ExternalAnnotatorBatchInspection;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class LSPLocalInspectionTool extends LocalInspectionTool implements ExternalAnnotatorBatchInspection {
    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "LSP";
    }

    @Nls
    @NotNull
    @Override
    public String getGroupDisplayName() {
        return "LSP";
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }
}
