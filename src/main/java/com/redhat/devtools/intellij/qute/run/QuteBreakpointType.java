/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.run;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.redhat.devtools.lsp4ij.dap.breakpoints.DAPBreakpointTypeBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Qute breakpoint type.
 */
public class QuteBreakpointType extends DAPBreakpointTypeBase<XBreakpointProperties<?>> {

    private static final String BREAKPOINT_ID = "qute-breakpoint";

    public QuteBreakpointType() {
        super(BREAKPOINT_ID, "Qute Breakpoint");
    }

    @Override
    public @Nullable XBreakpointProperties<?> createBreakpointProperties(@NotNull VirtualFile virtualFile, int line) {
        return null;
    }

}
