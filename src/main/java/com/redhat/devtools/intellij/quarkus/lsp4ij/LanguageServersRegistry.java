package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.lsp4ij.server.StreamConnectionProvider;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LanguageServersRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageServersRegistry.class);

    public abstract static class LanguageServerDefinition {
        public final @Nonnull String id;
        public final @Nonnull String label;
        public final boolean isSingleton;
        public final @Nonnull Map<FileType, String> langugeIdMappings;

        public LanguageServerDefinition(@Nonnull String id, @Nonnull String label, boolean isSingleton) {
            this.id = id;
            this.label = label;
            this.isSingleton = isSingleton;
            this.langugeIdMappings = new ConcurrentHashMap<>();
        }

        public void registerAssociation(@Nonnull FileType contentType, @Nonnull String languageId) {
            this.langugeIdMappings.put(contentType, languageId);
        }

        public abstract StreamConnectionProvider createConnectionProvider();

        public LanguageClientImpl createLanguageClient() {
            return new LanguageClientImpl();
        }

        public Class<? extends LanguageServer> getServerInterface() {
            return LanguageServer.class;
        }

    }

    static class ExtensionLanguageServerDefinition extends LanguageServerDefinition {
        private ServerExtensionPointBean extension;

        public ExtensionLanguageServerDefinition(ServerExtensionPointBean element) {
            super(element.id, element.label, element.singleton);
            this.extension = element;
        }

        @Override
        public StreamConnectionProvider createConnectionProvider() {
            try {
                return (StreamConnectionProvider) extension.instantiate(extension.clazz, ApplicationManager.getApplication().getPicoContainer());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(
                        "Exception occurred while creating an instance of the stream connection provider", e); //$NON-NLS-1$
            }
        }

        @Override
        public LanguageClientImpl createLanguageClient() {
            String clientImpl = extension.clientImpl;
            if (clientImpl != null && !clientImpl.isEmpty()) {
                try {
                    return (LanguageClientImpl) extension.instantiate(clientImpl, ApplicationManager.getApplication().getPicoContainer());
                } catch (ClassNotFoundException e) {
                    LOGGER.error(e.getLocalizedMessage(), e);
                }
            }
            return super.createLanguageClient();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<? extends LanguageServer> getServerInterface() {
            String serverInterface = extension.serverInterface;
            if (serverInterface != null && !serverInterface.isEmpty()) {
                    try {
                        return (Class<? extends LanguageServer>) (Class<?>)extension.findClass(serverInterface);
                    } catch (ClassNotFoundException exception) {
                        LOGGER.error(exception.getLocalizedMessage(), exception);
                    }
                }
            return super.getServerInterface();
            }
        }

    private static LanguageServersRegistry INSTANCE = null;
    public static LanguageServersRegistry getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LanguageServersRegistry();
        }
        return INSTANCE;
    }

    private List<ContentTypeToLanguageServerDefinition> connections = new ArrayList<>();

    private LanguageServersRegistry() {
        initialize();
    }

    private void initialize() {
        Map<String, LanguageServerDefinition> servers = new HashMap<>();
        List<ContentTypeMapping> contentTypes = new ArrayList<>();
        for (ServerExtensionPointBean server : ServerExtensionPointBean.EP_NAME.getExtensions()) {
            if (server.id != null && !server.id.isEmpty()) {
                servers.put(server.id, new ExtensionLanguageServerDefinition(server));
            }
        }
        for (ContentTypeMappingExtensionPointBean extension : ContentTypeMappingExtensionPointBean.EP_NAME.getExtensions()) {
            FileType contentType = FileTypeManager.getInstance().findFileTypeByName(extension.contenType);
            if (contentType != null) {
                contentTypes.add(new ContentTypeMapping(contentType, extension.id, extension.languageId));
            }
        }


        for (ContentTypeMapping mapping : contentTypes) {
            LanguageServerDefinition lsDefinition = servers.get(mapping.languageId);
            if (lsDefinition != null) {
                registerAssociation(mapping.contentType, lsDefinition, mapping.languageId);
            } else {
                LOGGER.warn("server '" + mapping.id + "' not available"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    /**
     * @param contentType
     * @return the {@link LanguageServerDefinition}s <strong>directly</strong> associated to the given content-type.
     * This does <strong>not</strong> include the one that match transitively as per content-type hierarchy
     */
    List<ContentTypeToLanguageServerDefinition> findProviderFor(final @NonNull FileType contentType) {
        return connections.stream()
                .filter(entry -> entry.getKey().equals(contentType))
                .collect(Collectors.toList());
    }


    public void registerAssociation(@Nonnull FileType contentType,
                                    @Nonnull LanguageServerDefinition serverDefinition, @Nullable String languageId) {
        if (languageId != null) {
            serverDefinition.registerAssociation(contentType, languageId);
        }

        connections.add(new ContentTypeToLanguageServerDefinition(contentType, serverDefinition));
    }

    public List<ContentTypeToLanguageServerDefinition> getContentTypeToLSPExtensions() {
        return this.connections.stream().filter(mapping -> mapping.getValue() instanceof ExtensionLanguageServerDefinition).collect(Collectors.toList());
    }

    public @Nullable LanguageServerDefinition getDefinition(@NonNull String languageServerId) {
        for (ContentTypeToLanguageServerDefinition mapping : this.connections) {
            if (mapping.getValue().id.equals(languageServerId)) {
                return mapping.getValue();
            }
        }
        return null;
    }

    /**
     * internal class to capture content-type mappings for language servers
     */
    private static class ContentTypeMapping {

        @Nonnull public final String id;
        @Nonnull public final FileType contentType;
        @Nullable public final String languageId;

        public ContentTypeMapping(@Nonnull FileType contentType, @Nonnull String id, @Nullable String languageId) {
            this.contentType = contentType;
            this.id = id;
            this.languageId = languageId;
        }

    }

    /**
     * @param file
     * @param serverDefinition
     * @return whether the given serverDefinition is suitable for the file
     */
    public boolean matches(@Nonnull VirtualFile file, @NonNull LanguageServerDefinition serverDefinition) {
        return getAvailableLSFor(LSPIJUtils.getFileContentTypes(file)).contains(serverDefinition);
    }

    /**
     * @param document
     * @param serverDefinition
     * @return whether the given serverDefinition is suitable for the file
     */
    public boolean matches(@Nonnull Document document, @Nonnull LanguageServerDefinition serverDefinition) {
        return getAvailableLSFor(LSPIJUtils.getDocumentContentTypes(document)).contains(serverDefinition);
    }


    private Set<LanguageServerDefinition> getAvailableLSFor(Collection<FileType> contentTypes) {
        Set<LanguageServerDefinition> res = new HashSet<>();
        for (ContentTypeToLanguageServerDefinition mapping : this.connections) {
            if (contentTypes.contains(mapping.getKey())) {
                res.add(mapping.getValue());
            }
        }
        return res;
    }




}

