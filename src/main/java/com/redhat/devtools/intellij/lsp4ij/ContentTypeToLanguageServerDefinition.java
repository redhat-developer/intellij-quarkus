package com.redhat.devtools.intellij.lsp4ij;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.annotation.Nonnull;
import java.util.AbstractMap;

public class ContentTypeToLanguageServerDefinition extends AbstractMap.SimpleEntry<Language, LanguageServersRegistry.LanguageServerDefinition> {

    private final DocumentMatcher documentMatcher;
    public ContentTypeToLanguageServerDefinition(@Nonnull Language language,
                                                 DocumentMatcher documentMatcher, @Nonnull LanguageServersRegistry.LanguageServerDefinition provider) {
        super(language, provider);
        this.documentMatcher = documentMatcher;
    }

    public boolean isEnabled() {
        return true;
    }

    public boolean matches(Document document, VirtualFile file, Project project) {
        return documentMatcher.match(document, file, project);
    }
}
