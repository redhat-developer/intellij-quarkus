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
package com.redhat.devtools.intellij.qute.psi.internal.extensions.renarde;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.DirectClassInheritorsSearch;
import com.intellij.psi.util.PsiClassUtil;
import com.intellij.util.Query;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.AbstractDataModelProvider;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.SearchContext;
import com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils;

import com.redhat.qute.commons.datamodel.resolvers.ValueResolverInfo;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverKind;

import static com.redhat.devtools.intellij.qute.psi.internal.extensions.renarde.RenardeJavaConstants.RENARDE_CONTROLLER_TYPE;
import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.JAVA_LANG_OBJECT_TYPE;

/**
 * uri, uriabs renarde support.
 *
 * @author Angelo ZERR
 * @see https://github.com/quarkiverse/quarkus-renarde/blob/main/docs/modules/ROOT/pages/index.adoc#obtaining-a-uri-in-qute-views
 */
public class UriNamespaceResolverSupport extends AbstractDataModelProvider {

    private static final Logger LOGGER = Logger.getLogger(UriNamespaceResolverSupport.class.getName());

    private static final String URI_NAMESPACE = "uri";

    private static final String URIABS_NAMESPACE = "uriabs";

    @Override
    public void beginSearch(SearchContext context, ProgressIndicator monitor) {
        Module javaProject = context.getJavaProject();
        PsiClass type = PsiTypeUtils.findType(javaProject, RENARDE_CONTROLLER_TYPE);
        if (type != null) {
            try {
                // Find all classes which extends 'io.quarkiverse.renarde.Controller'
                collectRenardeController(type, context, monitor);
            } catch (ProcessCanceledException e) {
                //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
                //TODO delete block when minimum required version is 2024.2
                throw e;
            } catch (IndexNotReadyException | CancellationException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while collecting Renarde Controller.", e);
            }
        }
    }

    public void collectRenardeController(PsiClass type, SearchContext context, ProgressIndicator monitor) {
        if (type == null) {
            return;
        }

        // Collect all sub types classes which extend 'io.quarkiverse.renarde.Controller'
        Project project = context.getUtils().getProject();
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        List<PsiClass> subTypes = new ArrayList<>();
        collectSubTypes(type, scope, subTypes);
        if (subTypes.isEmpty()) {
            return;
        }

        // Create a value resolver per each subtypes with "uri" and "uriabs" namespace
        List<ValueResolverInfo> resolvers = context.getDataModelProject().getValueResolvers();
        for (PsiClass controllerType : subTypes) {
            if (isRenardeController(controllerType)) {
                addRenardeController(URI_NAMESPACE, controllerType, resolvers);
                addRenardeController(URIABS_NAMESPACE, controllerType, resolvers);
            }
        }
    }

    private static void collectSubTypes(PsiClass psiClass, GlobalSearchScope scope, List<PsiClass> subTypes) {
        Query<PsiClass> query = DirectClassInheritorsSearch.search(psiClass, scope);
        List<PsiClass> directSubTypes = new ArrayList<>(query.findAll());
        subTypes.addAll(directSubTypes);

        for (PsiClass directSubType : directSubTypes) {
            collectSubTypes(directSubType, scope, subTypes);
        }
    }


    /**
     * Returns true if the given Java type is a non abstract Renarde controller and
     * false otherwise.
     *
     * @param controllerType the renarde controller type.
     * @return true if the given Java type is a non abstract Renarde controller and
     * false otherwise.
     */
    private static boolean isRenardeController(PsiClass controllerType) {
        if (controllerType.hasModifierProperty(PsiModifier.ABSTRACT)) {
            return false;
        }
        String typeName = controllerType.getQualifiedName();
        return !(JAVA_LANG_OBJECT_TYPE.equals(typeName) || RENARDE_CONTROLLER_TYPE.equals(typeName));
    }

    /**
     * Add renarde controller as Qute resolver.
     *
     * @param namespace      the uri, uriabs renarde namespace.
     * @param controllerType the controller type.
     * @param resolvers      the resolvers to fill.
     */
    private static void addRenardeController(String namespace, PsiClass controllerType,
                                             List<ValueResolverInfo> resolvers) {
        String className = controllerType.getQualifiedName();
        String named = controllerType.getName();
        ValueResolverInfo resolver = new ValueResolverInfo();
        resolver.setNamed(named);
        resolver.setSourceType(className);
        resolver.setSignature(className);
        resolver.setNamespace(namespace);
        resolver.setKind(ValueResolverKind.Renarde);
        if (!resolvers.contains(resolver)) {
            resolvers.add(resolver);
        }
    }

    @Override
    protected boolean isNamespaceAvailable(String namespace, SearchContext context, ProgressIndicator monitor) {
        // uri, and uriabs are available only for renarde project
        Module javaProject = context.getJavaProject();
        return PsiTypeUtils.findType(javaProject, RENARDE_CONTROLLER_TYPE) != null;
    }

    @Override
    public void collectDataModel(Object match, SearchContext context, ProgressIndicator monitor) {
        // Do nothing
    }

    @Override
    protected String[] getPatterns() {
        return null;
    }

    @Override
    protected Query<? extends Object> createSearchPattern(SearchContext context, String pattern) {
        return null;
    }
}