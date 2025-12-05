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


import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunConfigurationOptions;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.redhat.devtools.intellij.qute.lang.QuteFileType;
import com.redhat.devtools.intellij.qute.run.client.QuteDAPClient;
import com.redhat.devtools.lsp4ij.dap.DAPDebugProcess;
import com.redhat.devtools.lsp4ij.dap.DAPDebuggerEditorsProvider;
import com.redhat.devtools.lsp4ij.dap.DebugMode;
import com.redhat.devtools.lsp4ij.dap.breakpoints.DAPBreakpointHandler;
import com.redhat.devtools.lsp4ij.dap.breakpoints.DAPBreakpointHandlerBase;
import com.redhat.devtools.lsp4ij.dap.breakpoints.DAPBreakpointProperties;
import com.redhat.devtools.lsp4ij.dap.client.DAPClient;
import com.redhat.devtools.lsp4ij.dap.client.LaunchUtils;
import com.redhat.devtools.lsp4ij.dap.definitions.DebugAdapterServerDefinition;
import com.redhat.devtools.lsp4ij.dap.descriptors.DebugAdapterDescriptor;
import com.redhat.devtools.lsp4ij.settings.ServerTrace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Qute debugger adapter descriptor.
 */
public class QuteDebugAdapterDescriptor extends DebugAdapterDescriptor {

    public QuteDebugAdapterDescriptor(@NotNull RunConfigurationOptions options,
                                      @NotNull ExecutionEnvironment environment,
                                      @Nullable DebugAdapterServerDefinition serverDefinition) {
        super(options, environment, serverDefinition);
        super.setVariableSupport(new QuteDebugAdapterVariableSupport());
    }

    @Override
    public ProcessHandler startServer() throws ExecutionException {
        return null;
    }

    @Override
    public @NotNull Map<String, Object> getDapParameters() {
        // language=JSON
        String launchJson = """                
                {
                  "type": "qute",
                  "name": "Attach Qute template",
                  "request": "attach"
                }
                """;
        LaunchUtils.LaunchContext context = new LaunchUtils.LaunchContext();
        return LaunchUtils.getDapParameters(launchJson, context);
    }

    @Override
    public @Nullable FileType getFileType() {
        return null;
    }

    @Override
    public @NotNull DebugMode getDebugMode() {
        return DebugMode.ATTACH;
    }

    @Override
    public @NotNull DAPBreakpointHandlerBase<?> createBreakpointHandler(@NotNull XDebugSession session, Project project) {
        return new QuteBreakpointHandler(session, this, project);
    }

    @Override
    public @NotNull XDebuggerEditorsProvider createDebuggerEditorsProvider(@Nullable FileType fileType, @NotNull DAPDebugProcess debugProcess) {
        return new QuteDebuggerEditorsProvider(fileType, debugProcess);
    }

    @Override
    public @NotNull DAPClient createClient(@NotNull DAPDebugProcess debugProcess, @NotNull Map<String, Object> dapParameters, boolean isDebug, @NotNull DebugMode debugMode, @NotNull ServerTrace serverTrace, @Nullable DAPClient parentClient) {
        return new QuteDAPClient(debugProcess, dapParameters, isDebug, debugMode, serverTrace, parentClient);
    }
}
