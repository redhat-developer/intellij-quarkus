/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.lsp4ij;

import com.intellij.lang.Language;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4ij.server.StreamConnectionProvider;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Language server accessor.
 */
public class LanguageServiceAccessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageServiceAccessor.class);

    private final Project project;

    public static LanguageServiceAccessor getInstance(Project project) {
        return ServiceManager.getService(project, LanguageServiceAccessor.class);
    }

    private LanguageServiceAccessor(Project project) {
        this.project = project;
    }

    private final Set<LanguageServerWrapper> startedServers = new HashSet<>();
    private Map<StreamConnectionProvider, LanguageServersRegistry.LanguageServerDefinition> providersToLSDefinitions = new HashMap<>();

    @Nonnull
    public CompletableFuture<List<LanguageServerItem>> getLanguageServers(@Nonnull Document document,
                                                                                 Predicate<ServerCapabilities> filter) {
        URI uri = LSPIJUtils.toUri(document);
        if (uri == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        final List<LanguageServerItem> servers = Collections.synchronizedList(new ArrayList<>());
        try {
            return CompletableFuture.allOf(getLSWrappers(document).stream().map(wrapper ->
                    wrapper.getInitializedServer()
                            .thenComposeAsync(server -> {
                                if (server != null && wrapper.isEnabled() && (filter == null || filter.test(wrapper.getServerCapabilities()))) {
                                    try {
                                        return wrapper.connect(document);
                                    } catch (IOException ex) {
                                        LOGGER.warn(ex.getLocalizedMessage(), ex);
                                    }
                                }
                                return CompletableFuture.completedFuture(null);
                            }).thenAccept(server -> {
                                if (server != null) {
                                    servers.add(new LanguageServerItem(server, wrapper));
                                }
                            })).toArray(CompletableFuture[]::new))
                    .thenApply(theVoid -> servers);
        } catch (final ProcessCanceledException cancellation) {
            throw cancellation;
        } catch (final Exception e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }

    /**
     * Return the started servers.
     *
     * @return the started servers.
     */
    public Set<LanguageServerWrapper> getStartedServers() {
        return startedServers;
    }

    public void projectClosing(Project project) {
        // On project closing, we dispose all language servers
        startedServers.forEach(ls -> {
            if (project.equals(ls.getProject())) {
                ls.dispose();
            }
        });
    }

    /**
     * Get the requested language server instance for the given document. Starts the
     * language server if not already started.
     *
     * @param document             the document for which the initialized LanguageServer shall be returned
     * @param serverId             the ID of the LanguageServer to be returned
     * @param capabilitesPredicate a predicate to check capabilities
     * @return a LanguageServer for the given file, which is defined with provided
     * server ID and conforms to specified request. If
     * {@code capabilitesPredicate} does not test positive for the server's
     * capabilities, {@code null} is returned.
     */
    public CompletableFuture<LanguageServer> getInitializedLanguageServer(Document document,
                                                                          LanguageServersRegistry.LanguageServerDefinition lsDefinition, Predicate<ServerCapabilities> capabilitiesPredicate)
            throws IOException {
        URI initialPath = LSPIJUtils.toUri(document);
        LanguageServerWrapper wrapper = getLSWrapperForConnection(document, lsDefinition, initialPath);
        if (wrapper != null && capabilitiesComply(wrapper, capabilitiesPredicate)) {
            wrapper.connect(document);
            return wrapper.getInitializedServer();
        }
        return null;
    }

    /**
     * Checks if the given {@code wrapper}'s capabilities comply with the given
     * {@code capabilitiesPredicate}.
     *
     * @param wrapper               the server that's capabilities are tested with
     *                              {@code capabilitiesPredicate}
     * @param capabilitiesPredicate predicate testing the capabilities of {@code wrapper}.
     * @return The result of applying the capabilities of {@code wrapper} to
     * {@code capabilitiesPredicate}, or {@code false} if
     * {@code capabilitiesPredicate == null} or
     * {@code wrapper.getServerCapabilities() == null}
     */
    private static boolean capabilitiesComply(LanguageServerWrapper wrapper,
                                              Predicate<ServerCapabilities> capabilitiesPredicate) {
        return capabilitiesPredicate == null
                || wrapper.getServerCapabilities() == null /* null check is workaround for https://github.com/TypeFox/ls-api/issues/47 */
                || capabilitiesPredicate.test(wrapper.getServerCapabilities());
    }

    @Nonnull
    private Collection<LanguageServerWrapper> getLSWrappers(@Nonnull Document document) {
        LinkedHashSet<LanguageServerWrapper> res = new LinkedHashSet<>();
        VirtualFile file = LSPIJUtils.getFile(document);
        URI uri = LSPIJUtils.toUri(document);
        if (uri == null) {
            return Collections.emptyList();
        }
        URI path = uri;

        // look for running language servers via content-type
        Queue<Language> contentTypes = new LinkedList<>();
        Set<Language> processedContentTypes = new HashSet<>();
        contentTypes.add(LSPIJUtils.getDocumentLanguage(document, project));

        synchronized (startedServers) {
            // already started compatible servers that fit request
            res.addAll(startedServers.stream()
                    .filter(wrapper -> {
                        try {
                            return wrapper.isEnabled() && (wrapper.isConnectedTo(path) || LanguageServersRegistry.getInstance().matches(document, wrapper.serverDefinition, project));
                        } catch (ProcessCanceledException cancellation) {
                            throw cancellation;
                        } catch (Exception e) {
                            LOGGER.warn(e.getLocalizedMessage(), e);
                            return false;
                        }
                    })
                    .filter(wrapper -> wrapper.canOperate(document))
                    .collect(Collectors.toList()));

            while (!contentTypes.isEmpty()) {
                Language contentType = contentTypes.poll();
                if (contentType == null || processedContentTypes.contains(contentType)) {
                    continue;
                }
                for (ContentTypeToLanguageServerDefinition mapping : LanguageServersRegistry.getInstance()
                        .findProviderFor(contentType)) {
                    if (mapping == null || !mapping.isEnabled()) {
                        continue;
                    }
                    LanguageServersRegistry.LanguageServerDefinition serverDefinition = mapping.getValue();
                    if (serverDefinition == null) {
                        continue;
                    }
                    if (startedServers.stream().anyMatch(wrapper -> wrapper.serverDefinition.equals(serverDefinition)
                            && wrapper.canOperate(document))) {
                        // we already checked a compatible LS with this definition
                        continue;
                    }
                    final Project fileProject = file != null ? LSPIJUtils.getProject(file) : null;
                    if (fileProject != null) {
                        LanguageServerWrapper wrapper = new LanguageServerWrapper(fileProject, serverDefinition);
                        startedServers.add(wrapper);
                        res.add(wrapper);
                    }
                }
                processedContentTypes.add(contentType);
            }
            return res;
        }
    }

    private LanguageServerWrapper getLSWrapperForConnection(Document document,
                                                            LanguageServersRegistry.LanguageServerDefinition serverDefinition, URI initialPath) throws IOException {
        if (!serverDefinition.isEnabled()) {
            // don't return a language server wrapper for the given server definition
            return null;
        }
        LanguageServerWrapper wrapper = null;

        synchronized (startedServers) {
            for (LanguageServerWrapper startedWrapper : getStartedLSWrappers(document)) {
                if (startedWrapper.serverDefinition.equals(serverDefinition)) {
                    wrapper = startedWrapper;
                    break;
                }
            }
            if (wrapper == null) {
                wrapper = new LanguageServerWrapper(serverDefinition, initialPath);
                wrapper.start();
            }

            startedServers.add(wrapper);
        }
        return wrapper;
    }

    private List<LanguageServerWrapper> getStartedLSWrappers(
            Document document) {
        return getStartedLSWrappers(wrapper -> wrapper.canOperate(document));
    }

    private List<LanguageServerWrapper> getStartedLSWrappers(Predicate<LanguageServerWrapper> predicate) {
        return startedServers.stream().filter(predicate)
                .collect(Collectors.toList());
        // TODO multi-root: also return servers which support multi-root?
    }


    private Collection<LanguageServerWrapper> getMatchingStartedWrappers(@Nonnull VirtualFile file,
                                                                         @Nullable Predicate<ServerCapabilities> request) {
        synchronized (startedServers) {
            return startedServers.stream().filter(wrapper -> wrapper.isConnectedTo(LSPIJUtils.toUri(file))
                    || (LanguageServersRegistry.getInstance().matches(file, wrapper.serverDefinition, project)
                    && wrapper.canOperate(LSPIJUtils.getProject(file)))).filter(wrapper -> request == null
                    || (wrapper.getServerCapabilities() == null || request.test(wrapper.getServerCapabilities())))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Gets list of running LS satisfying a capability predicate. This does not
     * start any matching language servers, it returns the already running ones.
     *
     * @param request
     * @return list of Language Servers
     */
    @Nonnull
    public List<LanguageServer> getActiveLanguageServers(Predicate<ServerCapabilities> request) {
        return getLanguageServers(null, request, true);
    }

    /**
     * Gets list of LS initialized for given project
     *
     * @param onlyActiveLS true if this method should return only the already running
     *                     language servers, otherwise previously started language servers
     *                     will be re-activated
     * @return list of Language Servers
     */
    @Nonnull
    public List<LanguageServer> getLanguageServers(@Nullable Project project,
                                                   Predicate<ServerCapabilities> request, boolean onlyActiveLS) {
        List<LanguageServer> serverInfos = new ArrayList<>();
        for (LanguageServerWrapper wrapper : startedServers) {
            if ((!onlyActiveLS || wrapper.isActive()) && (project == null || wrapper.canOperate(project))) {
                @Nullable
                LanguageServer server = wrapper.getServer();
                if (server == null) {
                    continue;
                }
                if (request == null
                        || wrapper.getServerCapabilities() == null /* null check is workaround for https://github.com/TypeFox/ls-api/issues/47 */
                        || request.test(wrapper.getServerCapabilities())) {
                    serverInfos.add(server);
                }
            }
        }
        return serverInfos;
    }

    public boolean checkCapability(LanguageServer languageServer, Predicate<ServerCapabilities> condition) {
        return startedServers.stream().filter(wrapper -> wrapper.isActive() && wrapper.getServer() == languageServer)
                .anyMatch(wrapper -> condition.test(wrapper.getServerCapabilities()));
    }

    public Optional<LanguageServersRegistry.LanguageServerDefinition> resolveServerDefinition(LanguageServer languageServer) {
        synchronized (startedServers) {
            return startedServers.stream().filter(wrapper -> languageServer.equals(wrapper.getServer())).findFirst().map(wrapper -> wrapper.serverDefinition);
        }
    }

}
