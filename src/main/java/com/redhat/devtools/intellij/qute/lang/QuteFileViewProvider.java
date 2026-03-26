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

import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.MultiplePsiFilesPerDocumentFileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtilBase;
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider;
import com.intellij.psi.tree.IElementType;
import com.redhat.devtools.intellij.qute.lang.psi.QuteElementTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * File view provider for Qute templates.
 * <p>
 * This provider supports template languages embedded in Qute files
 * (e.g. HTML, YAML) and manages the association between the base Qute
 * language and the underlying template data language.
 */
public class QuteFileViewProvider
        extends MultiplePsiFilesPerDocumentFileViewProvider
        implements TemplateLanguageFileViewProvider {

    /**
     * Key used to store and retrieve the template language associated
     * with an injected Qute {@link com.intellij.psi.PsiLiteralExpression}.
     */
    public static final Key<Language> TEMPLATE_LANGUAGE_KEY =
            Key.create("Qute.TemplateLanguage");

    private final @NotNull Language language;
    private final @NotNull Language templateLanguage;

    public QuteFileViewProvider(@NotNull VirtualFile file,
                                @NotNull Language language,
                                @NotNull PsiManager manager,
                                boolean eventSystemEnabled) {
        this(file, language, getTemplateLanguage(file, manager.getProject()), manager, eventSystemEnabled);
    }

    private QuteFileViewProvider(@NotNull VirtualFile file,
                                 @NotNull Language language,
                                 @NotNull Language templateLanguage,
                                 @NotNull PsiManager manager,
                                 boolean eventSystemEnabled) {
        super(manager, file, eventSystemEnabled);
        this.language = language;
        this.templateLanguage = templateLanguage;
    }

    /**
     * Determines the template language associated with the given file.
     * <p>
     * The template language is inferred from the file extension
     * (e.g. HTML, YAML). If no specific language can be determined,
     * {@link QuteLanguage} is used as a fallback.
     * <p>
     * When Qute content is injected into a Java file, the template
     * language is retrieved from the injection host via
     * {@link #TEMPLATE_LANGUAGE_KEY}.
     *
     * @param file    the virtual file
     * @param project the current project
     * @return the resolved template language, or a fallback language
     */
    public static @NotNull Language getTemplateLanguage(@NotNull VirtualFile file,
                                                        @NotNull Project project) {
        @Nullable String fileExtension = file.getExtension();
        FileType fileType = fileExtension != null
                ? FileTypeManager.getInstance().getFileTypeByExtension(fileExtension)
                : null;

        Language language = fileType instanceof LanguageFileType
                ? ((LanguageFileType) fileType).getLanguage()
                : QuteLanguage.INSTANCE;

        if (language.is(JavaLanguage.INSTANCE) && file instanceof VirtualFileWindow) {
            // Injected Qute content inside a Java file:
            // retrieve the template language from the injection host.
            var host = InjectedLanguageReflectionUtil.findInjectionHost(file);
            @Nullable Language templateLanguage =
                    host != null ? host.getUserData(TEMPLATE_LANGUAGE_KEY) : null;
            return templateLanguage != null
                    ? templateLanguage
                    : PlainTextLanguage.INSTANCE;
        }
        return language;
    }

    @Override
    protected PsiFile createFile(@NotNull Language lang) {
        if (lang == getTemplateDataLanguage()) {
            ParserDefinition parserDefinition =
                    LanguageParserDefinitions.INSTANCE.forLanguage(lang);
            if (parserDefinition != null) {
                PsiFile file = parserDefinition.createFile(this);
                if (file instanceof PsiFileImpl) {
                    ((PsiFileImpl) file)
                            .setContentElementType(QuteElementTypes.QUTE_FILE_DATA);
                }
                return file;
            }
        }
        return super.createFile(lang);
    }

    /**
     * @return the base language of this view provider (Qute).
     */
    @Override
    public @NotNull Language getBaseLanguage() {
        return language;
    }

    /**
     * @return all languages handled by this view provider
     * (base Qute language and template data language).
     */
    @Override
    public @NotNull Set<Language> getLanguages() {
        Set<Language> languages = new LinkedHashSet<>();
        languages.add(getBaseLanguage());
        languages.add(getTemplateDataLanguage());
        return languages;
    }

    /**
     * @return the language used for the template data (e.g. HTML, YAML).
     */
    @Override
    public @NotNull Language getTemplateDataLanguage() {
        return templateLanguage;
    }

    @Override
    public IElementType getContentElementType(@NotNull Language language) {
        return language == getTemplateDataLanguage()
                ? QuteElementTypes.QUTE_FILE_DATA
                : null;
    }

    @Override
    protected @NotNull MultiplePsiFilesPerDocumentFileViewProvider cloneInner(
            @NotNull VirtualFile fileCopy) {
        return new QuteFileViewProvider(
                fileCopy,
                getBaseLanguage(),
                getTemplateDataLanguage(),
                getManager(),
                false
        );
    }
}
