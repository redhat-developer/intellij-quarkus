/*******************************************************************************
 * Copyright (c) 2019-2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.MergeQuery;
import com.intellij.util.Query;
import com.intellij.util.UniqueResultsQuery;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.PropertiesCollector;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.StaticPropertyProviderExtensionPointBean;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfoParams;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.eclipse.lsp4mp.commons.MicroProfilePropertyDefinitionParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * MicroProfile properties manager used to:
 *
 * <ul>
 * <li>collect MicroProfile, Quarkus properties</li>
 * <li>find Java definition from a given property</li>
 * </ul>
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/PropertiesManager.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/PropertiesManager.java</a>
 */
public class PropertiesManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesManager.class);

    private static final PropertiesManager INSTANCE = new PropertiesManager();

    public static PropertiesManager getInstance() {
        return INSTANCE;
    }

    private PropertiesManager() {
    }

    public MicroProfileProjectInfo getMicroProfileProjectInfo(MicroProfileProjectInfoParams params, IPsiUtils utils, ProgressIndicator monitor) {
        try {
            VirtualFile file = utils.findFile(params.getUri());
            if (file == null) {
                throw new UnsupportedOperationException(String.format("Cannot find virtual file for '%s'", params.getUri()));
            }
            return getMicroProfileProjectInfo(file, params.getScopes(), utils, params.getDocumentFormat(), monitor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MicroProfileProjectInfo getMicroProfileProjectInfo(VirtualFile file, List<MicroProfilePropertiesScope> scopes, IPsiUtils utils, DocumentFormat documentFormat, ProgressIndicator monitor) {
        Module module = utils.getModule(file);
        ClasspathKind classpathKind = PsiUtilsLSImpl.getClasspathKind(file, module);
        return getMicroProfileProjectInfo(module, scopes, classpathKind, utils, documentFormat, monitor);
    }

    public MicroProfileProjectInfo getMicroProfileProjectInfo(Module module,
                                                              List<MicroProfilePropertiesScope> scopes, ClasspathKind classpathKind, IPsiUtils utils,
                                                              DocumentFormat documentFormat, ProgressIndicator monitor) {
        MicroProfileProjectInfo info = createInfo(module, classpathKind);
        if (classpathKind == ClasspathKind.NONE) {
            info.setProperties(Collections.emptyList());
            return info;
        }
        monitor.setText("Scanning MicroProfile properties for '" + module.getName() + "' project in '" + scopes.stream() //
                .map(MicroProfilePropertiesScope::name) //
                .collect(Collectors.joining("+")) //
                + "'");
        long startTime = System.currentTimeMillis();
        boolean excludeTestCode = classpathKind == ClasspathKind.SRC;
        PropertiesCollector collector = new PropertiesCollector(info, scopes);
        if (module != null) {
            SearchScope scope = createSearchScope(module, scopes, classpathKind == ClasspathKind.TEST);
            SearchContext context = new SearchContext(module, scope, collector, utils, documentFormat);
            Query<PsiModifierListOwner> query = createSearchQuery(context);
            if (query != null) {
                try {
                    beginSearch(context, monitor);
                    for (PsiModifierListOwner psiMember : query.findAll()) {
                        collectProperties(psiMember, context, monitor);
                    }
                }
                finally {
                    endSearch(context, monitor);
                }
            }
        }
        LOGGER.info("End computing MicroProfile properties for '" + info.getProjectURI() + "' in "
                + (System.currentTimeMillis() - startTime) + "ms.");
        return info;
    }

    private void beginSearch(SearchContext context, ProgressIndicator monitor) {
        for (IPropertiesProvider provider : getPropertiesProviders()) {
            monitor.checkCanceled();
            provider.beginSearch(context);
        }
    }

    private void endSearch(SearchContext context, ProgressIndicator monitor) {
        for (IPropertiesProvider provider : getPropertiesProviders()) {
            monitor.checkCanceled();
            provider.endSearch(context);
        }
    }

    private void collectProperties(PsiModifierListOwner psiMember, SearchContext context, ProgressIndicator monitor) {
        for (IPropertiesProvider provider : getPropertiesProviders()) {
            monitor.checkCanceled();
            provider.collectProperties(psiMember, context);
        }
    }

    private static MicroProfileProjectInfo createInfo(Module module, ClasspathKind classpathKind) {
        MicroProfileProjectInfo info = new MicroProfileProjectInfo();
        info.setProjectURI(PsiUtilsLSImpl.getProjectURI(module));
        info.setClasspathKind(classpathKind);
        return info;
    }

    private SearchScope createSearchScope(@NotNull Module module, List<MicroProfilePropertiesScope> scopes,
                                          boolean excludeTestCode) {
        if (MicroProfilePropertiesScope.isOnlySources(scopes)) {
            return module.getModuleScope(!excludeTestCode);
        }
        SearchScope searchScope = GlobalSearchScope.moduleRuntimeScope(module, !excludeTestCode);
        for (MicroProfilePropertiesScope scope : scopes) {
            switch (scope) {
                case sources:
                    searchScope = searchScope.union(module.getModuleScope(!excludeTestCode));
                    break;
                case dependencies:
                    searchScope = searchScope.union(module.getModuleWithLibrariesScope());
                    break;
                /*added missing default case */
                default:
                    break;
            }
        }
        return searchScope;
    }

    private @Nullable Query<PsiModifierListOwner> createSearchQuery(SearchContext context) {
        Query<PsiModifierListOwner> query = null;

        for (IPropertiesProvider provider : getPropertiesProviders()) {
            Query<PsiModifierListOwner> providerQuery = provider.createSearchPattern(context);
            if (providerQuery != null) {
                if (query == null) {
                    query = providerQuery;
                } else {
                    query = new MergeQuery<>(query, providerQuery);
                }
            }
        }
        return new UniqueResultsQuery<>(query);
    }

    @NotNull
    List<IPropertiesProvider> getPropertiesProviders() {
        List<IPropertiesProvider> allProviders = new ArrayList<>();
        allProviders.addAll(IPropertiesProvider.EP_NAME.getExtensionList());
        allProviders.addAll(StaticPropertyProviderExtensionPointBean.EP_NAME.getExtensionList().stream()
                .map(bean -> bean.getInstance()).collect(Collectors.toList()));
        return allProviders;
    }

    // ---------------------------------- Properties definition

    public Location findPropertyLocation(MicroProfilePropertyDefinitionParams params, IPsiUtils utils) {
        try {
            VirtualFile file = utils.findFile(params.getUri());
            if (file == null) {
                throw new UnsupportedOperationException(String.format("Cannot find IFile for '%s'", params.getUri()));
            }
            return findPropertyLocation(file, params.getSourceType(), params.getSourceField(), params.getSourceMethod(),
                    utils);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Location findPropertyLocation(VirtualFile file, String sourceType, String sourceField, String sourceMethod,
                                         IPsiUtils utils) {
        Module module = utils.getModule(file);
        return findPropertyLocation(module, sourceType, sourceField, sourceMethod, utils);
    }

    public Location findPropertyLocation(Module module, String sourceType, String sourceField, String sourceMethod, IPsiUtils utils) {
        PsiMember fieldOrMethod = ReadAction
            .compute(() -> findDeclaredProperty(module, sourceType, sourceField, sourceMethod, utils));
        if (fieldOrMethod != null) {
            PsiFile classFile = fieldOrMethod.getContainingFile();
            if (classFile != null) {
                // Try to download source if required
                if (utils != null) {
                    utils.discoverSource(classFile);
                }
            }
            if (utils != null) {
                return ReadAction
                    .compute(() -> utils.toLocation(fieldOrMethod));
            }
        }
        return null;
    }

    /**
     * Returns the Java field from the given property source
     *
     * @param module       the Java project
     * @param sourceType   the source type (class or interface)
     * @param sourceField  the source field and null otherwise.
     * @param sourceMethod the source method and null otherwise.
     * @return the Java field from the given property sources
     */
    public PsiMember findDeclaredProperty(Module module, String sourceType, String sourceField,
                                          String sourceMethod, IPsiUtils utils) {
        if (sourceType == null) {
            return null;
        }
        // Try to find type with standard classpath
        PsiClass type = utils.findClass(module, sourceType);
        if (type == null) {
            return null;
        }
        if (sourceField != null) {
            return type.findFieldByName(sourceField, true);
        }
        if (sourceMethod != null) {
            int startBracketIndex = sourceMethod.indexOf('(');
            String methodName = sourceMethod.substring(0, startBracketIndex);
            // Method signature has been generated with PSI API, so we are sure that we have
            // a ')' character.
            for (PsiMethod method : type.findMethodsByName(methodName, true)) {
                String signature = PsiTypeUtils.getSourceMethod(method);
                if (signature.equals(sourceMethod)) {
                    return method;
                }
            }
        }
        return type;
    }
}
