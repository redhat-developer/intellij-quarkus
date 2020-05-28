package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.openapi.fileTypes.FileType;

import javax.annotation.Nonnull;
import java.util.AbstractMap;

public class ContentTypeToLanguageServerDefinition extends AbstractMap.SimpleEntry<FileType, LanguageServersRegistry.LanguageServerDefinition> {
    public ContentTypeToLanguageServerDefinition(@Nonnull FileType contentType,
                                                 @Nonnull LanguageServersRegistry.LanguageServerDefinition provider) {
        super(contentType, provider);
    }

    public boolean isEnabled() {
        return true;
    }
}
