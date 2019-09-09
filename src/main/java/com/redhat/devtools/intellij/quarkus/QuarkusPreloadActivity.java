package com.redhat.devtools.intellij.quarkus;

import com.github.gtache.lsp.client.languageserver.serverdefinition.ExeLanguageServerDefinition;
import com.github.gtache.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.cl.PluginClassLoader;
import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProgressIndicator;
import com.redhat.devtools.intellij.quarkus.lsp.QuarkusLanguageServerDefinition;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Field;

public class QuarkusPreloadActivity extends PreloadingActivity {
    @Override
    public void preload(@NotNull ProgressIndicator indicator) {
        hackClassLoader();
        IdeaPluginDescriptor descriptor = PluginManager.getPlugin(PluginId.getId("com.redhat.devtools.intellij.quarkus"));
        File serverPath = new File(descriptor.getPath(), "lib/server/com.redhat.quarkus.ls-0.0.1-SNAPSHOT-uber.jar");
        String javaHome = System.getProperty("java.home");
        LanguageServerDefinition.register(new QuarkusLanguageServerDefinition("properties", javaHome + File.separator + "bin" + File.separator + "java", new String[] { "-jar", serverPath.getAbsolutePath().toString()}));
    }

    private void hackClassLoader() {
        ClassLoader loader = QuarkusPreloadActivity.class.getClassLoader();
        if (loader instanceof PluginClassLoader) {
            try {
                Field parentsField = loader.getClass().getDeclaredField("myParents");
                parentsField.setAccessible(true);
                ClassLoader[] parents = (ClassLoader[]) parentsField.get(loader);
                if (parents.length > 1) {
                    ClassLoader first = parents[0];
                    parents[0] = parents[parents.length - 1];
                    parents[parents.length - 1] = first;
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

}
