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
package com.redhat.devtools.intellij.quarkus.lsp4ij.console.explorer;

import com.intellij.openapi.application.ApplicationManager;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageServerWrapper;
import com.redhat.devtools.intellij.quarkus.lsp4ij.lifecycle.LanguageServerLifecycleListener;
import com.redhat.devtools.intellij.quarkus.lsp4ij.settings.ServerTrace;
import com.redhat.devtools.intellij.quarkus.lsp4ij.settings.UserDefinedLanguageServerSettings;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.messages.Message;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.HashMap;
import java.util.Map;

/**
 * Language server listener to refresh the language server explorer according to the server state and fill the LSP console.
 *
 * @author Angelo ZERR
 */
public class LanguageServerExplorerLifecycleListener implements LanguageServerLifecycleListener {

    private final Map<LanguageServerWrapper, TracingMessageConsumer> tracingPerServer = new HashMap<>(10);

    private boolean disposed;

    private final LanguageServerExplorer explorer;

    public LanguageServerExplorerLifecycleListener(LanguageServerExplorer explorer) {
        this.explorer = explorer;
    }

    @Override
    public void handleStartingProcess(LanguageServerWrapper languageServer) {
        invokeLater(() -> {
            if (isDisposed()) {
                return;
            }
            LanguageServerProcessTreeNode processTreeNode = findLanguageServerProcessTreeNode(languageServer);
            if (processTreeNode != null) {
                processTreeNode.setServerStatus(ServerStatus.startingProcess);
                explorer.selectAndExpand(processTreeNode);
            }
        });
    }

    @Override
    public void handleStartedProcess(LanguageServerWrapper languageServer, Throwable exception) {
        invokeLater(() -> {
            if (isDisposed()) {
                return;
            }
            LanguageServerProcessTreeNode processTreeNode = findLanguageServerProcessTreeNode(languageServer);
            if (processTreeNode != null) {
                processTreeNode.setServerStatus(ServerStatus.startedProcess);
            }
        });
    }

    @Override
    public void handleStartedLanguageServer(LanguageServerWrapper languageServer, Throwable exception) {
        invokeLater(() -> {
            if (explorer.isDisposed()) {
                return;
            }
            LanguageServerProcessTreeNode processTreeNode = findLanguageServerProcessTreeNode(languageServer);
            if (processTreeNode != null) {
                processTreeNode.setServerStatus(ServerStatus.started);
            }
        });
    }

    @Override
    public void handleLSPMessage(Message message, MessageConsumer messageConsumer, LanguageServerWrapper languageServer) {
        if (explorer.isDisposed()) {
            return;
        }
        ServerTrace serverTrace = getServerTrace(languageServer.serverDefinition.id);
        if (serverTrace == ServerTrace.off) {
            return;
        }

        TracingMessageConsumer tracing = getLSPRequestCacheFor(languageServer);
        String log = tracing.log(message, messageConsumer, serverTrace);
        invokeLater(() -> showMessage(languageServer, log));
    }

    private TracingMessageConsumer getLSPRequestCacheFor(LanguageServerWrapper languageServer) {
        TracingMessageConsumer cache = tracingPerServer.get(languageServer);
        if (cache != null) {
            return cache;
        }
        synchronized (tracingPerServer) {
            cache = tracingPerServer.get(languageServer);
            if (cache != null) {
                return cache;
            }
            cache = new TracingMessageConsumer();
            tracingPerServer.put(languageServer, cache);
            return cache;
        }
    }

    @Override
    public void handleStoppingLanguageServer(LanguageServerWrapper languageServer) {
        invokeLater(() -> {
            if (explorer.isDisposed()) {
                return;
            }
            LanguageServerProcessTreeNode processTreeNode = findLanguageServerProcessTreeNode(languageServer);
            if (processTreeNode != null) {
                processTreeNode.setServerStatus(ServerStatus.stopping);
            }
        });
    }

    @Override
    public void handleStoppedLanguageServer(LanguageServerWrapper languageServer, Throwable exception) {
        invokeLater(() -> {
            if (explorer.isDisposed()) {
                return;
            }
            LanguageServerProcessTreeNode processTreeNode = findLanguageServerProcessTreeNode(languageServer);
            if (processTreeNode != null) {
                processTreeNode.setServerStatus(ServerStatus.stopped);
            }
        });
    }

    private static ServerTrace getServerTrace(String languageServerId) {
        ServerTrace serverTrace = null;
        UserDefinedLanguageServerSettings.LanguageServerDefinitionSettings settings = UserDefinedLanguageServerSettings.getInstance().getLanguageServerSettings(languageServerId);
        if (settings != null) {
            serverTrace = settings.getServerTrace();
        }
        return serverTrace != null ? serverTrace : ServerTrace.off;
    }

    private LanguageServerTreeNode findLanguageServerTreeNode(LanguageServerWrapper languageServer) {
        var tree = explorer.getTree();
        DefaultMutableTreeNode top = (DefaultMutableTreeNode) tree.getModel().getRoot();
        for (int i = 0; i < top.getChildCount(); i++) {
            LanguageServerTreeNode node = (LanguageServerTreeNode) top.getChildAt(i);
            if (node.getServerDefinition().equals(languageServer.serverDefinition)) {
                return node;
            }
        }
        return null;
    }

    private LanguageServerProcessTreeNode findLanguageServerProcessTreeNode(LanguageServerWrapper languageServer) {
        LanguageServerTreeNode node = findLanguageServerTreeNode(languageServer);
        if (node == null) {
            return null;
        }
        var processTreeNode = node.getActiveProcessTreeNode();
        if (processTreeNode == null) {
            var treeModel = explorer.getTreeModel();
            processTreeNode = new LanguageServerProcessTreeNode(languageServer, treeModel);
            node.add(processTreeNode);
        }
        return processTreeNode;
    }

    private void showMessage(LanguageServerWrapper languageServer, String message) {
        if (explorer.isDisposed()) {
            return;
        }
        LanguageServerProcessTreeNode processTreeNode = findLanguageServerProcessTreeNode(languageServer);
        if (processTreeNode != null) {
            if (processTreeNode.getServerStatus() == null) {
                processTreeNode.setServerStatus(ServerStatus.started);
                explorer.selectAndExpand(processTreeNode);
            }
            explorer.showMessage(processTreeNode, message);
        }
    }

    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public void dispose() {
        disposed = true;
        tracingPerServer.clear();
    }

    private static void invokeLater(Runnable runnable) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            runnable.run();
        } else {
            ApplicationManager.getApplication().invokeLater(runnable);
        }
    }

}
