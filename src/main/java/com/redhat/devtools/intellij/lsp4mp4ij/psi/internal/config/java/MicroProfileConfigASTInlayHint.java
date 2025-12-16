/*******************************************************************************
 * Copyright (c) 2025 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.config.java;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotationMemberValueExpression;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiField;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.inlayhint.JavaASTInlayHint;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.inlayhint.JavaInlayHintsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4mp.commons.MicroProfileJavaInlayHintSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Show converters and default value as inlay hint in Java file for fields which
 * are annotated with ConfigProperty annotation.
 */
public class MicroProfileConfigASTInlayHint extends JavaASTInlayHint {

    @Override
    public void visitAnnotation(@NotNull PsiAnnotation annotation) {
        if (AnnotationUtils.isMatchAnnotation(annotation, MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION)) {
            PsiField parent = PsiTreeUtil.getParentOfType(annotation, PsiField.class);
            if (parent != null) {
                JavaInlayHintsContext context = getContext();
                MicroProfileJavaInlayHintSettings settings = context.getSettings();
                if (settings.getDefaultValues().isEnabled()) {
                    generateDefaultValueInlayHint(annotation, context);
                }
                if (settings.getConverters().isEnabled()) {
                    generateConverterInlayHint(parent, context);
                }
            }
        }
    }

    private static void generateConverterInlayHint(PsiField fieldDeclaration,
                                                   JavaInlayHintsContext context) {
        context.addConverterInlayHint(fieldDeclaration.getType(), fieldDeclaration);
    }

    private static void generateDefaultValueInlayHint(PsiAnnotation annotation, JavaInlayHintsContext context) {
        PsiAnnotationMemberValue nameExpr = getAnnotationMemberValueExpression(annotation, MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION_NAME);
        if (nameExpr != null) {
            PsiAnnotationMemberValue defaultValueExpr = AnnotationUtils.getAnnotationMemberValueExpression(annotation,
                    MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE);
            if (defaultValueExpr == null) {
                String propertyKey = PsiTypeUtils.extractStringValue(nameExpr);
                if (propertyKey != null) {
                    String propertyValue = context.getMicroProfileProject().getProperty(propertyKey);
                    if (StringUtils.isNotBlank(propertyValue)) {
                        context.addInlayHint(", defaultValue=\"" + propertyValue + "\"",
                                nameExpr.getTextOffset() + nameExpr.getTextLength());
                    }
                }
            }
        }
    }

}
