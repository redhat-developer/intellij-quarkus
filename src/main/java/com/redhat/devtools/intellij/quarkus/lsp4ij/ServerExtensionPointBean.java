package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.xmlb.annotations.Attribute;

public class ServerExtensionPointBean extends AbstractExtensionPointBean {
    public static final ExtensionPointName<ServerExtensionPointBean> EP_NAME = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.server");

    @Attribute("id")
    public String id;

    @Attribute("label")
    public String label;

    @Attribute("class")
    public String clazz;

    @Attribute("clientImpl")
    public String clientImpl;

    @Attribute("serverInterface")
    public String serverInterface;

    @Attribute("singleton")
    public boolean singleton;

    public Class getClientImpl() throws ClassNotFoundException {
        return findClass(clientImpl);
    }

    public Class getServerInterface() throws ClassNotFoundException {
        return findClass(serverInterface);
    }
}
