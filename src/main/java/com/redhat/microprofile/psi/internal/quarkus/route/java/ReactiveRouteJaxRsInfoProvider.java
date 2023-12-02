/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.microprofile.psi.internal.quarkus.route.java;


import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.*;
import com.intellij.util.KeyedLazyInstanceEP;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.*;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.overlaps;
import static com.redhat.microprofile.psi.internal.quarkus.route.java.ReactiveRouteConstants.ROUTE_FQN;
import static com.redhat.microprofile.psi.internal.quarkus.route.java.ReactiveRouteUtils.getRouteHttpMethodName;
import static com.redhat.microprofile.psi.internal.quarkus.route.java.ReactiveRouteUtils.isReactiveRoute;

/**
 * Use custom logic for all JAX-RS features for Reactive @Route.
 *
 * @see <a href="https://quarkus.io/guides/reactive-routes#declaring-reactive-routes">https://quarkus.io/guides/reactive-routes#declaring-reactive-routes</a>
 */
public class ReactiveRouteJaxRsInfoProvider extends KeyedLazyInstanceEP<IJaxRsInfoProvider> implements IJaxRsInfoProvider {

    private static final Logger LOGGER = Logger.getLogger(ReactiveRouteJaxRsInfoProvider.class.getName());

    @Override
    public boolean canProvideJaxRsMethodInfoForClass(PsiFile typeRoot, Module javaProject, ProgressIndicator monitor) {
        return PsiTypeUtils.findType(javaProject, ROUTE_FQN) != null;
    }

    @Override
    public Set<PsiClass> getAllJaxRsClasses(Module javaProject, ProgressIndicator monitor) {
        // TODO: implement when LSP4IJ will support workspace symbols
        return Collections.emptySet();
    }

    @Override
    public List<JaxRsMethodInfo> getJaxRsMethodInfo(PsiFile typeRoot, JaxRsContext jaxrsContext, IPsiUtils utils,
                                                    ProgressIndicator monitor) {
        try {
            PsiClass type = findFirstClass(typeRoot);
            if (type == null) {
                return Collections.emptyList();
            }
            // See https://quarkus.io/guides/reactive-routes#routebase
            // Try to get the @RouteBase declared in the Java type

            PsiAnnotation routeBaseAnnotation = ReactiveRouteUtils.getRouteBaseAnnotation(type);
            String pathSegment = routeBaseAnnotation != null ? ReactiveRouteUtils.getRouteBasePath(routeBaseAnnotation) : null;

            List<JaxRsMethodInfo> methodInfos = new ArrayList<>();
            for (PsiMethod method : type.getMethods()) {

                if (method.isConstructor() || utils.isHiddenGeneratedElement(method)) {
                    continue;
                }
                // ignore element if method range overlaps the type range,
                // happens for generated
                // bytecode, i.e. with lombok
                if (overlaps(type.getNameIdentifier().getTextRange(), method.getNameIdentifier().getTextRange())) {
                    continue;
                }

                if (!isReactiveRoute(method)) {
                    continue;
                }
                // Here method is annotated with @Route

                // Method can have several @Route
                // @Route(path = "/first")
                // @Route(path = "/second")
                // public void route(RoutingContext rc) {
                //    // ...
                List<PsiAnnotation> routeAnnotations = ReactiveRouteUtils.getRouteAnnotations(method);

                // Loop for @Route annotation
                for (PsiAnnotation routeAnnotation : routeAnnotations) {
                    // @Route(path = "/first")
                    String methodSegment = ReactiveRouteUtils.getRoutePath(routeAnnotation);
                    if (methodSegment == null) {
                        // @Route(methods = Route.HttpMethod.GET)
                        // void hello(RoutingContext rc)
                        // Here the segment is the method name
                        methodSegment = method.getName();
                    }
                    String path;
                    if (pathSegment == null) {
                        path = methodSegment;
                    } else {
                        path = JaxRsUtils.buildURL(pathSegment, methodSegment);
                    }
                    String url = JaxRsUtils.buildURL(jaxrsContext.getLocalBaseURL(), path);

                    JaxRsMethodInfo methodInfo = createMethodInfo(method, routeAnnotation, url);
                    if (methodInfo != null) {
                        methodInfos.add(methodInfo);
                    }
                }
            }
            return methodInfos;
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while collecting JAX-RS methods for Reactive @Route", e);
            return Collections.emptyList();
        }
    }

    private PsiClass findFirstClass(PsiFile typeRoot) {
        for (PsiElement element : typeRoot.getChildren()) {
            if (element instanceof PsiClass) {
                return (PsiClass) element;
            }
        }
        return null;
    }

    private static JaxRsMethodInfo createMethodInfo(PsiMethod method, PsiAnnotation routeAnnotation, String url) {

        PsiFile resource = method.getContainingFile();
        if (resource == null) {
            return null;
        }
        String documentUri = LSPIJUtils.toUriAsString(resource);

        String httpMethodName = getRouteHttpMethodName(routeAnnotation);
        HttpMethod httpMethod = ReactiveRouteUtils.getHttpMethodForAnnotation(httpMethodName);
        return new JaxRsMethodInfo(url, httpMethod, method, documentUri);
    }

}