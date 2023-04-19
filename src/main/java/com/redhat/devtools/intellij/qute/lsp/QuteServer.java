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

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.redhat.devtools.intellij.quarkus.TelemetryService;
import com.redhat.devtools.intellij.quarkus.lsp4ij.server.ProcessStreamConnectionProvider;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class QuteServer extends ProcessStreamConnectionProvider {
    public QuteServer() {
        IdeaPluginDescriptor descriptor = PluginManager.getPlugin(PluginId.getId("com.redhat.devtools.intellij.quarkus"));
        File quteServerPath = new File(descriptor.getPath(), "lib/server/com.redhat.qute.ls-uber.jar");
        String javaHome = System.getProperty("java.home");
        setCommands(Arrays.asList(javaHome + File.separator + "bin" + File.separator + "java", "-jar",
                quteServerPath.getAbsolutePath(), "-DrunAsync=true"));
        TelemetryService.instance().action(TelemetryService.LSP_PREFIX + "startQute").send();
    }

    @Override
    public Object getInitializationOptions(URI rootUri) {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> settings = QuteUtils.getQuteSettings();
        Map<String, Object> extendedClientCapabilities = new HashMap<>();
        Map<String, Object> commands = new HashMap<>();
        Map<String, Object> commandsKind = new HashMap<>();
        commandsKind.put("valueSet", Arrays.asList("qute.command.java.definition", /* TODO support "qute.command.configuration.update" , */ "qute.command.open.uri"));
        commands.put("commandsKind", commandsKind);
        extendedClientCapabilities.put("commands", commands);
        extendedClientCapabilities.put("shouldLanguageServerExitOnShutdown", Boolean.TRUE);
        root.put("extendedClientCapabilities", extendedClientCapabilities);
        root.put("settings", settings);
        return root;
    }
}
