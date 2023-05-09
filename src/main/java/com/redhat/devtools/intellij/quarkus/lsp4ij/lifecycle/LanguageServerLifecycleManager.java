/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.quarkus.lsp4ij.lifecycle;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageServerWrapper;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Language server lifecycle manager
 */
public class LanguageServerLifecycleManager {

    public static LanguageServerLifecycleManager getInstance(Project project) {
        return ServiceManager.getService(project, LanguageServerLifecycleManager.class);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageServerLifecycleManager.class);//$NON-NLS-1$

    private final Collection<LanguageServerLifecycleListener> listeners;

    private boolean disposed;

    public LanguageServerLifecycleManager() {
        this(new ConcurrentLinkedQueue<>());
    }

    public LanguageServerLifecycleManager(Collection<LanguageServerLifecycleListener> listeners) {
        this.listeners = listeners;
    }

    public void addLanguageServerLifecycleListener(LanguageServerLifecycleListener listener) {
        this.listeners.add(listener);
    }

    public void removeLanguageServerLifecycleListener(LanguageServerLifecycleListener listener) {
        this.listeners.remove(listener);
    }

    public void onStartingProcess(LanguageServerWrapper languageServer) {
        if (isDisposed()) {
            return;
        }
        for (LanguageServerLifecycleListener listener : this.listeners) {
            try {
                listener.handleStartingProcess(languageServer);
            } catch (Exception e) {
                LOGGER.error("Error while handling starting process of the language server '" + languageServer.serverDefinition.id + "'", e);
            }
        }
    }

    public void onStartedProcess(LanguageServerWrapper languageServer, Exception exception) {
        if (isDisposed()) {
            return;
        }
        for (LanguageServerLifecycleListener listener : this.listeners) {
            try {
                listener.handleStartedProcess(languageServer, exception);
            } catch (Exception e) {
                LOGGER.error("Error while handling started process of the language server '" + languageServer.serverDefinition.id + "'", e);
            }
        }
    }

    public void onStartedLanguageServer(LanguageServerWrapper languageServer, Throwable exception) {
        if (isDisposed()) {
            return;
        }
        for (LanguageServerLifecycleListener listener : this.listeners) {
            try {
                listener.handleStartedLanguageServer(languageServer, exception);
            } catch (Exception e) {
                LOGGER.error("Error while handling started the language server '" + languageServer.serverDefinition.id + "'", e);
            }
        }
    }

    public void logLSPMessage(Message message, LanguageServerWrapper languageServer) {
        if (isDisposed()) {
            return;
        }
        for (LanguageServerLifecycleListener listener : this.listeners) {
            try {
                listener.handleLSPMessage(message, languageServer);
            } catch (Exception e) {
                LOGGER.error("Error while handling LSP message of the language server '" + languageServer.serverDefinition.id + "'", e);
            }
        }
    }

    public void onStoppingLanguageServer(LanguageServerWrapper languageServer) {
        if (isDisposed()) {
            return;
        }
        for (LanguageServerLifecycleListener listener : this.listeners) {
            try {
                listener.handleStoppingLanguageServer(languageServer);
            } catch (Exception e) {
                LOGGER.error("Error while handling stopping the language server '" + languageServer.serverDefinition.id + "'", e);
            }
        }

    }

    public void onStoppedLanguageServer(LanguageServerWrapper languageServer, Exception exception) {
        if (isDisposed()) {
            return;
        }
        for (LanguageServerLifecycleListener listener : this.listeners) {
            try {
                listener.handleStoppedLanguageServer(languageServer, exception);
            } catch (Exception e) {
                LOGGER.error("Error while handling stopped the language server '" + languageServer.serverDefinition.id + "'", e);
            }
        }
    }

    public boolean isDisposed() {
        return disposed;
    }

    public void dispose() {
        disposed = true;
        listeners.stream().forEach(LanguageServerLifecycleListener::dispose);
        listeners.clear();
    }
}
