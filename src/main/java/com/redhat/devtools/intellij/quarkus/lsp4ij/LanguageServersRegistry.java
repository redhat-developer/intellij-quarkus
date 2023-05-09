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
package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.lsp4ij.server.StreamConnectionProvider;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Language server registry.
 *
 */
public class LanguageServersRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageServersRegistry.class);

    public abstract static class LanguageServerDefinition {

        private static final int DEFAULT_LAST_DOCUMENTED_DISCONNECTED_TIMEOUT = 5;

        public final @Nonnull String id;
        public final @Nonnull String label;
        public final boolean isSingleton;
        public final @Nonnull Map<Language, String> languageIdMappings;
        public final String description;
        public final int lastDocumentDisconnectedTimeout;

        public LanguageServerDefinition(@Nonnull String id, @Nonnull String label, String description, boolean isSingleton, Integer lastDocumentDisconnectedTimeout) {
            this.id = id;
            this.label = label;
            this.description = description;
            this.isSingleton = isSingleton;
            this.lastDocumentDisconnectedTimeout = lastDocumentDisconnectedTimeout != null  && lastDocumentDisconnectedTimeout > 0 ? lastDocumentDisconnectedTimeout : DEFAULT_LAST_DOCUMENTED_DISCONNECTED_TIMEOUT;
            this.languageIdMappings = new ConcurrentHashMap<>();
        }

        public void registerAssociation(@Nonnull Language language, @Nonnull String languageId) {
            this.languageIdMappings.put(language, languageId);
        }

        @Nonnull
        public String getDisplayName() {
            return label != null ? label : id;
        }

        public abstract StreamConnectionProvider createConnectionProvider();

        public LanguageClientImpl createLanguageClient(Project project) {
            return new LanguageClientImpl(project);
        }

        public Class<? extends LanguageServer> getServerInterface() {
            return LanguageServer.class;
        }

        public <S extends LanguageServer> Launcher.Builder<S> createLauncherBuilder() {
            return new Launcher.Builder<>();
        }
    }

    static class ExtensionLanguageServerDefinition extends LanguageServerDefinition {
        private final ServerExtensionPointBean extension;

        public ExtensionLanguageServerDefinition(ServerExtensionPointBean element) {
            super(element.id, element.label, element.description, element.singleton, element.lastDocumentDisconnectedTimeout);
            this.extension = element;
        }

        @Override
        public StreamConnectionProvider createConnectionProvider() {
            try {
                return extension.getInstance();
            } catch (Exception e) {
                throw new RuntimeException(
                        "Exception occurred while creating an instance of the stream connection provider", e); //$NON-NLS-1$
            }
        }

        @Override
        public LanguageClientImpl createLanguageClient(Project project) {
            String clientImpl = extension.clientImpl;
            if (clientImpl != null && !clientImpl.isEmpty()) {
                try {
                    return (LanguageClientImpl) project.instantiateClass(extension.getClientImpl(),
                            extension.getPluginDescriptor().getPluginId());
                } catch (ClassNotFoundException e) {
                    LOGGER.warn(e.getLocalizedMessage(), e);
                }
            }
            return super.createLanguageClient(project);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<? extends LanguageServer> getServerInterface() {
            String serverInterface = extension.serverInterface;
            if (serverInterface != null && !serverInterface.isEmpty()) {
                    try {
                        return (Class<? extends LanguageServer>) (Class<?>)extension.getServerInterface();
                    } catch (ClassNotFoundException exception) {
                        LOGGER.warn(exception.getLocalizedMessage(), exception);
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

    private final List<ContentTypeToLanguageServerDefinition> connections = new ArrayList<>();

    private Map<String, LanguageServerIconProviderDefinition> serverIcons = new HashMap<>();

    private LanguageServersRegistry() {
        initialize();
    }

    private void initialize() {
        Map<String, LanguageServerDefinition> servers = new HashMap<>();
        List<LanguageMapping> languageMappings = new ArrayList<>();
        for (ServerExtensionPointBean server : ServerExtensionPointBean.EP_NAME.getExtensions()) {
            if (server.id != null && !server.id.isEmpty()) {
                servers.put(server.id, new ExtensionLanguageServerDefinition(server));
            }
        }
        for (LanguageMappingExtensionPointBean extension : LanguageMappingExtensionPointBean.EP_NAME.getExtensions()) {
            Language language = Language.findLanguageByID(extension.language);
            if (language != null) {
                languageMappings.add(new LanguageMapping(language, extension.id, extension.serverId));
            }
        }

        for (ServerIconProviderExtensionPointBean extension : ServerIconProviderExtensionPointBean.EP_NAME.getExtensions()) {
            serverIcons.put(extension.serverId, new LanguageServerIconProviderDefinition(extension));
        }

        for (LanguageMapping mapping : languageMappings) {
            LanguageServerDefinition lsDefinition = servers.get(mapping.languageId);
            if (lsDefinition != null) {
                registerAssociation(mapping.language, lsDefinition, mapping.languageId);
            } else {
                LOGGER.warn("server '" + mapping.id + "' not available"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    public Icon getServerIcon(String serverId) {
        LanguageServerIconProviderDefinition iconProvider = serverIcons.get(serverId);
        return iconProvider != null ? iconProvider.getIcon() : null;
    }

    /**
     * @param contentType
     * @return the {@link LanguageServerDefinition}s <strong>directly</strong> associated to the given content-type.
     * This does <strong>not</strong> include the one that match transitively as per content-type hierarchy
     */
    List<ContentTypeToLanguageServerDefinition> findProviderFor(final @NonNull Language contentType) {
        return connections.stream()
                .filter(entry -> contentType.isKindOf(entry.getKey()))
                .collect(Collectors.toList());
    }


    public void registerAssociation(@Nonnull Language language,
                                    @Nonnull LanguageServerDefinition serverDefinition, @Nullable String languageId) {
        if (languageId != null) {
            serverDefinition.registerAssociation(language, languageId);
        }

        connections.add(new ContentTypeToLanguageServerDefinition(language, serverDefinition));
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
    private static class LanguageMapping {

        @Nonnull public final String id;
        @Nonnull public final Language language;
        @Nullable public final String languageId;

        public LanguageMapping(@Nonnull Language language, @Nonnull String id, @Nullable String languageId) {
            this.language = language;
            this.id = id;
            this.languageId = languageId;
        }

    }

    /**
     * @param file
     * @param serverDefinition
     * @return whether the given serverDefinition is suitable for the file
     */
    public boolean matches(@Nonnull VirtualFile file, @NonNull LanguageServerDefinition serverDefinition,
                           Project project) {
        return getAvailableLSFor(LSPIJUtils.getFileLanguage(file, project)).contains(serverDefinition);
    }

    /**
     * @param document
     * @param serverDefinition
     * @return whether the given serverDefinition is suitable for the file
     */
    public boolean matches(@Nonnull Document document, @Nonnull LanguageServerDefinition serverDefinition,
                           Project project) {
        return getAvailableLSFor(LSPIJUtils.getDocumentLanguage(document, project)).contains(serverDefinition);
    }


    private Set<LanguageServerDefinition> getAvailableLSFor(Language language) {
        Set<LanguageServerDefinition> res = new HashSet<>();
        if (language != null) {
            for (ContentTypeToLanguageServerDefinition mapping : this.connections) {
                if (language.isKindOf(mapping.getKey())) {
                    res.add(mapping.getValue());
                }
            }
        }
        return res;
    }

    public Set<LanguageServerDefinition> getAllDefinitions() {
        return connections
                .stream()
                .map(definition -> definition.getValue())
                .collect(Collectors.toSet());
    }

}

