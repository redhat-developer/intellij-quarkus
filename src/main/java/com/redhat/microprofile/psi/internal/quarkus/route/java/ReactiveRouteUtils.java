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
package com.redhat.microprofile.psi.internal.quarkus.route.java;

import com.intellij.psi.*;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.HttpMethod;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.*;
import static com.redhat.microprofile.psi.internal.quarkus.route.java.ReactiveRouteConstants.*;


/**
 * Reactive @Route utilities.
 *
 * @see <a href="https://quarkus.io/guides/reactive-routes#declaring-reactive-routes">https://quarkus.io/guides/reactive-routes#declaring-reactive-routes</a>
 */
public class ReactiveRouteUtils {

    private ReactiveRouteUtils() {

    }

    /**
     * Returns true if the given method is a Reactive Route and false otherwise.
     *
     * @param method the method to check.
     * @return true if the given method is a Reactive Route and false otherwise.
     */
    public static boolean isReactiveRoute(@NotNull PsiMethod method) {
        if (!method.isConstructor() && AnnotationUtils.hasAnyAnnotation(method, ReactiveRouteConstants.ROUTE_FQN)) {
            // The method is annotated with @Route
            // A route method must be a non-private non-static method of a CDI bean.
            // See https://quarkus.io/guides/reactive-routes#reactive-route-methods
            return !method.getModifierList().hasExplicitModifier(PsiModifier.PRIVATE) && !method.getModifierList().hasExplicitModifier(PsiModifier.STATIC);
        }
        return false;
    }

    /**
     * Return the @RouteBase annotation from the given annotatable element and null otherwise.
     *
     * @param annotatable the annotatable element.
     * @return the @RouteBase annotation from the given annotatable element and null otherwise.
     */
    public static @Nullable PsiAnnotation getRouteBaseAnnotation(PsiElement annotatable) {
        return getFirstAnnotation(annotatable, ROUTE_BASE_FQN);
    }

    /**
     * Returns the value of @RouteBase path attribute and null otherwise..
     *
     * @param routeBaseAnnotation the @Route annotation.
     * @return the value of @RouteBase path attribute and null otherwise.
     */
    public static String getRouteBasePath(PsiAnnotation routeBaseAnnotation) {
        return getAnnotationMemberValue(routeBaseAnnotation, ROUTE_BASE_PATH);
    }

    /**
     * Return the list of @Route annotation from the given annotatable element.
     *
     * @param annotatable the annotatable element.
     * @return the list of @Route annotation from the given annotatable element.
     */
    public static List<PsiAnnotation> getRouteAnnotations(PsiElement annotatable) {
        return getAllAnnotations(annotatable, ROUTE_FQN);
    }

    /**
     * Returns the value of @Route path attribute and null otherwise..
     *
     * @param routeAnnotation the @Route annotation.
     * @return the value of @Route path attribute and null otherwise.
     */
    public static String getRoutePath(PsiAnnotation routeAnnotation) {
        return getAnnotationMemberValue(routeAnnotation, ROUTE_PATH);
    }

    public static String getRouteHttpMethodName(PsiAnnotation routeAnnotation) {
        PsiAnnotationMemberValue methodsExpr = getAnnotationMemberValueExpression(routeAnnotation, ROUTE_METHODS);
        PsiElement last = methodsExpr != null ? methodsExpr.getLastChild() : null;
        if (last != null) {
            // returns, GET, POST, etc
            return last.getText();
        }
        return null;
    }

    /**
     * Returns an HttpMethod given the FQN of a Reactive @Route/methods
     * annotation, nor null if the FQN doesn't match any HttpMethod.
     *
     * @param httpMethodName the Http method name of the annotation to convert into a HttpMethod
     * @return an HttpMethod given the FQN of a Reactive @Route/methods
     * * annotation, nor null if the FQN doesn't match any HttpMethod.
     */
    public static HttpMethod getHttpMethodForAnnotation(String httpMethodName) {
        if (httpMethodName != null) {
            try {
                return HttpMethod.valueOf(httpMethodName);
            } catch (Exception e) {
                // Do nothing
            }
        }
        return HttpMethod.GET;
    }
}
