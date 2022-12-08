package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.serviceContainer.BaseKeyedLazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;
import com.redhat.devtools.intellij.quarkus.lsp4ij.server.StreamConnectionProvider;
import org.jetbrains.annotations.Nullable;

public class ServerExtensionPointBean extends BaseKeyedLazyInstance<StreamConnectionProvider> {
    public static final ExtensionPointName<ServerExtensionPointBean> EP_NAME = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.server");

    @Attribute("id")
    public String id;

    @Attribute("label")
    public String label;

    @Attribute("class")
    public String clazz;

    @Attribute("clientImpl")
    public String clientImpl;
    private Class clientClass;

    @Attribute("serverInterface")
    public String serverInterface;
    private Class serverClass;

    @Attribute("singleton")
    public boolean singleton;

    public Class getClientImpl() throws ClassNotFoundException {
        if (clientClass == null) {
            clientClass = getPluginDescriptor().getPluginClassLoader().loadClass(clientImpl);
        }
        return clientClass;
    }

    public Class getServerInterface() throws ClassNotFoundException {
        if (serverClass == null) {
            serverClass = getPluginDescriptor().getPluginClassLoader().loadClass(clientImpl);
        }
        return serverClass;
    }

    @Override
    protected @Nullable String getImplementationClassName() {
        return clazz;
    }
}
