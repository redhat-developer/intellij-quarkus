/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 * Fraunhofer FOKUS
 ******************************************************************************/
package com.redhat.devtools.intellij.lsp4ij.command.internal;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4ij.LanguageServersRegistry;
import com.redhat.devtools.intellij.lsp4ij.LanguageServiceAccessor;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.ExecuteCommandOptions;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * This class provides methods to execute {@link Command} instances.
 */
public class CommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutor.class);

    private static final String LSP_COMMAND_CATEGORY_ID = "org.eclipse.lsp4e.commandCategory"; //$NON-NLS-1$
    private static final String LSP_COMMAND_PARAMETER_TYPE_ID = "org.eclipse.lsp4e.commandParameterType"; //$NON-NLS-1$
    private static final String LSP_PATH_PARAMETER_TYPE_ID = "org.eclipse.lsp4e.pathParameterType"; //$NON-NLS-1$

    /**
     * Will execute the given {@code command} either on a language server,
     * supporting the command, or on the client, if an {@link IHandler} is
     * registered for the ID of the command (see {@link LSPCommandHandler}). If
     * {@code command} is {@code null}, then this method will do nothing. If neither
     * the server, nor the client are able to handle the command explicitly, a
     * heuristic method will try to interpret the command locally.
     *
     * @param command
     *            the LSP Command to be executed. If {@code null} this method will
     *            do nothing.
     * @param document
     *            the document for which the command was created
     * @param languageServerId
     *            the ID of the language server for which the {@code command} is
     *            applicable. If {@code null}, the command will not be executed on
     *            the language server.
     */
    public static void executeCommand(Project project, Command command, Document document,
                                      String languageServerId) {
        if (command == null) {
            return;
        }
        if (executeCommandServerSide(project, command, languageServerId, document)) {
            return;
        }
        if (executeCommandClientSide(command, document)) {
            return;
        }
        // tentative fallback
        if (command.getArguments() != null) {
            WorkspaceEdit edit = createWorkspaceEdit(command.getArguments(), document);
            LSPIJUtils.applyWorkspaceEdit(edit);
        }
    }

    private static boolean executeCommandServerSide(Project project, Command command, String languageServerId,
                                                    Document document) {
        if (languageServerId == null) {
            return false;
        }
        LanguageServersRegistry.LanguageServerDefinition languageServerDefinition = LanguageServersRegistry.getInstance()
                .getDefinition(languageServerId);
        if (languageServerDefinition == null) {
            return false;
        }

        try {
            CompletableFuture<LanguageServer> languageServerFuture = getLanguageServerForCommand(project, command, document,
                    languageServerDefinition);
            if (languageServerFuture == null) {
                return false;
            }
            // Server can handle command
            languageServerFuture.thenAcceptAsync(server -> {
                ExecuteCommandParams params = new ExecuteCommandParams();
                params.setCommand(command.getCommand());
                params.setArguments(command.getArguments());
                server.getWorkspaceService().executeCommand(params);
            });
            return true;
        } catch (IOException e) {
            // log and let the code fall through for LSPEclipseUtils to handle
            LOGGER.warn(e.getLocalizedMessage(), e);
            return false;
        }

    }

    private static CompletableFuture<LanguageServer> getLanguageServerForCommand(Project project,
                                                                                 Command command,
                                                                                 Document document, LanguageServersRegistry.LanguageServerDefinition languageServerDefinition) throws IOException {
        CompletableFuture<LanguageServer> languageServerFuture = LanguageServiceAccessor.getInstance(project)
                .getInitializedLanguageServer(document, languageServerDefinition, serverCapabilities -> {
                    ExecuteCommandOptions provider = serverCapabilities.getExecuteCommandProvider();
                    return provider != null && provider.getCommands().contains(command.getCommand());
                });
        return languageServerFuture;
    }

    @SuppressWarnings("unused") // ECJ compiler for some reason thinks handlerService == null is always false
    private static boolean executeCommandClientSide(Command command, Document document) {
        Application workbench = ApplicationManager.getApplication();
        if (workbench == null) {
            return false;
        }
        URI context = LSPIJUtils.toUri(document);
        AnAction parameterizedCommand = createEclipseCoreCommand(command, context, workbench);
        if (parameterizedCommand == null) {
            return false;
        }
        DataContext dataContext = createDataContext(command, context, workbench);
        ActionUtil.invokeAction(parameterizedCommand, dataContext, ActionPlaces.UNKNOWN, null, null);
        return true;
    }

    private static AnAction createEclipseCoreCommand(Command command, URI context,
                                                                 Application workbench) {
        // Usually commands are defined via extension point, but we synthesize one on
        // the fly for the command ID, since we do not want downstream users
        // having to define them.
        String commandId = command.getCommand();
        return ActionManager.getInstance().getAction(commandId);
    }

    private static DataContext createDataContext(Command command, URI context,
                                                        Application workbench) {

        return new DataContext() {
            @Nullable
            @Override
            public Object getData(@NotNull String dataId) {
                if (LSP_COMMAND_PARAMETER_TYPE_ID.equals(dataId)) {
                    return command;
                } else if (LSP_PATH_PARAMETER_TYPE_ID.equals(dataId)) {
                    return context;
                }
                return null;
            }
        };
    }

    // TODO consider using Entry/SimpleEntry instead
    private static final class Pair<K, V> {
        K key;
        V value;

        Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    // this method may be turned public if needed elsewhere
    /**
     * Very empirical and unsafe heuristic to turn unknown command arguments into a
     * workspace edit...
     */
    private static WorkspaceEdit createWorkspaceEdit(List<Object> commandArguments, Document document) {
        WorkspaceEdit res = new WorkspaceEdit();
        Map<String, List<TextEdit>> changes = new HashMap<>();
        res.setChanges(changes);
        URI initialUri = LSPIJUtils.toUri(document);
        Pair<URI, List<TextEdit>> currentEntry = new Pair<>(initialUri, new ArrayList<>());
        commandArguments.stream().flatMap(item -> {
            if (item instanceof List) {
                return ((List<?>) item).stream();
            } else {
                return Collections.singleton(item).stream();
            }
        }).forEach(arg -> {
                if (arg instanceof String) {
                    changes.put(currentEntry.key.toString(), currentEntry.value);
                    VirtualFile resource = LSPIJUtils.findResourceFor((String) arg);
                    if (resource != null) {
                        currentEntry.key = LSPIJUtils.toUri(resource);
                        currentEntry.value = new ArrayList<>();
                    }
                } else if (arg instanceof WorkspaceEdit) {
                    changes.putAll(((WorkspaceEdit) arg).getChanges());
                } else if (arg instanceof TextEdit) {
                    currentEntry.value.add((TextEdit) arg);
                } else if (arg instanceof Map) {
                    Gson gson = new Gson(); // TODO? retrieve the GSon used by LS
                    TextEdit edit = gson.fromJson(gson.toJson(arg), TextEdit.class);
                    if (edit != null) {
                        currentEntry.value.add(edit);
                    }
                } else if (arg instanceof JsonPrimitive) {
                    JsonPrimitive json = (JsonPrimitive) arg;
                    if (json.isString()) {
                        changes.put(currentEntry.key.toString(), currentEntry.value);
                        VirtualFile resource = LSPIJUtils.findResourceFor(json.getAsString());
                        if (resource != null) {
                            currentEntry.key = LSPIJUtils.toUri(resource);
                            currentEntry.value = new ArrayList<>();
                        }
                    }
                } else if (arg instanceof JsonArray) {
                    Gson gson = new Gson(); // TODO? retrieve the GSon used by LS
                    JsonArray array = (JsonArray) arg;
                    array.forEach(elt -> {
                        TextEdit edit = gson.fromJson(gson.toJson(elt), TextEdit.class);
                        if (edit != null) {
                            currentEntry.value.add(edit);
                        }
                    });
                } else if (arg instanceof JsonObject) {
                    Gson gson = new Gson(); // TODO? retrieve the GSon used by LS
                    WorkspaceEdit wEdit = gson.fromJson((JsonObject) arg, WorkspaceEdit.class);
                    Map<String, List<TextEdit>> entries = wEdit.getChanges();
                    if (wEdit != null && !entries.isEmpty()) {
                        changes.putAll(entries);
                    } else {
                        TextEdit edit = gson.fromJson((JsonObject) arg, TextEdit.class);
                        if (edit != null && edit.getRange() != null) {
                            currentEntry.value.add(edit);
                        }
                    }
                }
        });
        if (!currentEntry.value.isEmpty()) {
            changes.put(currentEntry.key.toString(), currentEntry.value);
        }
        return res;
    }
}
