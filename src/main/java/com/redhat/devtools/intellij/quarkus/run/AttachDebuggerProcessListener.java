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
import com.intellij.execution.process.ProcessOutputType;
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
import com.intellij.openapi.util.registry.Registry;
import com.redhat.devtools.intellij.qute.run.QuteConfigurationType;
import com.redhat.devtools.intellij.qute.run.QuteRunConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.MissingResourceException;

import static com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfiguration.QUARKUS_CONFIGURATION;

/**
 * ProcessListener which tracks the message Listening for transport dt_socket at address: $PORT' to start the
 * remote debugger of the given port $PORT.
 */
public class AttachDebuggerProcessListener implements ProcessListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(AttachDebuggerProcessListener.class);

    private static final String LISTENING_FOR_TRANSPORT_DT_SOCKET_AT_ADDRESS = "Listening for transport dt_socket at address: ";
    private static final String JWDP_HANDSHAKE = "JDWP-Handshake";

    private static final String QUTE_LISTENING_ON_PORT = "Qute debugger server listening on port ";

    private final Project project;
    private final ExecutionEnvironment env;
    private final @Nullable Integer debugPort;
    private boolean connected; // to prevent from several messages like 'Listening for transport dt_socket at address:'
    private boolean quteConnected; // to prevent from several messages like 'Listening for transport dt_socket at address:'

    AttachDebuggerProcessListener(@NotNull Project project,
                                  @NotNull ExecutionEnvironment env,
                                  @Nullable Integer debugPort) {
        this.project = project;
        this.env = env;
        this.debugPort = debugPort;
    }

    @Override
    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        if (ProcessOutputType.isStdout(outputType)) {
            String message = event.getText();
            if (!connected && debugPort != null && message.startsWith(LISTENING_FOR_TRANSPORT_DT_SOCKET_AT_ADDRESS + debugPort)) {
                connected = true;
                ProgressManager.getInstance().run(new Task.Backgroundable(project, QUARKUS_CONFIGURATION, true) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        String name = env.getRunProfile().getName();
                        createRemoteConfiguration(indicator, debugPort, name);
                    }
                });
            } else if (!quteConnected && message.startsWith(QUTE_LISTENING_ON_PORT)) {
                quteConnected = true;
                Integer quteDebugPort = getQuteDebugPort(message);
                if (quteDebugPort == null) {
                    LOGGER.error("Cannot extract Qute debug port from the given message: {}", message);
                    return;
                }
                ProgressManager.getInstance().run(new Task.Backgroundable(project, QUARKUS_CONFIGURATION, true) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        String name = env.getRunProfile().getName();
                        createQuteConfiguration(indicator, quteDebugPort, name);
                    }
                });
            }
        }
    }

    @Nullable
    private static Integer getQuteDebugPort(String message) {
        try {
            String port = message.substring(QUTE_LISTENING_ON_PORT.length()).trim();
            return Integer.valueOf(port);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void processTerminated(@NotNull ProcessEvent event) {
        event.getProcessHandler().removeProcessListener(this);
    }

    private void createRemoteConfiguration(@NotNull ProgressIndicator indicator, int port, String name) {
        indicator.setText("Connecting Java debugger to port " + port);
        RunnerAndConfigurationSettings settings = RunManager.getInstance(project).createConfiguration(name + " (Remote)", RemoteConfigurationType.class);
        RemoteConfiguration remoteConfiguration = (RemoteConfiguration) settings.getConfiguration();
        remoteConfiguration.PORT = Integer.toString(port);
        long groupId = ExecutionEnvironment.getNextUnusedExecutionId();
        ExecutionUtil.runConfiguration(settings, DefaultDebugExecutor.getDebugExecutorInstance(), DefaultExecutionTarget.INSTANCE, groupId);
    }

    private void createQuteConfiguration(@NotNull ProgressIndicator indicator, int port, String name) {
        createQuteConfiguration(port, name, project, indicator, true);
    }

    public static void createQuteConfiguration(int port,
                                               @NotNull String name,
                                               @NotNull Project project,
                                               @NotNull ProgressIndicator indicator,
                                               boolean waitForPortAvailable) {
        indicator.setText("Connecting Qute debugger to port " + port);
        try {
            if (waitForPortAvailable) {
                waitForPortAvailable(port, indicator);
            }
            RunnerAndConfigurationSettings settings = RunManager.getInstance(project).createConfiguration(name + " (Qute)", QuteConfigurationType.class);
            QuteRunConfiguration quteConfiguration = (QuteRunConfiguration) settings.getConfiguration();
            quteConfiguration.setAttachPort(Integer.toString(port));
            long groupId = ExecutionEnvironment.getNextUnusedExecutionId();
            ExecutionUtil.runConfiguration(settings, DefaultDebugExecutor.getDebugExecutorInstance(), DefaultExecutionTarget.INSTANCE, groupId);
        } catch (IOException e) {
            ApplicationManager.getApplication()
                    .invokeLater(() -> Messages.showErrorDialog("Can' t connector to port " + port, "Quarkus"));
        }
    }

    public static boolean isDebuggerAutoAttach() {
        try {
            return Registry.is("debugger.auto.attach.from.any.console");
        } catch (MissingResourceException e) {
            return false;
        }
    }

    private static void waitForPortAvailable(int port, ProgressIndicator monitor) throws IOException {
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
