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

import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.redhat.devtools.lsp4ij.dap.breakpoints.DAPBreakpointHandlerBase;
import org.jetbrains.annotations.NotNull;

/**
 * Qute breakpoint handler.
 */
public class QuteBreakpointHandler extends DAPBreakpointHandlerBase<XLineBreakpoint<XBreakpointProperties<?>>> {
    public QuteBreakpointHandler(@NotNull XDebugSession session,
                                 @NotNull QuteDebugAdapterDescriptor quteDebugAdapterDescriptor,
                                 @NotNull Project project) {
        super(QuteBreakpointType.class, session, quteDebugAdapterDescriptor, project);
    }
}
