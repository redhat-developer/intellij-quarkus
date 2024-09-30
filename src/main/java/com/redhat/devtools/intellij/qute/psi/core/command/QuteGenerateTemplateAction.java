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
import com.redhat.devtools.lsp4ij.LanguageServerItem;
import com.redhat.devtools.lsp4ij.commands.CommandExecutor;
import com.redhat.devtools.lsp4ij.commands.LSPCommand;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public class QuteGenerateTemplateAction extends QuteAction {

    private static final String TEMPLATE_FILE_URI = "templateFileUri";
    private static final String QUTE_COMMAND_GENERATE_TEMPLATE_CONTENT = "qute.command.generate.template.content";

    private static System.Logger LOGGER = System.getLogger(QuteGenerateTemplateAction.class.getName());

    @Override
    protected void commandPerformed(@NotNull LSPCommand command, @NotNull AnActionEvent e) {
        LanguageServerItem server = e.getDataContext().getData(CommandExecutor.LSP_COMMAND_LANGUAGE_SERVER);
        try {
            if (server != null) {
                URI uri = getURI(command);
                ExecuteCommandParams params = new ExecuteCommandParams();
                params.setCommand(QUTE_COMMAND_GENERATE_TEMPLATE_CONTENT);
                params.setArguments(command.getOriginalArguments());
                server.getWorkspaceService()
                        .executeCommand(params)
                        .thenApply(content -> {
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
                        }).exceptionally(ex -> {
                            LOGGER.log(System.Logger.Level.WARNING, "Error while generating Qute template", ex);
                            return ex;
                        });
            }
        } catch (URISyntaxException ex) {
            LOGGER.log(System.Logger.Level.WARNING, ex.getLocalizedMessage(), ex);
        }
    }

    private URI getURI(LSPCommand command) throws URISyntaxException {
        Object arg = command.getArgumentAt(0);
        if (arg instanceof JsonObject jsonObject) {
            if (jsonObject.has(TEMPLATE_FILE_URI)) {
                return new URI(jsonObject.get(TEMPLATE_FILE_URI).getAsString());
            }
        }
        return null;
    }
}
