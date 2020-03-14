package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.xmlb.annotations.Attribute;

public class ContentTypeMappingExtensionPointBean extends AbstractExtensionPointBean {public static final ExtensionPointName<ContentTypeMappingExtensionPointBean> EP_NAME = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.contentTypeMapping");

    @Attribute("id")
    public String id;

    @Attribute("contentType")
    public String contenType;

    @Attribute("languageId")
    public String languageId;
}
