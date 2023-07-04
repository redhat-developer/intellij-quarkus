package com.redhat.devtools.intellij.lsp4ij;

import com.intellij.lang.Language;

import javax.annotation.Nonnull;
import java.util.AbstractMap;

public class ContentTypeToLanguageServerDefinition extends AbstractMap.SimpleEntry<Language, LanguageServersRegistry.LanguageServerDefinition> {
    public ContentTypeToLanguageServerDefinition(@Nonnull Language language,
                                                 @Nonnull LanguageServersRegistry.LanguageServerDefinition provider) {
        super(language, provider);
    }

    public boolean isEnabled() {
        return true;
    }
}
