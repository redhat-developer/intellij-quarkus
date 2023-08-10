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
package com.redhat.devtools.intellij.quarkus.psi.internal;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsConstants;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.restclient.MicroProfileRestClientConstants;
import org.jetbrains.annotations.NotNull;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.hasAnyAnnotation;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.QUARKUS_DEPLOYMENT_BUILDSTEP_ANNOTATION;

/**
 * Automatically declares as used, methods annotated with {@link io.quarkus.deployment.annotations.BuildStep} annotations, such as:
 * <p>
 * {@code
 *     @BuildStep
 *     AdditionalBeanBuildItem producePrettyTime() {
 *         return new AdditionalBeanBuildItem(PrettyTimeProducer.class);
 *     }
 * }
 * </p>
 */
public class QuarkusBuildImplicitUsageProvider implements ImplicitUsageProvider {

    @Override
    public boolean isImplicitUsage(@NotNull PsiElement element) {
        return isImplicitRead(element) || isImplicitWrite(element);
    }

    @Override
    public boolean isImplicitRead(@NotNull PsiElement element) {
        return element instanceof PsiMethod &&
                !((PsiMethod)element).isConstructor() &&
                AnnotationUtils.hasAnyAnnotation(element, QUARKUS_DEPLOYMENT_BUILDSTEP_ANNOTATION);
    }

    @Override
    public boolean isImplicitWrite(@NotNull PsiElement element) {
        return false;
    }
}
