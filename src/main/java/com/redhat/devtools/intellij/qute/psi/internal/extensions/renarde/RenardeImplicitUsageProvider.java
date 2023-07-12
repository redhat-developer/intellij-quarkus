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
package com.redhat.devtools.intellij.qute.psi.internal.extensions.renarde;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants;
import com.redhat.microprofile.psi.internal.quarkus.renarde.java.RenardeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Automatically declares as used, public methods from classes extending Renarde's {@io.quarkiverse.renarde.Controller}
 */
public class RenardeImplicitUsageProvider implements ImplicitUsageProvider {
    @Override
    public boolean isImplicitUsage(@NotNull PsiElement element) {
        return isImplicitRead(element) || isImplicitWrite(element);
    }

    @Override
    public boolean isImplicitRead(@NotNull PsiElement element) {
        PsiClass clazz = null;
        if (element instanceof PsiClass) {
            clazz = (PsiClass)element;
            if (!hasPublicMethods(clazz)) {
                return false;
            };
        } else if (element instanceof PsiMethod) {
            PsiMethod method = (PsiMethod)element;
            if (isPublic(method)) {
                clazz = method.getContainingClass();
            }
        }
        return RenardeUtils.isControllerClass(clazz);
    }

    private boolean hasPublicMethods(@NotNull PsiClass clazz) {
        return Arrays.stream(clazz.getAllMethods()).anyMatch(this::isPublic);
    }

    private boolean isPublic(@NotNull PsiMethod method) {
        return !method.isConstructor()
                && method.getContainingClass() != null
                && !QuteJavaConstants.JAVA_LANG_OBJECT_TYPE.equals(method.getContainingClass().getQualifiedName())
                && method.getModifierList().hasModifierProperty(PsiModifier.PUBLIC)
                && !method.getModifierList().hasModifierProperty(PsiModifier.STATIC);
    }

    @Override
    public boolean isImplicitWrite(@NotNull PsiElement element) {
        return false;
    }
}
