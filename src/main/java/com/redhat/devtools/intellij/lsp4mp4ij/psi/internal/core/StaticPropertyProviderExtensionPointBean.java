package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core;

import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.serviceContainer.BaseKeyedLazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;
import com.redhat.devtools.intellij.quarkus.lsp4ij.server.StreamConnectionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StaticPropertyProviderExtensionPointBean extends BaseKeyedLazyInstance<StaticPropertyProvider> {
    public static final ExtensionPointName<StaticPropertyProviderExtensionPointBean> EP_NAME =
            ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.staticPropertyProvider");

    @Attribute("resource")
    public String resource;

    @Attribute("type")
    public String type;

    @Override
    protected @Nullable String getImplementationClassName() {
        return StaticPropertyProvider.class.getName();
    }

    @Override
    public @NotNull StaticPropertyProvider createInstance(@NotNull ComponentManager componentManager, @NotNull PluginDescriptor pluginDescriptor) {
        var provider = new StaticPropertyProvider(resource);
        if (type != null) {
            provider.setType(type);
        }
        return provider;
    }
}
