/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.quarkus.psi.internal.validators;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.util.InheritanceUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.IJavaDiagnosticsParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PositionUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;
import com.redhat.devtools.intellij.quarkus.psi.internal.builditems.QuarkusBuildItemErrorCode;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4mp.commons.DocumentFormat;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates <code>io.quarkus.builder.item.BuildItem</code> subclasses.
 * <ul>
 *     <li>checks if the BuildItem is final or abstract</li>
 * </ul>
 */
public class QuarkusBuildItemDiagnosticsParticipant implements IJavaDiagnosticsParticipant {

    private static final String INVALID_MODIFIER = "BuildItem class %2$s%1$s%2$s must either be declared final or abstract";

    @Override
    public boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context) {
        // Collection of diagnostics for Quarkus Build Items is done only if
        // io.quarkus.builder.item.BuildItem is on the classpath
        Module javaProject = context.getJavaProject();
        return PsiTypeUtils.findType(javaProject, QuarkusConstants.QUARKUS_BUILD_ITEM_CLASS_NAME) != null;
    }

    @Override
    public void collectDiagnostics(JavaDiagnosticsContext context) {
        PsiFile typeRoot = context.getTypeRoot();
        PsiElement[] elements = typeRoot.getChildren();
        collectDiagnostics(elements, context);
    }

    private static void collectDiagnostics(PsiElement[] elements,
                                           JavaDiagnosticsContext context) {
        for (PsiElement element : elements) {
            if (element instanceof PsiClass) {
                PsiClass psiClass = (PsiClass) element;
                if (isBuildItem(psiClass)) {
                    validateBuildItem(psiClass, context);
                }
            }
        }
    }

    private static boolean isBuildItem(PsiClass type) {
        return InheritanceUtil.isInheritor(type, QuarkusConstants.QUARKUS_BUILD_ITEM_CLASS_NAME);
    }

    private static void validateBuildItem(PsiClass psiClass, JavaDiagnosticsContext context) {
        if (psiClass.hasModifierProperty(PsiModifier.FINAL)
        || psiClass.hasModifierProperty(PsiModifier.ABSTRACT)
        ) {
            return;
        }
        Range range = PositionUtils.toClassDeclarationRange(psiClass, context.getUtils());
        context.addDiagnostic(createDiagnosticMessage(psiClass, context.getDocumentFormat()),
                range, QuarkusConstants.QUARKUS_DIAGNOSTIC_SOURCE,
                QuarkusBuildItemErrorCode.InvalidModifierBuildItem,
                DiagnosticSeverity.Error);
    }

    private static String createDiagnosticMessage(PsiClass classType, DocumentFormat documentFormat) {
        String quote = DocumentFormat.Markdown.equals(documentFormat)?"`":"'";
        return String.format(INVALID_MODIFIER, classType.getQualifiedName(), quote);
    }
}
