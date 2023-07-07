package com.redhat.devtools.intellij.lsp4ij;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4ij.server.StreamConnectionProvider;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

    /**
     * This is meant for test code to clear state that might have leaked from other
     * tests. It isn't meant to be used in production code.
     */
    public void clearStartedServers() {
        synchronized (startedServers) {
            startedServers.forEach(LanguageServerWrapper::stop);
            startedServers.clear();
        }
    }

    void shutdownAllDispatchers() {
        startedServers.forEach(LanguageServerWrapper::stopDispatcher);
    }

    /**
     * A bean storing association of a Document/File with a language server.
     */
    public static class LSPDocumentInfo {

        private final @Nonnull
        URI fileUri;
        private final @Nonnull
        Document document;
        private final @Nonnull
        LanguageServerWrapper wrapper;

        private LSPDocumentInfo(@Nonnull URI fileUri, @Nonnull Document document,
                                @Nonnull LanguageServerWrapper wrapper) {
            this.fileUri = fileUri;
            this.document = document;
            this.wrapper = wrapper;
        }

        public @Nonnull
        Document getDocument() {
            return this.document;
        }

        /**
         * TODO consider directly returning a {@link TextDocumentIdentifier}
         *
         * @return
         */
        public @Nonnull
        URI getFileUri() {
            return this.fileUri;
        }

        /**
         * Returns the language server, regardless of if it is initialized.
         *
         * @deprecated use {@link #getInitializedLanguageClient()} instead.
         */
        @Deprecated
        public LanguageServer getLanguageClient() {
            try {
                return this.wrapper.getInitializedServer().get();
            } catch (ExecutionException e) {
                LOGGER.warn(e.getLocalizedMessage(), e);
                return this.wrapper.getServer();
            } catch (InterruptedException e) {
                LOGGER.warn(e.getLocalizedMessage(), e);
                Thread.currentThread().interrupt();
                return this.wrapper.getServer();
            }
        }

        public int getVersion() {
            return wrapper.getVersion(LSPIJUtils.getFile(document));
        }

        public CompletableFuture<LanguageServer> getInitializedLanguageClient() {
            return this.wrapper.getInitializedServer();
        }

        public @Nullable
        ServerCapabilities getCapabilites() {
            return this.wrapper.getServerCapabilities();
        }

        public boolean isActive() {
            return this.wrapper.isActive();
        }
    }


    public @Nonnull
    List<CompletableFuture<LanguageServer>> getInitializedLanguageServers(@Nonnull VirtualFile file,
                                                                          @Nullable Predicate<ServerCapabilities> request) throws IOException {
        synchronized (startedServers) {
            Collection<LanguageServerWrapper> wrappers = getLSWrappers(file, request);
            return wrappers.stream().map(wrapper -> wrapper.getInitializedServer().thenApplyAsync(server -> {
                try {
                    wrapper.connect(file, null);
                } catch (IOException e) {
                    LOGGER.warn(e.getLocalizedMessage(), e);
                }
                return server;
            })).collect(Collectors.toList());
        }
    }

    public void disableLanguageServerContentType(
            @Nonnull ContentTypeToLanguageServerDefinition contentTypeToLSDefinition) {
        Optional<LanguageServerWrapper> result = startedServers.stream()
                .filter(server -> server.serverDefinition.equals(contentTypeToLSDefinition.getValue())).findFirst();
        if (result.isPresent()) {
            Language language = contentTypeToLSDefinition.getKey();
            if (language != null) {
                result.get().disconnectContentType(language);
            }
        }

    }

    public void enableLanguageServerContentType(
            @Nonnull ContentTypeToLanguageServerDefinition contentTypeToLSDefinition,
            @Nonnull Editor[] editors) {
        for (Editor editor : editors) {
            VirtualFile editorFile = LSPIJUtils.getFile(editor.getDocument());
            Language language = contentTypeToLSDefinition.getKey();
            LanguageServersRegistry.LanguageServerDefinition lsDefinition = contentTypeToLSDefinition.getValue();
            Language contentLanguage = LanguageUtil.getLanguageForPsi(project, editorFile);
            if (contentTypeToLSDefinition.isEnabled() && language != null && contentLanguage != null
                    && contentLanguage.isKindOf(language)
                    && lsDefinition != null) {
                try {
                    getInitializedLanguageServer(editorFile, lsDefinition, capabilities -> true);
                } catch (IOException e) {
                    LOGGER.warn(e.getLocalizedMessage(), e);
                }
            }
        }

    }

    /**
     * Get the requested language server instance for the given file. Starts the language server if not already started.
     *
     * @param file
     * @param lsDefinition
     * @param capabilitiesPredicate a predicate to check capabilities
     * @return a LanguageServer for the given file, which is defined with provided server ID and conforms to specified request
     * @deprecated use {@link #getInitializedLanguageServer(IFile, LanguageServerDefinition, Predicate)} instead.
     */
    @Deprecated
    public LanguageServer getLanguageServer(@Nonnull VirtualFile file, @Nonnull LanguageServersRegistry.LanguageServerDefinition lsDefinition,
                                                   Predicate<ServerCapabilities> capabilitiesPredicate)
            throws IOException {
        LanguageServerWrapper wrapper = getLSWrapperForConnection(LSPIJUtils.getProject(file), lsDefinition, LSPIJUtils.toUri(file));
        if (capabilitiesPredicate == null
                || wrapper.getServerCapabilities() == null /* null check is workaround for https://github.com/TypeFox/ls-api/issues/47 */
                || capabilitiesPredicate.test(wrapper.getServerCapabilities())) {
            wrapper.connect(file, null);
            return wrapper.getServer();
        }
        return null;
    }

    /**
     * Get the requested language server instance for the given file. Starts the language server if not already started.
     *
     * @param file
     * @param lsDefinition
     * @param capabilitiesPredicate a predicate to check capabilities
     * @return a LanguageServer for the given file, which is defined with provided server ID and conforms to specified request
     */
    public CompletableFuture<LanguageServer> getInitializedLanguageServer(@Nonnull VirtualFile file,
                                                                                 @Nonnull LanguageServersRegistry.LanguageServerDefinition lsDefinition,
                                                                                 Predicate<ServerCapabilities> capabilitiesPredicate)
            throws IOException {
        LanguageServerWrapper wrapper = getLSWrapperForConnection(LSPIJUtils.getProject(file), lsDefinition, LSPIJUtils.toUri(file));
        if (capabilitiesPredicate == null
                || wrapper.getServerCapabilities() == null /* null check is workaround for https://github.com/TypeFox/ls-api/issues/47 */
                || capabilitiesPredicate.test(wrapper.getServerCapabilities())) {
            wrapper.connect(file, null);
            return wrapper.getInitializedServer();
        }
        return null;
    }

    /**
     * Get the requested language server instance for the given document. Starts the
     * language server if not already started.
     *
     * @param document the document for which the initialized LanguageServer shall be returned
     * @param serverId the ID of the LanguageServer to be returned
     * @param capabilitesPredicate
     *            a predicate to check capabilities
     * @return a LanguageServer for the given file, which is defined with provided
     *         server ID and conforms to specified request. If
     *         {@code capabilitesPredicate} does not test positive for the server's
     *         capabilities, {@code null} is returned.
     */
    public CompletableFuture<LanguageServer> getInitializedLanguageServer(Document document,
                                                                                 LanguageServersRegistry.LanguageServerDefinition lsDefinition, Predicate<ServerCapabilities> capabilitiesPredicate)
            throws IOException {
        URI initialPath = LSPIJUtils.toUri(document);
        LanguageServerWrapper wrapper = getLSWrapperForConnection(document, lsDefinition, initialPath);
        if (capabilitiesComply(wrapper, capabilitiesPredicate)) {
            wrapper.connect(document);
            return wrapper.getInitializedServer();
        }
        return null;
    }

    /**
     * Checks if the given {@code wrapper}'s capabilities comply with the given
     * {@code capabilitiesPredicate}.
     *
     * @param wrapper
     *            the server that's capabilities are tested with
     *            {@code capabilitiesPredicate}
     * @param capabilitiesPredicate
     *            predicate testing the capabilities of {@code wrapper}.
     * @return The result of applying the capabilities of {@code wrapper} to
     *         {@code capabilitiesPredicate}, or {@code false} if
     *         {@code capabilitiesPredicate == null} or
     *         {@code wrapper.getServerCapabilities() == null}
     */
    private static boolean capabilitiesComply(LanguageServerWrapper wrapper,
                                              Predicate<ServerCapabilities> capabilitiesPredicate) {
        return capabilitiesPredicate == null
                || wrapper.getServerCapabilities() == null /* null check is workaround for https://github.com/TypeFox/ls-api/issues/47 */
                || capabilitiesPredicate.test(wrapper.getServerCapabilities());
    }




    /**
     * TODO we need a similar method for generic IDocument (enabling non-IFiles)
     *
     * @param file
     * @param request
     * @return
     * @throws IOException
     * @noreference This method is currently internal and should only be referenced
     * for testing
     */
    @Nonnull
    public Collection<LanguageServerWrapper> getLSWrappers(@Nonnull VirtualFile file,
                                                                  @Nullable Predicate<ServerCapabilities> request) throws IOException {
        LinkedHashSet<LanguageServerWrapper> res = new LinkedHashSet<>();
        Module project = LSPIJUtils.getProject(file);
        if (project == null) {
            return res;
        }

        res.addAll(getMatchingStartedWrappers(file, request));

        // look for running language servers via content-type
        Queue<Language> contentTypes = new LinkedList<>();
        Set<Language> addedContentTypes = new HashSet<>();
        contentTypes.add(LSPIJUtils.getFileLanguage(file, project.getProject()));
        addedContentTypes.addAll(contentTypes);

        while (!contentTypes.isEmpty()) {
            Language contentType = contentTypes.poll();
            if (contentType == null) {
                continue;
            }
            for (ContentTypeToLanguageServerDefinition mapping : LanguageServersRegistry.getInstance().findProviderFor(contentType)) {
                if (mapping != null && mapping.getValue() != null && mapping.isEnabled()) {
                    LanguageServerWrapper wrapper = getLSWrapperForConnection(project, mapping.getValue(), LSPIJUtils.toUri(file));
                    if (request == null
                            || wrapper.getServerCapabilities() == null /* null check is workaround for https://github.com/TypeFox/ls-api/issues/47 */
                            || request.test(wrapper.getServerCapabilities())) {
                        res.add(wrapper);
                    }
                }
            }
        }
        return res;
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
                            return wrapper.isConnectedTo(path) || LanguageServersRegistry.getInstance().matches(document, wrapper.serverDefinition, project);
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
                    final Module fileProject = file != null ? LSPIJUtils.getProject(file) : null;
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

    /**
     * Return existing {@link LanguageServerWrapper} for the given connection. If
     * not found, create a new one with the given connection and register it for
     * this project/content-type.
     *
     * @param project
     * @param serverDefinition
     * @return
     * @throws IOException
     * @Deprecated will be made private soon
     * @noreference will be made private soon
     * @deprecated
     */
    @Deprecated
    public LanguageServerWrapper getLSWrapperForConnection(@Nonnull Module project,
                                                                  @Nonnull LanguageServersRegistry.LanguageServerDefinition serverDefinition) throws IOException {
        return getLSWrapperForConnection(project, serverDefinition, null);
    }

    @Deprecated
    private LanguageServerWrapper getLSWrapperForConnection(@Nonnull Module project,
                                                                   @Nonnull LanguageServersRegistry.LanguageServerDefinition serverDefinition, @Nullable URI initialPath) throws IOException {
        LanguageServerWrapper wrapper = null;

        synchronized (startedServers) {
            for (LanguageServerWrapper startedWrapper : getStartedLSWrappers(project)) {
                if (startedWrapper.serverDefinition.equals(serverDefinition)) {
                    wrapper = startedWrapper;
                    break;
                }
            }
            if (wrapper == null) {
                wrapper = project != null ? new LanguageServerWrapper(project, serverDefinition) :
                        new LanguageServerWrapper(serverDefinition, initialPath);
                wrapper.start();
            }

            startedServers.add(wrapper);
        }
        return wrapper;
    }

    private LanguageServerWrapper getLSWrapperForConnection(Document document,
                                                                   LanguageServersRegistry.LanguageServerDefinition serverDefinition, URI initialPath) throws IOException {
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

    private @Nonnull
    List<LanguageServerWrapper> getStartedLSWrappers(
            @Nonnull Module project) {
        return startedServers.stream().filter(wrapper -> wrapper.canOperate(project))
                .collect(Collectors.toList());
        // TODO multi-root: also return servers which support multi-root?
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
     * Gets list of LS initialized for given project.
     *
     * @param project
     * @param request
     * @return list of Language Servers
     */
    @Nonnull
    public List<LanguageServer> getLanguageServers(@Nonnull Module project,
                                                          Predicate<ServerCapabilities> request) {
        return getLanguageServers(project, request, false);
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
    public List<LanguageServer> getLanguageServers(@Nullable Module project,
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

    protected LanguageServersRegistry.LanguageServerDefinition getLSDefinition(@Nonnull StreamConnectionProvider provider) {
        return providersToLSDefinitions.get(provider);
    }

    @Nonnull
    public List<LSPDocumentInfo> getLSPDocumentInfosFor(@Nonnull Document document, @Nonnull Predicate<ServerCapabilities> capabilityRequest) {
        URI fileUri = LSPIJUtils.toUri(document);
        List<LSPDocumentInfo> res = new ArrayList<>();
        try {
            getLSWrappers(document).stream().filter(wrapper -> wrapper.getServerCapabilities() == null
                    || capabilityRequest.test(wrapper.getServerCapabilities())).forEach(wrapper -> {
                try {
                    wrapper.connect(document);
                } catch (IOException e) {
                    LOGGER.warn(e.getLocalizedMessage(), e);
                }
                res.add(new LSPDocumentInfo(fileUri, document, wrapper));
            });
        } catch (final Exception e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }
        return res;
    }

    @Nonnull
    public CompletableFuture<List<Pair<LanguageServerWrapper, LanguageServer>>> getLanguageServers(@Nonnull Document document,
                                                                                                   Predicate<ServerCapabilities> filter) {
        URI uri = LSPIJUtils.toUri(document);
        if (uri == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        final List<Pair<LanguageServerWrapper, LanguageServer>> res = Collections.synchronizedList(new ArrayList<>());
        try {
            return CompletableFuture.allOf(getLSWrappers(document).stream().map(wrapper ->
                    wrapper.getInitializedServer().thenComposeAsync(server -> {
                        if (server != null && (filter == null || filter.test(wrapper.getServerCapabilities()))) {
                            try {
                                return wrapper.connect(document);
                            } catch (IOException ex) {
                                LOGGER.warn(ex.getLocalizedMessage(), ex);
                            }
                        }
                        return CompletableFuture.completedFuture(null);
                    }).thenAccept(server -> {
                        if (server != null) {
                            res.add(new Pair(wrapper, server));
                        }
                    })).toArray(CompletableFuture[]::new)).thenApply(theVoid -> res);
        } catch (final ProcessCanceledException cancellation) {
            throw cancellation;
        } catch (final Exception e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }
        return CompletableFuture.completedFuture(Collections.emptyList());
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
