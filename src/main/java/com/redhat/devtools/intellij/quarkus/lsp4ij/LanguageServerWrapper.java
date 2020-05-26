package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.AppTopics;
import com.intellij.ProjectTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.redhat.devtools.intellij.quarkus.lsp4ij.server.StreamConnectionProvider;
import com.redhat.devtools.intellij.quarkus.lsp4ij.ui.Messages;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.CodeActionCapabilities;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionKindCapabilities;
import org.eclipse.lsp4j.CodeActionLiteralSupportCapabilities;
import org.eclipse.lsp4j.CodeLensCapabilities;
import org.eclipse.lsp4j.ColorProviderCapabilities;
import org.eclipse.lsp4j.CompletionCapabilities;
import org.eclipse.lsp4j.CompletionItemCapabilities;
import org.eclipse.lsp4j.DefinitionCapabilities;
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.DocumentHighlightCapabilities;
import org.eclipse.lsp4j.DocumentLinkCapabilities;
import org.eclipse.lsp4j.DocumentSymbolCapabilities;
import org.eclipse.lsp4j.ExecuteCommandCapabilities;
import org.eclipse.lsp4j.ExecuteCommandOptions;
import org.eclipse.lsp4j.FailureHandlingKind;
import org.eclipse.lsp4j.FormattingCapabilities;
import org.eclipse.lsp4j.HoverCapabilities;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.RangeFormattingCapabilities;
import org.eclipse.lsp4j.ReferencesCapabilities;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.RenameCapabilities;
import org.eclipse.lsp4j.ResourceOperationKind;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SignatureHelpCapabilities;
import org.eclipse.lsp4j.SymbolCapabilities;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.SymbolKindCapabilities;
import org.eclipse.lsp4j.SynchronizationCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.TypeDefinitionCapabilities;
import org.eclipse.lsp4j.UnregistrationParams;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;
import org.eclipse.lsp4j.WorkspaceEditCapabilities;
import org.eclipse.lsp4j.WorkspaceFoldersChangeEvent;
import org.eclipse.lsp4j.WorkspaceFoldersOptions;
import org.eclipse.lsp4j.WorkspaceServerCapabilities;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LanguageServerWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageServerWrapper.class);//$NON-NLS-1$
    private static final String CLIENT_NAME = "IntelliJ";

    class Listener implements DocumentListener, FileDocumentManagerListener {
        @Override
        public void documentChanged(@NotNull DocumentEvent event) {
            URI uri = LSPIJUtils.toUri(event.getDocument());
            if (uri != null) {
                DocumentContentSynchronizer documentListener = connectedDocuments.get(uri);
                if (documentListener != null && documentListener.getModificationStamp() < event.getOldTimeStamp()) {
                    documentListener.documentSaved(event.getOldTimeStamp());
                }
            }
        }

        @Override
        public void beforeDocumentSaving(@NotNull Document document) {
            /*VirtualFile file = LSPIJUtils.getFile(document);
            URI uri = LSPIJUtils.toUri(file);
            if (uri != null) {
                disconnect(uri);
            }*/
        }
    }

    private Listener fileBufferListener = new Listener();
    private MessageBusConnection messageBusConnection;

    @Nonnull
    public final LanguageServersRegistry.LanguageServerDefinition serverDefinition;
    @Nullable
    protected final Module initialProject;
    @Nonnull
    protected final Set<Module> allWatchedProjects;
    @Nonnull
    protected Map<URI, DocumentContentSynchronizer> connectedDocuments;
    @Nullable
    protected final URI initialPath;

    protected StreamConnectionProvider lspStreamProvider;
    private Future<?> launcherFuture;
    private CompletableFuture<Void> initializeFuture;
    private LanguageServer languageServer;
    private ServerCapabilities serverCapabilities;

    /**
     * Map containing unregistration handlers for dynamic capability registrations.
     */
    private @Nonnull Map<String, Runnable> dynamicRegistrations = new HashMap<>();
    private boolean initiallySupportsWorkspaceFolders = false;

    /* Backwards compatible constructor */
    public LanguageServerWrapper(@Nonnull Module project, @Nonnull LanguageServersRegistry.LanguageServerDefinition serverDefinition) {
        this(project, serverDefinition, null);
    }

    public LanguageServerWrapper(@Nonnull LanguageServersRegistry.LanguageServerDefinition serverDefinition, @Nullable URI initialPath) {
        this(null, serverDefinition, initialPath);
    }

    /** Unified private constructor to set sensible defaults in all cases */
    private LanguageServerWrapper(@Nullable Module project, @Nonnull LanguageServersRegistry.LanguageServerDefinition serverDefinition,
                                  @Nullable URI initialPath) {
        this.initialProject = project;
        this.initialPath = initialPath;
        this.allWatchedProjects = new HashSet<>();
        this.serverDefinition = serverDefinition;
        this.connectedDocuments = new HashMap<>();
    }

    /**
     * Starts a language server and triggers initialization. If language server is
     * started and active, does nothing. If language server is inactive, restart it.
     *
     * @throws IOException
     */
    public synchronized void start() throws IOException {
        Map<URI, Document> filesToReconnect = Collections.emptyMap();
        if (this.languageServer != null) {
            if (isActive()) {
                return;
            } else {
                filesToReconnect = new HashMap<>();
                for (Map.Entry<URI, DocumentContentSynchronizer> entry : this.connectedDocuments.entrySet()) {
                    filesToReconnect.put(entry.getKey(), entry.getValue().getDocument());
                }
                stop();
            }
        }
        try {
            if (LoggingStreamConnectionProviderProxy.shouldLog(serverDefinition.id)) {
                this.lspStreamProvider = new LoggingStreamConnectionProviderProxy(
                        serverDefinition.createConnectionProvider(), serverDefinition.id);
            } else {
                this.lspStreamProvider = serverDefinition.createConnectionProvider();
            }
            this.lspStreamProvider.start();

            LanguageClientImpl client = serverDefinition.createLanguageClient(initialProject.getProject());
            ExecutorService executorService = Executors.newCachedThreadPool();
            final InitializeParams initParams = new InitializeParams();
            initParams.setProcessId(getCurrentProcessId());

            URI rootURI = null;
            Module project = this.initialProject;
            if (project != null) {
                rootURI = LSPIJUtils.toUri(this.initialProject);
                initParams.setRootUri(rootURI.toString());
                initParams.setRootPath(rootURI.getPath());
            } else {
                // This is required due to overzealous static analysis. Dereferencing
                // this.initialPath directly will trigger a "potential null"
                // warning/error. Checking for this.initialPath == null is not
                // enough.
                final URI initialPath = this.initialPath;
                if (initialPath != null) {
                    File projectDirectory = new File(initialPath);
                    if (projectDirectory.isFile()) {
                        projectDirectory = projectDirectory.getParentFile();
                    }
                    initParams.setRootUri(LSPIJUtils.toUri(projectDirectory).toString());
                } else {
                    initParams.setRootUri(LSPIJUtils.toUri(new File("/")).toString()); //$NON-NLS-1$
                }
            }
            Launcher<? extends LanguageServer> launcher = Launcher.createLauncher(client,
                    serverDefinition.getServerInterface(), this.lspStreamProvider.getInputStream(),
                    this.lspStreamProvider.getOutputStream(), executorService, consumer -> (message -> {
                        consumer.consume(message);
                        logMessage(message);
                        URI root = initParams.getRootUri() != null ? URI.create(initParams.getRootUri()) : null;
                        final StreamConnectionProvider currentConnectionProvider = this.lspStreamProvider;
                        if (currentConnectionProvider != null && isActive()) {
                            currentConnectionProvider.handleMessage(message, this.languageServer, root);
                        }
                    }));

            this.languageServer = launcher.getRemoteProxy();
            client.connect(languageServer, this);
            this.launcherFuture = launcher.startListening();

            WorkspaceClientCapabilities workspaceClientCapabilities = new WorkspaceClientCapabilities();
            workspaceClientCapabilities.setApplyEdit(Boolean.TRUE);
            workspaceClientCapabilities.setExecuteCommand(new ExecuteCommandCapabilities(Boolean.TRUE));
            workspaceClientCapabilities.setSymbol(new SymbolCapabilities(Boolean.TRUE));
            workspaceClientCapabilities.setWorkspaceFolders(Boolean.TRUE);
            WorkspaceEditCapabilities editCapabilities = new WorkspaceEditCapabilities();
            editCapabilities.setDocumentChanges(Boolean.TRUE);
            editCapabilities.setResourceOperations(Arrays.asList(ResourceOperationKind.Create,
                    ResourceOperationKind.Delete, ResourceOperationKind.Rename));
            editCapabilities.setFailureHandling(FailureHandlingKind.Undo);
            workspaceClientCapabilities.setWorkspaceEdit(editCapabilities);
            TextDocumentClientCapabilities textDocumentClientCapabilities = new TextDocumentClientCapabilities();
            textDocumentClientCapabilities
                    .setCodeAction(
                            new CodeActionCapabilities(
                                    new CodeActionLiteralSupportCapabilities(
                                            new CodeActionKindCapabilities(Arrays.asList(CodeActionKind.QuickFix,
                                                    CodeActionKind.Refactor, CodeActionKind.RefactorExtract,
                                                    CodeActionKind.RefactorInline, CodeActionKind.RefactorRewrite,
                                                    CodeActionKind.Source, CodeActionKind.SourceOrganizeImports))),
                                    true));
            textDocumentClientCapabilities.setCodeLens(new CodeLensCapabilities());
            textDocumentClientCapabilities.setColorProvider(new ColorProviderCapabilities());
            CompletionItemCapabilities completionItemCapabilities = new CompletionItemCapabilities(Boolean.TRUE);
            completionItemCapabilities.setDocumentationFormat(Arrays.asList(MarkupKind.MARKDOWN, MarkupKind.PLAINTEXT));
            textDocumentClientCapabilities
                    .setCompletion(new CompletionCapabilities(completionItemCapabilities));
            DefinitionCapabilities definitionCapabilities = new DefinitionCapabilities();
            definitionCapabilities.setLinkSupport(Boolean.TRUE);
            textDocumentClientCapabilities.setDefinition(definitionCapabilities);
            TypeDefinitionCapabilities typeDefinitionCapabilities = new TypeDefinitionCapabilities();
            typeDefinitionCapabilities.setLinkSupport(Boolean.TRUE);
            textDocumentClientCapabilities.setTypeDefinition(typeDefinitionCapabilities);
            textDocumentClientCapabilities.setDocumentHighlight(new DocumentHighlightCapabilities());
            textDocumentClientCapabilities.setDocumentLink(new DocumentLinkCapabilities());
            DocumentSymbolCapabilities documentSymbol = new DocumentSymbolCapabilities();
            documentSymbol.setHierarchicalDocumentSymbolSupport(true);
            documentSymbol.setSymbolKind(new SymbolKindCapabilities(Arrays.asList(SymbolKind.Array, SymbolKind.Boolean,
                    SymbolKind.Class, SymbolKind.Constant, SymbolKind.Constructor, SymbolKind.Enum,
                    SymbolKind.EnumMember, SymbolKind.Event, SymbolKind.Field, SymbolKind.File, SymbolKind.Function,
                    SymbolKind.Interface, SymbolKind.Key, SymbolKind.Method, SymbolKind.Module, SymbolKind.Namespace,
                    SymbolKind.Null, SymbolKind.Number, SymbolKind.Object, SymbolKind.Operator, SymbolKind.Package,
                    SymbolKind.Property, SymbolKind.String, SymbolKind.Struct, SymbolKind.TypeParameter,
                    SymbolKind.Variable)));
            textDocumentClientCapabilities.setDocumentSymbol(documentSymbol);
            textDocumentClientCapabilities.setFormatting(new FormattingCapabilities(Boolean.TRUE));
            HoverCapabilities hoverCapabilities = new HoverCapabilities();
            hoverCapabilities.setContentFormat(Arrays.asList(MarkupKind.MARKDOWN, MarkupKind.PLAINTEXT));
            textDocumentClientCapabilities.setHover(hoverCapabilities);
            textDocumentClientCapabilities.setOnTypeFormatting(null); // TODO
            textDocumentClientCapabilities.setRangeFormatting(new RangeFormattingCapabilities());
            textDocumentClientCapabilities.setReferences(new ReferencesCapabilities());
            textDocumentClientCapabilities.setRename(new RenameCapabilities());
            textDocumentClientCapabilities.setSignatureHelp(new SignatureHelpCapabilities());
            textDocumentClientCapabilities
                    .setSynchronization(new SynchronizationCapabilities(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE));
            initParams.setCapabilities(
                    new ClientCapabilities(workspaceClientCapabilities, textDocumentClientCapabilities, lspStreamProvider.getExperimentalFeaturesPOJO()));
            initParams.setClientName(CLIENT_NAME);

            initParams.setInitializationOptions(this.lspStreamProvider.getInitializationOptions(rootURI));
            initParams.setTrace(this.lspStreamProvider.getTrace(rootURI));

            // no then...Async future here as we want this chain of operation to be sequential and
            // "atomic"-ish
            initializeFuture = languageServer.initialize(initParams).thenAccept(res -> {
                serverCapabilities = res.getCapabilities();
                this.initiallySupportsWorkspaceFolders = supportsWorkspaceFolders(serverCapabilities);
            }).thenRun(() -> {
                this.languageServer.initialized(new InitializedParams());
            });

            final Map<URI, Document> toReconnect = filesToReconnect;
            initializeFuture.thenRunAsync(() -> {
                if (this.initialProject != null) {
                    watchProject(this.initialProject, true);
                }
                for (Map.Entry<URI, Document> fileToReconnect : toReconnect.entrySet()) {
                    try {
                        connect(fileToReconnect.getKey(), fileToReconnect.getValue());
                    } catch (IOException e) {
                        LOGGER.error(e.getLocalizedMessage(), e);
                    }
                }
            });
            EditorFactory.getInstance().getEventMulticaster().addDocumentListener(fileBufferListener);
            messageBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
            messageBusConnection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, fileBufferListener);
        } catch (Exception ex) {
            LOGGER.error(ex.getLocalizedMessage(), ex);
            stop();
        }
    }

    private static boolean supportsWorkspaceFolders(ServerCapabilities serverCapabilities) {
        return serverCapabilities != null && serverCapabilities.getWorkspace() != null
                && serverCapabilities.getWorkspace().getWorkspaceFolders() != null
                && Boolean.TRUE.equals(serverCapabilities.getWorkspace().getWorkspaceFolders().getSupported());
    }

    private Integer getCurrentProcessId() {
        String segment = ManagementFactory.getRuntimeMXBean().getName().split("@")[0]; //$NON-NLS-1$
        try {
            return Integer.valueOf(segment);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void logMessage(Message message) {
        if (message instanceof ResponseMessage && ((ResponseMessage) message).getError() != null
                && ((ResponseMessage) message).getId()
                .equals(Integer.toString(ResponseErrorCode.RequestCancelled.getValue()))) {
            ResponseMessage responseMessage = (ResponseMessage) message;
            LOGGER.error("", new ResponseErrorException(responseMessage.getError()));
        } else if (LOGGER.isDebugEnabled()) {
            LOGGER.info(message.getClass().getSimpleName() + '\n' + message.toString());
        }
    }

    /**
     * @return whether the underlying connection to language server is still active
     */
    public boolean isActive() {
        return this.launcherFuture != null && !this.launcherFuture.isDone() && !this.launcherFuture.isCancelled();
    }

    synchronized void stop() {
        if (this.initializeFuture != null) {
            this.initializeFuture.cancel(true);
            this.initializeFuture = null;
        }

        this.serverCapabilities = null;
        this.dynamicRegistrations.clear();

        final Future<?> serverFuture = this.launcherFuture;
        final StreamConnectionProvider provider = this.lspStreamProvider;
        final LanguageServer languageServerInstance = this.languageServer;

        Runnable shutdownKillAndStopFutureAndProvider = () -> {
            if (languageServerInstance != null) {
                CompletableFuture<Object> shutdown = languageServerInstance.shutdown();
                try {
                    shutdown.get(5, TimeUnit.SECONDS);
                }
                catch (Exception e) {
                }
            }

            if (serverFuture != null) {
                serverFuture.cancel(true);
            }

            if (languageServerInstance != null) {
                languageServerInstance.exit();
            }

            if (provider != null) {
                provider.stop();
            }
        };

        CompletableFuture.runAsync(shutdownKillAndStopFutureAndProvider);

        this.launcherFuture = null;
        this.lspStreamProvider = null;

        while (!this.connectedDocuments.isEmpty()) {
            disconnect(this.connectedDocuments.keySet().iterator().next());
        }
        this.languageServer = null;

        EditorFactory.getInstance().getEventMulticaster().removeDocumentListener(fileBufferListener);
        messageBusConnection.disconnect();
    }

    /**
     *
     * @param file
     * @param document
     * @return null if not connection has happened, a future tracking the connection state otherwise
     * @throws IOException
     */
    public @Nullable CompletableFuture<LanguageServer> connect(@Nonnull VirtualFile file, Document document) throws IOException {
        return connect(LSPIJUtils.toUri(file), document);
    }

    /**
     *
     * @param document
     * @return null if not connection has happened, a future tracking the connection state otherwise
     * @throws IOException
     */
    public @Nullable CompletableFuture<LanguageServer> connect(Document document) throws IOException {
        VirtualFile file = LSPIJUtils.getFile(document);

        if (file != null && file.exists()) {
            return connect(file, document);
        } else {
            URI uri = LSPIJUtils.toUri(document);
            if (uri != null) {
                return connect(uri, document);
            }
        }
        return null;
    }

    protected synchronized void watchProject(Module project, boolean isInitializationRootProject) {
        if (this.allWatchedProjects.contains(project)) {
            return;
        }
        if (isInitializationRootProject && !this.allWatchedProjects.isEmpty()) {
            return; // there can be only one root project
        }
        if (!isInitializationRootProject && !supportsWorkspaceFolderCapability()) {
            // multi project and WorkspaceFolder notifications not supported by this server
            // instance
            return;
        }
        this.allWatchedProjects.add(project);
        project.getProject().getMessageBus().connect(project.getProject()).subscribe(ProjectTopics.MODULES, new ModuleListener() {
            @Override
            public void moduleRemoved(@NotNull Project project, @NotNull Module module) {
                unwatchProject(module);
            }
            //TODO: should we handle module rename
        });
        /*project.getWorkspace().addResourceChangeListener(event -> {
            if (project.equals(event.getResource()) && (event.getDelta().getKind() == IResourceDelta.MOVED_FROM
                    || event.getDelta().getKind() == IResourceDelta.REMOVED)) {
                unwatchProject(project);
            }
        }, IResourceChangeEvent.POST_CHANGE);*/
        if (supportsWorkspaceFolderCapability()) {
            WorkspaceFoldersChangeEvent event = new WorkspaceFoldersChangeEvent();
            event.getAdded().add(LSPIJUtils.toWorkspaceFolder(project));
            DidChangeWorkspaceFoldersParams params = new DidChangeWorkspaceFoldersParams();
            params.setEvent(event);
            this.languageServer.getWorkspaceService().didChangeWorkspaceFolders(params);
        }
    }

    private synchronized void unwatchProject(@Nonnull Module project) {
        this.allWatchedProjects.remove(project);
        // TODO? disconnect resources?
        if (supportsWorkspaceFolderCapability()) {
            WorkspaceFoldersChangeEvent event = new WorkspaceFoldersChangeEvent();
            event.getRemoved().add(LSPIJUtils.toWorkspaceFolder(project));
            DidChangeWorkspaceFoldersParams params = new DidChangeWorkspaceFoldersParams();
            params.setEvent(event);
            this.languageServer.getWorkspaceService().didChangeWorkspaceFolders(params);
        }
    }

    /**
     * Check whether this LS is suitable for provided project. Starts the LS if not
     * already started.
     *
     * @return whether this language server can operate on the given project
     * @since 0.5
     */
    public boolean canOperate(Module project) {
        if (project.equals(this.initialProject) || this.allWatchedProjects.contains(project)) {
            return true;
        }

        return serverDefinition.isSingleton || supportsWorkspaceFolderCapability();
    }

    /**
     * @return true, if the server supports multi-root workspaces via workspace
     *         folders
     * @since 0.6
     */
    private boolean supportsWorkspaceFolderCapability() {
        if (this.initializeFuture != null) {
            try {
                this.initializeFuture.get(1, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException e) {
                LOGGER.error(e.getLocalizedMessage(), e);
            } catch (InterruptedException e) {
                LOGGER.error(e.getLocalizedMessage(), e);
                Thread.currentThread().interrupt();
            }
        }
        return initiallySupportsWorkspaceFolders || supportsWorkspaceFolders(serverCapabilities);
    }

    /**
     * To make public when we support non IFiles
     *
     * @return null if not connection has happened, a future that completes when file is initialized otherwise
     * @noreference internal so far
     */
    private CompletableFuture<LanguageServer> connect(@Nonnull URI absolutePath, Document document) throws IOException {
        final URI thePath = absolutePath; // should be useless

        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file != null && file.exists()) {
            watchProject(LSPIJUtils.getProject(file), false);
        }

        if (this.connectedDocuments.containsKey(thePath)) {
            return CompletableFuture.completedFuture(languageServer);
        }
        start();
        if (this.initializeFuture == null) {
            return null;
        }
        if (document == null) {
            VirtualFile docFile = LSPIJUtils.findResourceFor(thePath);
            document = LSPIJUtils.getDocument(docFile);
        }
        if (document == null) {
            return null;
        }
        final Document theDocument = document;
        return initializeFuture.thenComposeAsync(theVoid -> {
            synchronized (connectedDocuments) {
                if (this.connectedDocuments.containsKey(thePath)) {
                    return CompletableFuture.completedFuture(null);
                }
                Either<TextDocumentSyncKind, TextDocumentSyncOptions> syncOptions = initializeFuture == null ? null
                        : this.serverCapabilities.getTextDocumentSync();
                TextDocumentSyncKind syncKind = null;
                if (syncOptions != null) {
                    if (syncOptions.isRight()) {
                        syncKind = syncOptions.getRight().getChange();
                    } else if (syncOptions.isLeft()) {
                        syncKind = syncOptions.getLeft();
                    }
                }
                DocumentContentSynchronizer listener = new DocumentContentSynchronizer(this, theDocument, syncKind);
                theDocument.addDocumentListener(listener);
                LanguageServerWrapper.this.connectedDocuments.put(thePath, listener);
                return listener.didOpenFuture;
            }
        }).thenApply(theVoid -> languageServer);
    }

    public void disconnect(URI path) {
        DocumentContentSynchronizer documentListener = this.connectedDocuments.remove(path);
        if (documentListener != null) {
            documentListener.getDocument().removeDocumentListener(documentListener);
            documentListener.documentClosed();
        }
        if (this.connectedDocuments.isEmpty()) {
            stop();
        }
    }

    public void disconnectContentType(@Nonnull FileType contentType) {
        List<URI> pathsToDisconnect = new ArrayList<>();
        for (URI path : connectedDocuments.keySet()) {
            VirtualFile foundFiles = LSPIJUtils.findResourceFor(path);
            if (foundFiles != null) {
                LSPIJUtils.getFileContentTypes(foundFiles).forEach(type -> {
                    if (type.equals(contentType)) {
                        pathsToDisconnect.add(path);
                    }
                });
            }
        }
        for (URI path : pathsToDisconnect) {
            disconnect(path);
        }
    }

    /**
     * checks if the wrapper is already connected to the document at the given path
     *
     * @noreference test only
     */
    public boolean isConnectedTo(URI location) {
        return connectedDocuments.containsKey(location);
    }

    /**
     * Starts and returns the language server, regardless of if it is initialized.
     * If not in the UI Thread, will wait to return the initialized server.
     *
     * @deprecated use {@link #getInitializedServer()} instead.
     */
    @Deprecated
    @Nullable
    public LanguageServer getServer() {
        CompletableFuture<LanguageServer> languagServerFuture = getInitializedServer();
        if (ApplicationManager.getApplication().isDispatchThread()) { // UI Thread
            return this.languageServer;
        } else {
            return languagServerFuture.join();
        }
    }

    /**
     * Starts the language server and returns a CompletableFuture waiting for the
     * server to be initialized. If done in the UI stream, a job will be created
     * displaying that the server is being initialized
     */
    @Nonnull
    public CompletableFuture<LanguageServer> getInitializedServer() {
        try {
            start();
        } catch (IOException ex) {
            LOGGER.error(ex.getLocalizedMessage(), ex);
        }
        if (initializeFuture != null && !this.initializeFuture.isDone()) {
            /*if (ApplicationManager.getApplication().isDispatchThread()) { // UI Thread
                try {
                    ProgressManager.getInstance().run(new Task.WithResult<Void, Exception>(null, Messages.initializeLanguageServer_job, false) {
                        @Override
                        protected Void compute(@NotNull ProgressIndicator indicator) throws Exception {
                            indicator.setText("Waiting for server " + LanguageServerWrapper.this.serverDefinition.id + " to be started");
                            initializeFuture.join();
                            return null;
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error(e.getLocalizedMessage(), e);
                }
            }*/
            return initializeFuture.thenApply(r -> this.languageServer);
        }
        return CompletableFuture.completedFuture(this.languageServer);
    }

    /**
     * Warning: this is a long running operation
     *
     * @return the server capabilities, or null if initialization job didn't
     *         complete
     */
    @Nullable
    public ServerCapabilities getServerCapabilities() {
        try {
            getInitializedServer().get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            LOGGER.error("LanguageServer not initialized after 10s", e); //$NON-NLS-1$
        } catch (ExecutionException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        } catch (CancellationException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
            Thread.currentThread().interrupt();
        }

        return this.serverCapabilities;
    }

    /**
     * @return The language ID that this wrapper is dealing with if defined in the
     *         content type mapping for the language server
     */
    @Nullable
    public String getLanguageId(FileType[] contentTypes) {
        for (FileType contentType : contentTypes) {
            String languageId = serverDefinition.langugeIdMappings.get(contentType);
            if (languageId != null) {
                return languageId;
            }
        }
        return null;
    }

    void registerCapability(RegistrationParams params) {
        params.getRegistrations().forEach(reg -> {
            if ("workspace/didChangeWorkspaceFolders".equals(reg.getMethod())) { //$NON-NLS-1$
                assert serverCapabilities != null :
                        "Dynamic capability registration failed! Server not yet initialized?"; //$NON-NLS-1$
                if (initiallySupportsWorkspaceFolders) {
                    // Can treat this as a NOP since nothing can disable it dynamically if it was
                    // enabled on initialization.
                } else if (supportsWorkspaceFolders(serverCapabilities)) {
                    LOGGER.warn(
                            "Dynamic registration of 'workspace/didChangeWorkspaceFolders' ignored. It was already enabled before"); //$NON-NLS-1$);
                } else {
                    addRegistration(reg, () -> setWorkspaceFoldersEnablement(false));
                    setWorkspaceFoldersEnablement(true);
                }
            } else if ("workspace/executeCommand".equals(reg.getMethod())) { //$NON-NLS-1$
                Gson gson = new Gson(); // TODO? retrieve the GSon used by LS
                ExecuteCommandOptions executeCommandOptions = gson.fromJson((JsonObject) reg.getRegisterOptions(),
                        ExecuteCommandOptions.class);
                List<String> newCommands = executeCommandOptions.getCommands();
                if (!newCommands.isEmpty()) {
                    addRegistration(reg, () -> unregisterCommands(newCommands));
                    registerCommands(newCommands);
                }
            } else if ("textDocument/formatting".equals(reg.getMethod())) { //$NON-NLS-1$
                final Boolean beforeRegistration = serverCapabilities.getDocumentFormattingProvider();
                serverCapabilities.setDocumentFormattingProvider(Boolean.TRUE);
                addRegistration(reg, () -> serverCapabilities.setDocumentFormattingProvider(beforeRegistration));
            } else if ("textDocument/rangeFormatting".equals(reg.getMethod())) { //$NON-NLS-1$
                final Boolean beforeRegistration = serverCapabilities.getDocumentRangeFormattingProvider();
                serverCapabilities.setDocumentRangeFormattingProvider(Boolean.TRUE);
                addRegistration(reg, () -> serverCapabilities.setDocumentRangeFormattingProvider(beforeRegistration));
            }
        });
    }

    private void addRegistration(@Nonnull Registration reg, @Nonnull Runnable unregistrationHandler) {
        String regId = reg.getId();
        synchronized (dynamicRegistrations) {
            assert !dynamicRegistrations.containsKey(regId):"Registration id is not unique"; //$NON-NLS-1$
            dynamicRegistrations.put(regId, unregistrationHandler);
        }
    }

    synchronized void setWorkspaceFoldersEnablement(boolean enable) {
        if (serverCapabilities == null) {
            this.serverCapabilities = new ServerCapabilities();
        }
        WorkspaceServerCapabilities workspace = serverCapabilities.getWorkspace();
        if (workspace == null) {
            workspace = new WorkspaceServerCapabilities();
            serverCapabilities.setWorkspace(workspace);
        }
        WorkspaceFoldersOptions folders = workspace.getWorkspaceFolders();
        if (folders == null) {
            folders = new WorkspaceFoldersOptions();
            workspace.setWorkspaceFolders(folders);
        }
        folders.setSupported(enable);
    }

    synchronized void registerCommands(List<String> newCommands) {
        ServerCapabilities caps = this.getServerCapabilities();
        if (caps != null) {
            ExecuteCommandOptions commandProvider = caps.getExecuteCommandProvider();
            if (commandProvider == null) {
                commandProvider = new ExecuteCommandOptions(new ArrayList<>());
                caps.setExecuteCommandProvider(commandProvider);
            }
            List<String> existingCommands = commandProvider.getCommands();
            for (String newCmd : newCommands) {
                assert !existingCommands.contains(newCmd):"Command already registered '" + newCmd + "'"; //$NON-NLS-1$ //$NON-NLS-2$
                existingCommands.add(newCmd);
            }
        } else {
            throw new IllegalStateException("Dynamic command registration failed! Server not yet initialized?"); //$NON-NLS-1$
        }
    }

    void unregisterCapability(UnregistrationParams params) {
        params.getUnregisterations().forEach(reg -> {
            String id = reg.getId();
            Runnable unregistrator;
            synchronized (dynamicRegistrations) {
                unregistrator = dynamicRegistrations.get(id);
                dynamicRegistrations.remove(id);
            }
            if (unregistrator != null) {
                unregistrator.run();
            }
        });
    }

    void unregisterCommands(List<String> cmds) {
        ServerCapabilities caps = this.getServerCapabilities();
        if (caps != null) {
            ExecuteCommandOptions commandProvider = caps.getExecuteCommandProvider();
            if (commandProvider != null) {
                List<String> existingCommands = commandProvider.getCommands();
                existingCommands.removeAll(cmds);
            }
        }
    }

    int getVersion(VirtualFile file) {
        if (file != null && LSPIJUtils.toUri(file) != null) {
            DocumentContentSynchronizer documentContentSynchronizer = connectedDocuments.get(LSPIJUtils.toUri(file));
            if (documentContentSynchronizer != null) {
                return documentContentSynchronizer.getVersion();
            }
        }
        return -1;
    }

    public boolean canOperate(@Nonnull Document document) {
        if (this.isConnectedTo(LSPIJUtils.toUri(document))) {
            return true;
        }
        if (this.initialProject == null && this.connectedDocuments.isEmpty()) {
            return true;
        }
        VirtualFile file = LSPIJUtils.getFile(document);
        if (file != null && file.exists() && canOperate(LSPIJUtils.getProject(file))) {
            return true;
        }
        return serverDefinition.isSingleton || supportsWorkspaceFolderCapability();
    }

}
