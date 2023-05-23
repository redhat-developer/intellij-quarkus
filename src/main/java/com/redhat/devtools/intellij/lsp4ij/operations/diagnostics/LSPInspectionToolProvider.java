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
package com.redhat.devtools.intellij.lsp4ij.operations.diagnostics;

import com.intellij.codeInspection.InspectionToolProvider;
import org.jetbrains.annotations.NotNull;

public class LSPInspectionToolProvider implements InspectionToolProvider {
    @NotNull
    @Override
    public Class[] getInspectionClasses() {
        return new Class[] { LSPLocalInspectionTool.class};
    }
}
