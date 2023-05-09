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
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.jsonrpc.messages.NotificationMessage;
import org.eclipse.lsp4j.jsonrpc.messages.RequestMessage;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;

import javax.swing.tree.DefaultMutableTreeNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Language server listener to refresh the language server explorer according to the server state and fill the LSP console.
 *
 * @author Angelo ZERR
 */
public class LanguageServerExplorerLifecycleListener implements LanguageServerLifecycleListener {

    private static final DateTimeFormatter dateTracePattern = DateTimeFormatter.ofPattern("hh:mm:ss a");

    private boolean disposed;

    private static class LSPRequestInfo {

        public final String method;

        public final long startTime;

        public LSPRequestInfo(String method, long startTime) {
            this.method = method;
            this.startTime = startTime;
        }
    }

    private final Map<LanguageServerWrapper, Map<String, LSPRequestInfo>> pendingRequests = new ConcurrentHashMap<>(10);

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
    public void handleLSPMessage(Message message, LanguageServerWrapper languageServer) {
        if (explorer.isDisposed()) {
            return;
        }
        ServerTrace serverTrace = getServerTrace(languageServer.serverDefinition.id);
        if (serverTrace == ServerTrace.off) {
            return;
        }

        StringBuilder formattedMessage = new StringBuilder();
        fillHeaderTrace(formattedMessage);
        if (message instanceof RequestMessage) {
            // [Trace - 12:27:33 AM] Sending request 'initialize - (0)'.
            //  Params: {
            String id = ((RequestMessage) message).getId();
            String method = ((RequestMessage) message).getMethod();
            registerLSPRequest(id, new LSPRequestInfo(method, System.currentTimeMillis()), languageServer);
            formattedMessage.append(" Sending request '")
                    .append(method)
                    .append(" - (")
                    .append(id)
                    .append(")'.");
        } else if (message instanceof ResponseMessage) {
            // [Trace - 12:27:35 AM] Received response 'initialize - (0)' in 1921ms.
            String id = ((ResponseMessage) message).getId();
            LSPRequestInfo requestInfo = unregisterLSPRequest(id, languageServer);
            String method = requestInfo != null ? requestInfo.method : "<unknown>";
            formattedMessage.append(" Received response '")
                    .append(method)
                    .append(" - (")
                    .append(id)
                    .append(")'");
            if (requestInfo != null) {
                formattedMessage.append(" in ");
                formattedMessage.append(System.currentTimeMillis() - requestInfo.startTime);
                formattedMessage.append("ms");
            }
            formattedMessage.append(".");
        } else if (message instanceof NotificationMessage) {
            // [Trace - 12:27:35 AM] Sending notification 'initialized'.
            String method = ((NotificationMessage) message).getMethod();
            formattedMessage.append(" Sending notification '")
                    .append(method)
                    .append("'.");
        }
        if (serverTrace == ServerTrace.verbose) {
            formattedMessage.append("\n");
            formattedMessage.append(message.toString());
            formattedMessage.append("\n");
        }
        formattedMessage.append("\n");

        invokeLater(() -> showMessage(languageServer, formattedMessage.toString()));
    }

    private static void fillHeaderTrace(StringBuilder formattedMessage) {
        LocalDateTime datetime = LocalDateTime.now();
        String dateAsString = datetime.format(dateTracePattern);
        formattedMessage.append("[Trace")
                .append(" - ")
                .append(dateAsString)
                .append("]");
    }

    private void registerLSPRequest(String id, LSPRequestInfo lspRequestInfo, LanguageServerWrapper languageServer) {
        Map<String, LSPRequestInfo> cache = getLSPRequestCacheFor(languageServer);
        synchronized (cache) {
            cache.put(id, lspRequestInfo);
        }
    }

    private LSPRequestInfo unregisterLSPRequest(String id, LanguageServerWrapper languageServer) {
        Map<String, LSPRequestInfo> cache = getLSPRequestCacheFor(languageServer);
        synchronized (cache) {
            return cache.remove(id);
        }
    }

    private Map<String, LSPRequestInfo> getLSPRequestCacheFor(LanguageServerWrapper languageServer) {
        Map<String, LSPRequestInfo> cache = pendingRequests.get(languageServer);
        if (cache != null) {
            return cache;
        }
        synchronized (pendingRequests) {
            cache = pendingRequests.get(languageServer);
            if (cache != null) {
                return cache;
            }
            cache = new HashMap<>();
            pendingRequests.put(languageServer, cache);
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
        pendingRequests.clear();
    }

    private static void invokeLater(Runnable runnable) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            runnable.run();
        } else {
            ApplicationManager.getApplication().invokeLater(runnable);
        }
    }

}
