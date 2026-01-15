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
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants;
import org.eclipse.lsp4mp.commons.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry of Java annotations that embed Qute template content.
 */
public final class QuteJavaInjectionRegistry {

    private static final QuteJavaInjectionRegistry INSTANCE = new QuteJavaInjectionRegistry();

    private final Map<String, QuteJavaInjectionDescriptor> descriptorsByAnnotation;

    private QuteJavaInjectionRegistry() {
        Map<String, QuteJavaInjectionDescriptor> map = new HashMap<>();

        // --- Qute core ---
        register(map, new QuteJavaInjectionDescriptor(
                QuteJavaConstants.TEMPLATE_CONTENTS_ANNOTATION,
                element -> {

                    PsiAnnotation annotation = PsiTreeUtil.getParentOfType(
                            element, PsiAnnotation.class
                    );
                    if (annotation == null) {
                        return null;
                    }

                    // IMPORTANT:
                    // Do NOT resolve anything when indexes are not ready
                    if (DumbService.isDumb(annotation.getProject())) {
                        return PlainTextLanguage.INSTANCE;
                    }

                    String suffix = com.intellij.codeInsight.AnnotationUtil
                            .getDeclaredStringAttributeValue(annotation, "suffix");

                    return mapSuffixToLanguage(suffix);
                }
        ));

        register(map, new QuteJavaInjectionDescriptor(QuteJavaConstants.MESSAGE_ANNOTATION));

        // --- langchain4j ---
        register(map, new QuteJavaInjectionDescriptor("dev.langchain4j.service.UserMessage"));
        register(map, new QuteJavaInjectionDescriptor("dev.langchain4j.service.SystemMessage"));

        this.descriptorsByAnnotation = Collections.unmodifiableMap(map);
    }

    public static QuteJavaInjectionRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the descriptor for the given annotation, or null if not registered.
     *
     * IMPORTANT:
     * Must be dumb-aware because this method is called from debugger / injections.
     */
    public @Nullable QuteJavaInjectionDescriptor getDescriptor(@Nullable PsiAnnotation annotation) {
        if (annotation == null) {
            return null;
        }

        // 🔒 CRITICAL: never touch indexes during dumb mode
        if (DumbService.isDumb(annotation.getProject())) {
            return null;
        }

        // ⚠ This call resolves the annotation -> indexes required
        String qualifiedName = annotation.getQualifiedName();
        if (qualifiedName == null) {
            return null;
        }

        return descriptorsByAnnotation.get(qualifiedName);
    }

    private static void register(
            @NotNull Map<String, QuteJavaInjectionDescriptor> map,
            @NotNull QuteJavaInjectionDescriptor descriptor
    ) {
        map.put(descriptor.getAnnotationName(), descriptor);
    }

    /**
     * Maps a suffix string to the corresponding IntelliJ Language.
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
                Language lang = Language.findLanguageByID("JSON");
                return lang != null ? lang : PlainTextLanguage.INSTANCE;
            }
            case "yaml":
            case "yml": {
                Language lang = Language.findLanguageByID("YAML");
                return lang != null ? lang : PlainTextLanguage.INSTANCE;
            }
            default:
                return PlainTextLanguage.INSTANCE;
        }
    }
}
