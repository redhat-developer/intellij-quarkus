/*******************************************************************************
 * Copyright (c) 2024 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.quarkus.run;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.BaseOSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.redhat.devtools.intellij.quarkus.run.AttachDebuggerProcessListener.isDebuggerAutoAttach;

/**
 * Execution listener singleton which tracks any process starting to add in debug mode
 * an instance of {@link AttachDebuggerProcessListener} to the process handler
 * if it is a Quarkus run configuration.
 */
class AttachDebuggerExecutionListener implements ExecutionListener {

    private final @NotNull Project project;

    AttachDebuggerExecutionListener(@NotNull Project project) {
        this.project = project;
    }

    public void processStarting(@NotNull String executorId,
                                @NotNull ExecutionEnvironment env,
                                @NotNull ProcessHandler handler) {
        if (!DefaultDebugExecutor.EXECUTOR_ID.equals(executorId)) {
            return;
        }
        // Debug mode...
        RunnerAndConfigurationSettings settings = env.getRunnerAndConfigurationSettings();
        if (settings.getConfiguration() instanceof QuarkusRunConfiguration) {
            if (!isDebuggerAutoAttach()) {
                // The execution has been done by debugging a Quarkus run configuration (Gradle / Maven)
                // add a AttachDebuggerProcessListener to track
                // 'Listening for transport dt_socket at address: $PORT' message and starts
                // the remote debugger with the given port $PORT
               handler.addProcessListener(new AttachDebuggerProcessListener(project, env, getDebugPort(handler)));
            }
        }
    }

    /**
     * Returns the port declared in the command line with -Ddebug= and null otherwise.
     *
     * @param handler the process handler.
     * @return the port declared in the command line with -Ddebug= and null otherwise.
     */
    private @Nullable Integer getDebugPort(@NotNull ProcessHandler handler) {
        if (handler instanceof BaseOSProcessHandler osProcessHandler) {
            String commandLine = osProcessHandler.getCommandLine();
            int startIndex = commandLine.indexOf("-Ddebug=");
            if (startIndex != -1) {
                StringBuilder port = new StringBuilder();
                for (int i = startIndex + "-Ddebug=".length(); i < commandLine.length(); i++) {
                    char c = commandLine.charAt(i);
                    if (Character.isDigit(c)) {
                        port.append(c);
                    } else {
                        break;
                    }
                }
                if (!port.isEmpty()) {
                    return Integer.parseInt(port.toString());
                }
            }
        }
        return null;
    }

}
