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
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

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
        if (settings != null && settings.getConfiguration() instanceof QuarkusRunConfiguration) {
            // The execution has been done by debugging a Quarkus run configuration (Gradle / Maven)
            // add a AttachDebuggerProcessListener to track
            // 'Listening for transport dt_socket at address: $PORT' message and starts
            // the remote debugger with the givenport $PORT
            handler.addProcessListener(new AttachDebuggerProcessListener(project, env));
        }
    }

}
