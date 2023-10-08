/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.lsp;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.redhat.devtools.intellij.lsp4ij.server.JavaProcessCommandBuilder;
import com.redhat.devtools.intellij.lsp4ij.server.ProcessStreamConnectionProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.settings.UserDefinedMicroProfileSettings;
import com.redhat.devtools.intellij.quarkus.TelemetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Start the MicroProfile language server process with the Quarkus extension.
 */
public class QuarkusServer extends ProcessStreamConnectionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusServer.class);

    private final Project project;

    public QuarkusServer(Project project) {
        this.project = project;
        IdeaPluginDescriptor descriptor = PluginManager.getPlugin(PluginId.getId("com.redhat.devtools.intellij.quarkus"));
        File lsp4mpServerPath = new File(descriptor.getPath(), "lib/server/org.eclipse.lsp4mp.ls-uber.jar");
        File quarkusServerPath = new File(descriptor.getPath(), "lib/server/com.redhat.quarkus.ls.jar");

        List<String> commands = new JavaProcessCommandBuilder(project, "microprofile")
                .setJar(lsp4mpServerPath.getAbsolutePath())
                .setCp(quarkusServerPath.getAbsolutePath())
                .create();
        commands.add("-DrunAsync=true");
        super.setCommands(commands);

        try {
            TelemetryService.instance().action(TelemetryService.LSP_PREFIX + "start").send();
        } catch (Exception e) {
            LOGGER.error("Error while consuming telemetry service", e);
        }
    }

    @Override
    public Object getInitializationOptions(URI rootUri) {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> settings = UserDefinedMicroProfileSettings.getInstance(project).toSettingsForMicroProfileLS();
        root.put("settings", settings);

        Map<String, Object> extendedClientCapabilities = new HashMap<>();
        Map<String, Object> commands = new HashMap<>();
        Map<String, Object> commandsKind = new HashMap<>();
        commandsKind.put("valueSet", Arrays.asList("microprofile.command.configuration.update", "microprofile.command.open.uri"));
        commands.put("commandsKind", commandsKind);
        extendedClientCapabilities.put("commands", commands);
        extendedClientCapabilities.put("completion", new HashMap<>());
        extendedClientCapabilities.put("shouldLanguageServerExitOnShutdown", Boolean.TRUE);
        root.put("extendedClientCapabilities", extendedClientCapabilities);
        return root;
    }

}
