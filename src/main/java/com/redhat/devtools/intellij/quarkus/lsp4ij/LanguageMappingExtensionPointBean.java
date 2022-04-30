package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.xmlb.annotations.Attribute;

public class LanguageMappingExtensionPointBean extends AbstractExtensionPointBean {
    public static final ExtensionPointName<LanguageMappingExtensionPointBean> EP_NAME = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.languageMapping");

    @Attribute("id")
    public String id;

    @Attribute("language")
    public String language;

    @Attribute("serverId")
    public String serverId;
}
