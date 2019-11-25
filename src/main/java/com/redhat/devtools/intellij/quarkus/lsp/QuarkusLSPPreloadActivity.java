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
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.cl.PluginClassLoader;
import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;


public class QuarkusLSPPreloadActivity extends PreloadingActivity {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusLSPPreloadActivity.class);

    @Override
    public void preload(@NotNull ProgressIndicator indicator) {
        hackClassLoader();
        IdeaPluginDescriptor descriptor = PluginManager.getPlugin(PluginId.getId("com.redhat.devtools.intellij.quarkus"));
        File serverPath = new File(descriptor.getPath(), "lib/server/com.redhat.quarkus.ls-0.0.4-SNAPSHOT-uber.jar");
        String javaHome = System.getProperty("java.home");
        LanguageServerDefinition.register(new QuarkusLanguageServerDefinition("properties", javaHome + File.separator + "bin" + File.separator + "java", new String[] { "-jar", serverPath.getAbsolutePath().toString()}));
    }

    private void hackClassLoader() {
        ClassLoader loader = QuarkusLSPPreloadActivity.class.getClassLoader();
        if (loader instanceof PluginClassLoader) {
            try {
                Field parentsField = loader.getClass().getDeclaredField("myParents");
                parentsField.setAccessible(true);
                ClassLoader[] parents = (ClassLoader[]) parentsField.get(loader);
                for(int i=1; i < parents.length;++i) {
                    if (parents[i] instanceof PluginClassLoader && ((PluginClassLoader)parents[i]).getPluginId().getIdString().equals("com.github.gtache.lsp")) {
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
