/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.lsp;

import com.github.gtache.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import com.github.gtache.lsp.requests.Timeout;
import com.github.gtache.lsp.requests.Timeouts;
import com.github.gtache.lsp.settings.LSPState;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.cl.PluginClassLoader;
import com.intellij.openapi.components.BaseComponent;
import com.intellij.openapi.extensions.PluginId;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class QuarkusLSPInitializer implements BaseComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusLSPInitializer.class);

    @Override
    public void initComponent() {
        hackClassLoader();
        IdeaPluginDescriptor descriptor = PluginManager.getPlugin(PluginId.getId("com.redhat.devtools.intellij.quarkus"));
        File serverPath = new File(descriptor.getPath(), "lib/server/com.redhat.quarkus.ls-0.0.4-SNAPSHOT-uber.jar");
        String javaHome = System.getProperty("java.home");
        LanguageServerDefinition.register(new QuarkusLanguageServerDefinition("properties", javaHome + File.separator + "bin" + File.separator + "java", new String[] { "-jar", serverPath.getAbsolutePath().toString()}));
        updateCompletionTimeout();
    }

    private void updateCompletionTimeout() {
        Map<Timeouts, Integer> timeouts = new HashMap<>(Timeout.getTimeoutsJava());
        timeouts.putAll(LSPState.getInstance().getTimeouts());
        Integer completionTimeout = timeouts.get(Timeouts.COMPLETION);
        if (completionTimeout == null || completionTimeout < 200000) {
            timeouts.put(Timeouts.COMPLETION, 200000);
        }
        LSPState.getInstance().setTimeouts(timeouts);
        Timeout.setTimeouts(timeouts);
    }

    private void hackClassLoader() {
        ClassLoader loader = QuarkusLSPInitializer.class.getClassLoader();
        if (loader instanceof PluginClassLoader) {
            try {
                Field parentsField = loader.getClass().getDeclaredField("myParents");
                parentsField.setAccessible(true);
                ClassLoader[] parents = (ClassLoader[]) parentsField.get(loader);
                for(int i=0; i < parents.length;++i) {
                    if (parents[i] instanceof PluginClassLoader && ((PluginClassLoader)parents[i]).getPluginId().equals(PluginId.getId(QuarkusConstants.LSP_PLUGIN_ID))) {
                        ClassLoader first = parents[0];
                        parents[0] = parents[i];
                        parents[i] = first;
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
        }
    }
}
