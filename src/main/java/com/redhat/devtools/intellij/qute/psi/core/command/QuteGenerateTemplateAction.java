/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.core.command;

import com.google.gson.JsonObject;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4ij.LanguageServiceAccessor;
import com.redhat.devtools.intellij.lsp4ij.commands.CommandExecutor;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.ExecuteCommandOptions;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.services.LanguageServer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class QuteGenerateTemplateAction extends QuteAction {
    private static final String TEMPLATE_FILE_URI = "templateFileUri";
    private static final String QUTE_COMMAND_GENERATE_TEMPLATE_CONTENT = "qute.command.generate.template.content";
    private static System.Logger LOGGER = System.getLogger(QuteGenerateTemplateAction.class.getName());

    private LanguageServer getFirstServer(AnActionEvent e) {
        List<LanguageServer> servers = LanguageServiceAccessor.getInstance(e.getProject()).getActiveLanguageServers(cap -> {
            ExecuteCommandOptions provider = cap.getExecuteCommandProvider();
            return provider != null && provider.getCommands().contains(QUTE_COMMAND_GENERATE_TEMPLATE_CONTENT);
        });
        return servers.isEmpty()?null:servers.get(0);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Command command = e.getData(CommandExecutor.LSP_COMMAND);
        LanguageServer server = getFirstServer(e);
        try {
            if (server != null) {
                URI uri = getURI(command.getArguments());
                ExecuteCommandParams params = new ExecuteCommandParams();
                params.setCommand(QUTE_COMMAND_GENERATE_TEMPLATE_CONTENT);
                params.setArguments(command.getArguments());
                server.getWorkspaceService().executeCommand(params).thenApply(content -> {
                    try {
                        Path path = Path.of(uri);
                        Files.createDirectories(path.getParent());
                        Files.createFile(path);
                        Files.writeString(path, content.toString());
                        VirtualFile f = VfsUtil.findFile(path, true);
                        if (f != null) {
                            ApplicationManager.getApplication().invokeLater(() -> FileEditorManager.getInstance(e.getProject()).openFile(f, true));
                        }
                    } catch (IOException ex) {
                        LOGGER.log(System.Logger.Level.WARNING, ex.getLocalizedMessage(), ex);
                    }
                    return content;
                });
            }
        } catch (URISyntaxException ex) {
            LOGGER.log(System.Logger.Level.WARNING, ex.getLocalizedMessage(), ex);
        }
    }

    private URI getURI(List<Object> arguments) throws URISyntaxException {
        URI uri = null;
        if (!arguments.isEmpty() && arguments.get(0) instanceof JsonObject && ((JsonObject) arguments.get(0)).has(TEMPLATE_FILE_URI)) {
            uri = new URI(((JsonObject) arguments.get(0)).get(TEMPLATE_FILE_URI).getAsString());
        }
        return uri;
    }
}
