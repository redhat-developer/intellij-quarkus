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
import com.intellij.psi.PsiElement;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.restclient.MicroProfileRestClientConstants;
import org.jetbrains.annotations.NotNull;

/**
 * Automatically declares as used, methods annotated with jakarta.ws.rs.* or javax.ws.rs.* HTTP annotations,
 * or {@link org.eclipse.microprofile.rest.client.inject.RestClient}
 */
public class JavaEEImplicitUsageProvider implements ImplicitUsageProvider {

    @Override
    public boolean isImplicitUsage(@NotNull PsiElement element) {
        return isImplicitRead(element) || isImplicitWrite(element);
    }

    @Override
    public boolean isImplicitRead(@NotNull PsiElement element) {
        return AnnotationUtils.hasAnyAnnotation(element, JaxRsConstants.HTTP_METHOD_ANNOTATIONS) ||
                AnnotationUtils.hasAnnotation(element, MicroProfileRestClientConstants.REST_CLIENT_ANNOTATION);
    }

    @Override
    public boolean isImplicitWrite(@NotNull PsiElement element) {
        return false;
    }
}
