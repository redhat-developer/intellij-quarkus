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

import com.intellij.execution.DefaultExecutionTarget;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.remote.RemoteConfiguration;
import com.intellij.execution.remote.RemoteConfigurationType;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfiguration.QUARKUS_CONFIGURATION;

/**
 * ProcessListener which tracks the message Listening for transport dt_socket at address: $PORT' to start the
 * remote debugger of the given port $PORT.
 */
class AttachDebuggerProcessListener implements ProcessListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(AttachDebuggerProcessListener.class);

    private static final String LISTENING_FOR_TRANSPORT_DT_SOCKET_AT_ADDRESS = "Listening for transport dt_socket at address: ";

    private static final String JWDP_HANDSHAKE = "JDWP-Handshake";

    private final Project project;
    private final ExecutionEnvironment env;
    private boolean connected; // to prevent from several messages like 'Listening for transport dt_socket at address:'

    AttachDebuggerProcessListener(@NotNull Project project,
                                  @NotNull ExecutionEnvironment env) {
        this.project = project;
        this.env = env;
    }

    @Override
    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        String message = event.getText();
        if (!connected && message.startsWith(LISTENING_FOR_TRANSPORT_DT_SOCKET_AT_ADDRESS)) {
            connected = true;
            Integer debugPort = getDebugPort(message);
            if (debugPort == null) {
                LOGGER.error("Cannot extract port from the given message: " + message);
                return;
            }
            ProgressManager.getInstance().run(new Task.Backgroundable(project, QUARKUS_CONFIGURATION, false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    String name = env.getRunProfile().getName();
                    createRemoteConfiguration(indicator, debugPort, name);
                }
            });
        }
    }

    @Nullable
    private static Integer getDebugPort(String message) {
        try {
            String port = message.substring(LISTENING_FOR_TRANSPORT_DT_SOCKET_AT_ADDRESS.length(), message.length()).trim();
            return Integer.valueOf(port);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void processTerminated(@NotNull ProcessEvent event) {
        event.getProcessHandler().removeProcessListener(this);
    }

    private void createRemoteConfiguration(ProgressIndicator indicator, int port, String name) {
        indicator.setText("Connecting Java debugger to port " + port);
        try {
            waitForPortAvailable(port, indicator);
            RunnerAndConfigurationSettings settings = RunManager.getInstance(project).createConfiguration(name + " (Remote)", RemoteConfigurationType.class);
            RemoteConfiguration remoteConfiguration = (RemoteConfiguration) settings.getConfiguration();
            remoteConfiguration.PORT = Integer.toString(port);
            long groupId = ExecutionEnvironment.getNextUnusedExecutionId();
            ExecutionUtil.runConfiguration(settings, DefaultDebugExecutor.getDebugExecutorInstance(), DefaultExecutionTarget.INSTANCE, groupId);
        } catch (IOException e) {
            ApplicationManager.getApplication()
                    .invokeLater(() -> Messages.showErrorDialog("Can' t connector to port " + port, "Quarkus"));
        }
    }

    private void waitForPortAvailable(int port, ProgressIndicator monitor) throws IOException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 120_000 && !monitor.isCanceled()) {
            try (Socket socket = new Socket("localhost", port)) {
                socket.getOutputStream().write(JWDP_HANDSHAKE.getBytes(StandardCharsets.US_ASCII));
                return;
            } catch (ConnectException e) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e1) {
                    throw new IOException(e1);
                }
            }
        }
        throw new IOException("Can't connect remote debugger to port " + port);
    }
}
