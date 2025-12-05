/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.lang.injector;

import com.intellij.lang.Language;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants;
import org.eclipse.lsp4mp.commons.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of Java annotations that embed Qute template content.
 * <p>
 * Provides access to {@link QuteJavaInjectionDescriptor} for each annotation,
 * including logic for determining the template data language (HTML, text, XML, etc.).
 */
public class QuteJavaInjectionRegistry {

    private static final QuteJavaInjectionRegistry INSTANCE = new QuteJavaInjectionRegistry();
    private final Map<String, QuteJavaInjectionDescriptor> descriptorsByAnnotation;

    private QuteJavaInjectionRegistry() {
        this.descriptorsByAnnotation = new HashMap<>();

        // --- Qute core ---
        registerDescriptor(new QuteJavaInjectionDescriptor(
                QuteJavaConstants.TEMPLATE_CONTENTS_ANNOTATION,
                element -> {
                    PsiAnnotation annotation = PsiTreeUtil.getParentOfType(
                            element, PsiAnnotation.class
                    );
                    if (annotation == null) {
                        return null;
                    }
                    //  @TemplateContents(value = "<p>He <a></a>  <a></a> Hello2 {name}!}</p>",
                    //                    suffix = "html")
                    //    record HelloWithHtml(String name) implements TemplateInstance {}
                    String suffix = com.intellij.codeInsight.AnnotationUtil
                            .getDeclaredStringAttributeValue(annotation, "suffix");
                    return mapSuffixToLanguage(suffix);
                }
        ));

        registerDescriptor(new QuteJavaInjectionDescriptor(QuteJavaConstants.MESSAGE_ANNOTATION));

        // --- langchain4j ---
        registerDescriptor(new QuteJavaInjectionDescriptor("dev.langchain4j.service.UserMessage"));
        registerDescriptor(new QuteJavaInjectionDescriptor("dev.langchain4j.service.SystemMessage"));
    }

    public static QuteJavaInjectionRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Maps a suffix string to the corresponding IntelliJ Language.
     *
     * @param suffix the suffix, e.g. "html", "xml", "json", "yaml"
     * @return the corresponding Language instance, or PlainText if unknown/null
     */
    private static @NotNull Language mapSuffixToLanguage(@Nullable String suffix) {
        if (StringUtils.isEmpty(suffix)) {
            return PlainTextLanguage.INSTANCE;
        }
        switch (suffix.toLowerCase()) {
            case "html":
                return HTMLLanguage.INSTANCE;
            case "xml":
                return XMLLanguage.INSTANCE;
            case "json": {
                Language lang = Language.findLanguageByID("JSON"); // optional plugin
                return lang != null ? lang : PlainTextLanguage.INSTANCE;
            }
            case "yaml":
            case "yml": {
                Language lang = Language.findLanguageByID("YAML"); // optional plugin
                return lang != null ? lang : PlainTextLanguage.INSTANCE;
            }
            default:
                return PlainTextLanguage.INSTANCE;
        }
    }

    private void registerDescriptor(@NotNull QuteJavaInjectionDescriptor descriptor) {
        descriptorsByAnnotation.put(descriptor.getAnnotationName(), descriptor);
    }

    /**
     * Returns the descriptor for the given annotation, or null if not registered.
     */
    public @Nullable QuteJavaInjectionDescriptor getDescriptor(@Nullable PsiAnnotation annotation) {
        if (annotation == null) {
            return null;
        }
        return descriptorsByAnnotation.get(annotation.getQualifiedName());
    }
}
