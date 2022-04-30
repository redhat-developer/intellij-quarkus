/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.lang;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.MultiplePsiFilesPerDocumentFileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class QuteFileViewProvider extends MultiplePsiFilesPerDocumentFileViewProvider implements TemplateLanguageFileViewProvider {
    private final Language language;
    private final Language templateLanguage;

    public QuteFileViewProvider(VirtualFile file, Language language, Language templateLanguage, PsiManager manager, boolean eventSystemEnabled) {
        super(manager, file, eventSystemEnabled);
        this.language = language;
        this.templateLanguage = templateLanguage;
    }

    protected PsiFile createFile(@NotNull Language lang) {
        if (lang == getTemplateDataLanguage()) {
            final ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(lang);
            if (parserDefinition != null) {
                PsiFile file = parserDefinition.createFile(this);
                if (file instanceof PsiFileImpl) {
                    ((PsiFileImpl) file).setContentElementType(QuteElementTypes.QUTE_FILE_DATA);
                }
                return file;
            }
        }
        return super.createFile(lang);
    }

    @Override
    public @NotNull Language getBaseLanguage() {
        return language;
    }

    @Override
    public @NotNull Set<Language> getLanguages() {
        Set<Language> languages = new LinkedHashSet<>();
        languages.add(getTemplateDataLanguage());
        languages.add(getBaseLanguage());
        return languages;
    }

    @Override
    public @NotNull Language getTemplateDataLanguage() {
        return templateLanguage;
    }

    @Override
    public IElementType getContentElementType(@NotNull Language language) {
        return language == getTemplateDataLanguage()?QuteElementTypes.QUTE_FILE_DATA:null;
    }

    @Override
    protected @NotNull MultiplePsiFilesPerDocumentFileViewProvider cloneInner(@NotNull VirtualFile fileCopy) {
        return new QuteFileViewProvider(fileCopy, getBaseLanguage(), getTemplateDataLanguage(), getManager(), false);
    }
}
