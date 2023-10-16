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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.*;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.restclient.MicroProfileRestClientConstants;
import org.jetbrains.annotations.NotNull;

/**
 * Automatically declares the following as used:
 * <ul>
 *     <li>methods annotated with jakarta.ws.rs.* or javax.ws.rs.* HTTP annotations, or {@link org.eclipse.microprofile.rest.client.inject.RestClient}</li>
 *     <li>non-abstract public observer methods, i.e. with a parameter annotated with <code>@jakarta.enterprise.event.Observes</code> or <code>@javax.enterprise.event.Observes</code></li>
 * </ul>
 */
public class JavaEEImplicitUsageProvider implements ImplicitUsageProvider {

    private static final String JAKARTA_OBSERVES = "jakarta.enterprise.event.Observes";
    private static final String JAVAX_OBSERVES = "javax.enterprise.event.Observes";


    @Override
    public boolean isImplicitUsage(@NotNull PsiElement element) {
        return isImplicitRead(element) || isImplicitWrite(element);
    }

    @Override
    public boolean isImplicitRead(@NotNull PsiElement element) {
        return isHttpOrRestClient(element) || isObserverMethod(element);
    }

    private boolean isObserverMethod(@NotNull PsiElement element) {
        if (element instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) element;
            return isNonAbstract(method) && observesOneParameter(method);
        }
        return false;
    }

    private boolean observesOneParameter(PsiMethod method) {
        //XXX ideally we might want to check if the parent class is a managed bean class or session bean class (or of an extension)
        //But that might prove a bit complex, so let's skip this part for now.

        PsiParameter[] parameters = method.getParameterList().getParameters();

        int observesAnnotationCount = 0;

        for (PsiParameter parameter : parameters) {
            if (AnnotationUtils.hasAnyAnnotation(parameter, JAKARTA_OBSERVES, JAVAX_OBSERVES)) {
                observesAnnotationCount++;
                if (observesAnnotationCount > 1) {
                    return false;
                }
            }
        }

        return observesAnnotationCount == 1;
    }

    private boolean isNonAbstract(@NotNull PsiMethod method) {
        return !method.isConstructor() &&
                !method.hasModifierProperty(PsiModifier.ABSTRACT);
    }

    private boolean isHttpOrRestClient(@NotNull PsiElement element) {
        return AnnotationUtils.hasAnyAnnotation(element, JaxRsConstants.HTTP_METHOD_ANNOTATIONS) ||
                AnnotationUtils.hasAnnotation(element, MicroProfileRestClientConstants.REST_CLIENT_ANNOTATION);
    }

    @Override
    public boolean isImplicitWrite(@NotNull PsiElement element) {
        return false;
    }
}
