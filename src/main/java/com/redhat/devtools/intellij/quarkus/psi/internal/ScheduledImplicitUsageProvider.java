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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.metrics.MicroProfileMetricsConstants;
import org.jetbrains.annotations.NotNull;

import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.QUARKUS_DEPLOYMENT_BUILDSTEP_ANNOTATION;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.QUARKUS_SCHEDULED_ANNOTATION;

/**
 * Automatically declares as used, methods annotated with {@link io.quarkus.scheduler.Scheduled} annotations, on <code>@ApplicationScoped</code>-annotated classes.
 * <p>
 * {@code
 *     @Scheduled(cron="0 15 10 * * ?")
 *     void cronJob(ScheduledExecution execution) {
 *         counter.incrementAndGet();
 *         System.out.println(execution.getScheduledFireTime());
 *     }
 * }
 * </p>
 */
public class ScheduledImplicitUsageProvider implements ImplicitUsageProvider {

    @Override
    public boolean isImplicitUsage(@NotNull PsiElement element) {
        return isImplicitRead(element) || isImplicitWrite(element);
    }

    @Override
    public boolean isImplicitRead(@NotNull PsiElement element) {
        if (!(element instanceof PsiMethod)) {
            return false;
        }
        PsiMethod method = (PsiMethod) element;
        return !method.isConstructor() &&
                AnnotationUtils.hasAnyAnnotation(element, QUARKUS_SCHEDULED_ANNOTATION) &&
                isClassApplicationScoped(method.getContainingClass());
    }

    private boolean isClassApplicationScoped(PsiClass containingClass) {
        return (containingClass != null) && AnnotationUtils.hasAnyAnnotation(containingClass, MicroProfileMetricsConstants.APPLICATION_SCOPED_JAKARTA_ANNOTATION,
                MicroProfileMetricsConstants.APPLICATION_SCOPED_JAVAX_ANNOTATION);
    }

    @Override
    public boolean isImplicitWrite(@NotNull PsiElement element) {
        return false;
    }
}
