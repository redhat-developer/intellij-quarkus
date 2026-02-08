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
package com.redhat.devtools.intellij.qute.lsp;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.telemetry.TelemetryEventName;
import com.redhat.devtools.intellij.quarkus.telemetry.TelemetryManager;
import com.redhat.devtools.intellij.qute.psi.QuteCommandConstants;
import com.redhat.devtools.lsp4ij.server.JavaProcessCommandBuilder;
import com.redhat.devtools.lsp4ij.server.OSProcessStreamConnectionProvider;
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider;
import com.redhat.devtools.intellij.qute.settings.UserDefinedQuteSettings;
import com.redhat.qute.services.commands.QuteClientCommandConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Start the Qute language server process.
 */
public class QuteServer extends OSProcessStreamConnectionProvider {

    private final Project project;

    public QuteServer(Project project) {
        this.project = project;
        IdeaPluginDescriptor descriptor = PluginManagerCore.getPlugin(PluginId.getId("com.redhat.devtools.intellij.quarkus"));
        assert descriptor != null;
        Path pluginPath = descriptor.getPluginPath();
        assert pluginPath != null;
        Path quteServerPath = pluginPath.toAbsolutePath().resolve("lib/server/com.redhat.qute.ls-uber.jar");

        List<String> commands = new JavaProcessCommandBuilder(project, "qute")
                .setJar(quteServerPath.toString())
                .create();
        commands.add("-DrunAsync=true");
        super.setCommandLine(new GeneralCommandLine(commands));

        // Send "ls-startQute" telemetry event
        TelemetryManager.instance().send(TelemetryEventName.LSP_START_QUTE_SERVER);
    }

    @Override
    public Object getInitializationOptions(VirtualFile rootUri) {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> settings = UserDefinedQuteSettings.getInstance(project).toSettingsForQuteLS();
        Map<String, Object> extendedClientCapabilities = new HashMap<>();
        Map<String, Object> commands = new HashMap<>();
        Map<String, Object> commandsKind = new HashMap<>();
        commandsKind.put("valueSet", Arrays.asList(
                QuteClientCommandConstants.COMMAND_JAVA_DEFINITION,
                QuteClientCommandConstants.COMMAND_CONFIGURATION_UPDATE,
                QuteClientCommandConstants.COMMAND_OPEN_URI,
                QuteClientCommandConstants.COMMAND_EDITOR_ACTION_TRIGGET_SUGGEST));
        commands.put("commandsKind", commandsKind);
        extendedClientCapabilities.put("commands", commands);
        extendedClientCapabilities.put("shouldLanguageServerExitOnShutdown", Boolean.TRUE);
        root.put("extendedClientCapabilities", extendedClientCapabilities);
        root.put("settings", settings);
        return root;
    }
}
