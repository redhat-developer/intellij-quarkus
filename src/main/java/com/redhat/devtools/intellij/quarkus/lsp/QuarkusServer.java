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
import com.redhat.devtools.intellij.quarkus.TelemetryService;
import com.redhat.devtools.intellij.quarkus.lsp4ij.server.JavaProcessStreamConnectionProvider;
import com.redhat.devtools.intellij.quarkus.lsp4ij.server.ProcessStreamConnectionProvider;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Start the MicroProfile language server process with the Quarkus extension.
 */
public class QuarkusServer extends JavaProcessStreamConnectionProvider {

    private static final String QUARKUS_DEBUG_PORT = "quarkus.debug.port";

    public QuarkusServer() {
        IdeaPluginDescriptor descriptor = PluginManager.getPlugin(PluginId.getId("com.redhat.devtools.intellij.quarkus"));
        File lsp4mpServerPath = new File(descriptor.getPath(), "lib/server/org.eclipse.lsp4mp.ls-uber.jar");
        File quarkusServerPath = new File(descriptor.getPath(), "lib/server/com.redhat.quarkus.ls.jar");
        String debugPort =  System.getProperty(QUARKUS_DEBUG_PORT);

        List<String> commands = createJavaCommands(debugPort);
        commands.add("-jar");
        commands.add(lsp4mpServerPath.getAbsolutePath());
        commands.add("-cp");
        commands.add(quarkusServerPath.getAbsolutePath());
        commands.add("-DrunAsync=true");
        super.setCommands(commands);

        TelemetryService.instance().action(TelemetryService.LSP_PREFIX + "start").send();
    }

    @Override
    public Object getInitializationOptions(URI rootUri) {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> quarkus = new HashMap<>();
        Map<String, Object> tools = new HashMap<>();
        Map<String, Object> trace = new HashMap<>();
        trace.put("server", "verbose");
        tools.put("trace", trace);
        Map<String, Object> codeLens = new HashMap<>();
        codeLens.put("urlCodeLensEnabled", "true");
        tools.put("codeLens", codeLens);
        quarkus.put("tools", tools);
        settings.put("microprofile", quarkus);
        root.put("settings", settings);
        Map<String, Object> extendedClientCapabilities = new HashMap<>();
        Map<String, Object> commands = new HashMap<>();
        Map<String, Object> commandsKind = new HashMap<>();
        commandsKind.put("valueSet", Arrays.asList(/* TODO support "microprofile.command.configuration.update",*/ "microprofile.command.open.uri"));
        commands.put("commandsKind", commandsKind);
        extendedClientCapabilities.put("commands", commands);
        extendedClientCapabilities.put("completion", new HashMap<>());
        extendedClientCapabilities.put("shouldLanguageServerExitOnShutdown", Boolean.TRUE);
        root.put("extendedClientCapabilities", extendedClientCapabilities);
        return root;
    }
}
