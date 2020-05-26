package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.lsp4ij.server.StreamConnectionProvider;
import org.eclipse.lsp4j.ServerCapabilities;
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

    private LanguageServiceAccessor() {
        // this class shouldn't be instantiated
    }

    private static Set<LanguageServerWrapper> startedServers = new HashSet<>();
    private static Map<StreamConnectionProvider, LanguageServersRegistry.LanguageServerDefinition> providersToLSDefinitions = new HashMap<>();

    /**
     * This is meant for test code to clear state that might have leaked from other
     * tests. It isn't meant to be used in production code.
     */
    public static void clearStartedServers() {
        synchronized (startedServers) {
            startedServers.forEach(LanguageServerWrapper::stop);
            startedServers.clear();
        }
    }

    /**
     * A bean storing association of a Document/File with a language server.
     */
    public static class LSPDocumentInfo {

        private final @Nonnull URI fileUri;
        private final @Nonnull Document document;
        private final @Nonnull LanguageServerWrapper wrapper;

        private LSPDocumentInfo(@Nonnull URI fileUri, @Nonnull Document document,
                                @Nonnull LanguageServerWrapper wrapper) {
            this.fileUri = fileUri;
            this.document = document;
            this.wrapper = wrapper;
        }

        public @Nonnull Document getDocument() {
            return this.document;
        }

        /**
         * TODO consider directly returning a {@link TextDocumentIdentifier}
         * @return
         */
        public @Nonnull URI getFileUri() {
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
                LOGGER.error(e.getLocalizedMessage(), e);
                return this.wrapper.getServer();
            } catch (InterruptedException e) {
                LOGGER.error(e.getLocalizedMessage(), e);
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


    public static @Nonnull List<CompletableFuture<LanguageServer>> getInitializedLanguageServers(@Nonnull VirtualFile file,
                                                                                                 @Nullable Predicate<ServerCapabilities> request) throws IOException {
        synchronized (startedServers) {
            Collection<LanguageServerWrapper> wrappers = getLSWrappers(file, request);
            return wrappers.stream().map(wrapper -> wrapper.getInitializedServer().thenApplyAsync(server -> {
                try {
                    wrapper.connect(file, null);
                } catch (IOException e) {
                    LOGGER.error(e.getLocalizedMessage(), e);
                }
                return server;
            })).collect(Collectors.toList());
        }
    }

    public static void disableLanguageServerContentType(
            @Nonnull ContentTypeToLanguageServerDefinition contentTypeToLSDefinition) {
        Optional<LanguageServerWrapper> result = startedServers.stream()
                .filter(server -> server.serverDefinition.equals(contentTypeToLSDefinition.getValue())).findFirst();
        if (result.isPresent()) {
            FileType contentType = contentTypeToLSDefinition.getKey();
            if (contentType != null) {
                result.get().disconnectContentType(contentType);
            }
        }

    }

    public static void enableLanguageServerContentType(
            @Nonnull ContentTypeToLanguageServerDefinition contentTypeToLSDefinition,
            @Nonnull Editor[] editors) {
        for (Editor editor : editors) {
                    VirtualFile editorFile = LSPIJUtils.getFile(editor.getDocument());
                    FileType contentType = contentTypeToLSDefinition.getKey();
                    LanguageServersRegistry.LanguageServerDefinition lsDefinition = contentTypeToLSDefinition.getValue();
                    FileType contentDesc = editorFile.getFileType();
                    if (contentTypeToLSDefinition.isEnabled() && contentType != null && contentDesc != null
                            && contentType.equals(contentDesc)
                            && lsDefinition != null) {
                        try {
                            getInitializedLanguageServer(editorFile, lsDefinition, capabilities -> true);
                        } catch (IOException e) {
                            LOGGER.error(e.getLocalizedMessage(), e);
                        }
                    }
        }

    }

    /**
     * Get the requested language server instance for the given file. Starts the language server if not already started.
     * @param file
     * @param lsDefinition
     * @param capabilitiesPredicate a predicate to check capabilities
     * @return a LanguageServer for the given file, which is defined with provided server ID and conforms to specified request
     * @deprecated use {@link #getInitializedLanguageServer(IFile, LanguageServerDefinition, Predicate)} instead.
     */
    @Deprecated
    public static LanguageServer getLanguageServer(@Nonnull VirtualFile file, @Nonnull LanguageServersRegistry.LanguageServerDefinition lsDefinition,
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
     * @param file
     * @param lsDefinition
     * @param capabilitiesPredicate a predicate to check capabilities
     * @return a LanguageServer for the given file, which is defined with provided server ID and conforms to specified request
     */
    public static CompletableFuture<LanguageServer> getInitializedLanguageServer(@Nonnull VirtualFile file,
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
     * TODO we need a similar method for generic IDocument (enabling non-IFiles)
     *
     * @param file
     * @param request
     * @return
     * @throws IOException
     * @noreference This method is currently internal and should only be referenced
     *              for testing
     */
    @Nonnull
    public static Collection<LanguageServerWrapper> getLSWrappers(@Nonnull VirtualFile file,
                                                                  @Nullable Predicate<ServerCapabilities> request) throws IOException {
        LinkedHashSet<LanguageServerWrapper> res = new LinkedHashSet<>();
        Module project = LSPIJUtils.getProject(file);
        if (project == null) {
            return res;
        }

        res.addAll(getMatchingStartedWrappers(file, request));

        // look for running language servers via content-type
        Queue<FileType> contentTypes = new LinkedList<>();
        Set<FileType> addedContentTypes = new HashSet<>();
        contentTypes.addAll(LSPIJUtils.getFileContentTypes(file));
        addedContentTypes.addAll(contentTypes);

        while (!contentTypes.isEmpty()) {
            FileType contentType = contentTypes.poll();
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
    private static Collection<LanguageServerWrapper> getLSWrappers(@Nonnull Document document) {
        LinkedHashSet<LanguageServerWrapper> res = new LinkedHashSet<>();
        VirtualFile file = LSPIJUtils.getFile(document);
        URI uri = LSPIJUtils.toUri(document);
        if (uri == null) {
            return Collections.emptyList();
        }
        URI path = uri;

        // look for running language servers via content-type
        Queue<FileType> contentTypes = new LinkedList<>();
        Set<FileType> processedContentTypes = new HashSet<>();
        contentTypes.addAll(LSPIJUtils.getDocumentContentTypes(document));

        synchronized (startedServers) {
            // already started compatible servers that fit request
            res.addAll(startedServers.stream()
                    .filter(wrapper -> {
                        try {
                            return wrapper.isConnectedTo(path) || LanguageServersRegistry.getInstance().matches(document, wrapper.serverDefinition);
                        } catch (Exception e) {
                            LOGGER.error(e.getLocalizedMessage(), e);
                            return false;
                        }
                    })
                    .filter(wrapper -> wrapper.canOperate(document))
                    .collect(Collectors.toList()));

            while (!contentTypes.isEmpty()) {
                FileType contentType = contentTypes.poll();
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
                    LanguageServerWrapper wrapper = fileProject != null ? new LanguageServerWrapper(fileProject, serverDefinition) :
                            new LanguageServerWrapper(serverDefinition, path);
                    startedServers.add(wrapper);
                    res.add(wrapper);
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
    public static LanguageServerWrapper getLSWrapperForConnection(@Nonnull Module project,
                                                                  @Nonnull LanguageServersRegistry.LanguageServerDefinition serverDefinition) throws IOException {
        return 	getLSWrapperForConnection(project, serverDefinition, null);
    }

    @Deprecated
    private static LanguageServerWrapper getLSWrapperForConnection(@Nonnull Module project,
                                                                   @Nonnull LanguageServersRegistry.LanguageServerDefinition serverDefinition, @Nullable URI initialPath) throws IOException {
        LanguageServerWrapper wrapper = null;

        synchronized(startedServers) {
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

    private static @Nonnull List<LanguageServerWrapper> getStartedLSWrappers(
            @Nonnull Module project) {
        return startedServers.stream().filter(wrapper -> wrapper.canOperate(project))
                .collect(Collectors.toList());
        // TODO multi-root: also return servers which support multi-root?
    }

    private static Collection<LanguageServerWrapper> getMatchingStartedWrappers(@Nonnull VirtualFile file,
                                                                                @Nullable Predicate<ServerCapabilities> request) {
        synchronized (startedServers) {
            return startedServers.stream().filter(wrapper -> wrapper.isConnectedTo(LSPIJUtils.toUri(file))
                    || (LanguageServersRegistry.getInstance().matches(file, wrapper.serverDefinition)
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
    public static List<LanguageServer> getActiveLanguageServers(Predicate<ServerCapabilities> request) {
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
    public static List<LanguageServer> getLanguageServers(@Nonnull Module project,
                                                          Predicate<ServerCapabilities> request) {
        return getLanguageServers(project, request, false);
    }

    /**
     * Gets list of LS initialized for given project
     *
     * @param onlyActiveLS
     *            true if this method should return only the already running
     *            language servers, otherwise previously started language servers
     *            will be re-activated
     * @return list of Language Servers
     */
    @Nonnull
    public static List<LanguageServer> getLanguageServers(@Nullable Module project,
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

    protected static LanguageServersRegistry.LanguageServerDefinition getLSDefinition(@Nonnull StreamConnectionProvider provider) {
        return providersToLSDefinitions.get(provider);
    }

    @Nonnull public static List<LSPDocumentInfo> getLSPDocumentInfosFor(@Nonnull Document document, @Nonnull Predicate<ServerCapabilities> capabilityRequest) {
        URI fileUri = LSPIJUtils.toUri(document);
        List<LSPDocumentInfo> res = new ArrayList<>();
        try {
            getLSWrappers(document).stream().filter(wrapper -> wrapper.getServerCapabilities() == null
                    || capabilityRequest.test(wrapper.getServerCapabilities())).forEach(wrapper -> {
                try {
                    wrapper.connect(document);
                } catch (IOException e) {
                    LOGGER.error(e.getLocalizedMessage(), e);
                }
                res.add(new LSPDocumentInfo(fileUri, document, wrapper));
            });
        } catch (final Exception e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return res;
    }

    /**
     *
     * @param document
     * @param filter
     * @return
     * @since 0.9
     */
    @Nonnull
    public static CompletableFuture<List<LanguageServer>> getLanguageServers(@Nonnull Document document,
                                                                             Predicate<ServerCapabilities> filter) {
        URI uri = LSPIJUtils.toUri(document);
        if (uri == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        final List<LanguageServer> res = Collections.synchronizedList(new ArrayList<>());
        try {
            return CompletableFuture.allOf(getLSWrappers(document).stream().map(wrapper ->
                    wrapper.getInitializedServer().thenComposeAsync(server -> {
                        if (server != null && (filter == null || filter.test(wrapper.getServerCapabilities()))) {
                            try {
                                return wrapper.connect(document);
                            } catch (IOException ex) {
                                LOGGER.error(ex.getLocalizedMessage(), ex);
                            }
                        }
                        return CompletableFuture.completedFuture(null);
                    }).thenAccept(server -> {
                        if (server != null) {
                            res.add(server);
                        }
                    })).toArray(CompletableFuture[]::new)).thenApply(theVoid -> res);
        } catch (final Exception e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return CompletableFuture.completedFuture(Collections.emptyList());


    }

    public static boolean checkCapability(LanguageServer languageServer, Predicate<ServerCapabilities> condition) {
        return startedServers.stream().filter(wrapper -> wrapper.isActive() && wrapper.getServer() == languageServer)
                .anyMatch(wrapper -> condition.test(wrapper.getServerCapabilities()));
    }

}
