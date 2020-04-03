/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiMember;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.MergeQuery;
import com.intellij.util.Query;
import com.redhat.devtools.intellij.quarkus.search.core.utils.IPsiUtils;
import com.redhat.microprofile.commons.ClasspathKind;
import com.redhat.microprofile.commons.DocumentFormat;
import com.redhat.microprofile.commons.MicroProfileProjectInfo;
import com.redhat.microprofile.commons.MicroProfileProjectInfoParams;
import com.redhat.microprofile.commons.MicroProfilePropertiesScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Consumer;

/**
 * MicroProfile properties manager used to:
 *
 * <ul>
 * <li>collect MicroProfile, Quarkus properties</li>
 * <li>find Java definition from a given property</li>
 * </ul>
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/PropertiesManager.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/PropertiesManager.java</a>
 *
 */
public class PropertiesManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesManager.class);

    private static final PropertiesManager INSTANCE = new PropertiesManager();

    public static PropertiesManager getInstance() {
        return INSTANCE;
    }

    private PropertiesManager() {}

    public MicroProfileProjectInfo getMicroProfileProjectInfo(MicroProfileProjectInfoParams params, IPsiUtils utils) {
        try {
            VirtualFile file = utils.findFile(params.getUri());
            if (file == null) {
                throw new UnsupportedOperationException(String.format("Cannot find virtual file for '%s'", params.getUri()));
            }
            return getMicroProfileProjectInfo(file, params.getScopes(), utils, params.getDocumentFormat());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public MicroProfileProjectInfo getMicroProfileProjectInfo(VirtualFile file, List<MicroProfilePropertiesScope> scopes, IPsiUtils utils, DocumentFormat documentFormat) {
        Module module = utils.getModule(file);
        ClasspathKind classpathKind = PsiUtilsImpl.getClasspathKind(file, module);
        return getMicroProfileProjectInfo(module, scopes, classpathKind, utils, documentFormat);
    }

    public MicroProfileProjectInfo getMicroProfileProjectInfo(Module module,
                                                              List<MicroProfilePropertiesScope> scopes, ClasspathKind classpathKind, IPsiUtils utils,
                                                              DocumentFormat documentFormat) {
        MicroProfileProjectInfo info = createInfo(module, classpathKind);
        long startTime = System.currentTimeMillis();
        boolean excludeTestCode = classpathKind == ClasspathKind.SRC;
        PropertiesCollector collector = new PropertiesCollector(info);
        SearchScope scope = createSearchScope(module, scopes, classpathKind == ClasspathKind.TEST);
        SearchContext context = new SearchContext(module, scope, collector, utils, documentFormat);
        DumbService.getInstance(module.getProject()).runReadActionInSmartMode(() -> {
            Query<PsiMember> query = createSearchQuery(context);
            beginSearch(context);
            query.forEach((Consumer<? super PsiMember>) psiMember -> collectProperties(psiMember, context));
            endSearch(context);
        });
        LOGGER.info("End computing MicroProfile properties for '" + info.getProjectURI() + "' in "
                + (System.currentTimeMillis() - startTime) + "ms.");
        return info;
    }

    private void beginSearch(SearchContext context) {
        for(IPropertiesProvider provider : IPropertiesProvider.EP_NAME.getExtensions()) {
            provider.beginSearch(context);
        }
    }

    private void endSearch(SearchContext context) {
        for(IPropertiesProvider provider : IPropertiesProvider.EP_NAME.getExtensions()) {
            provider.endSearch(context);
        }
    }

    private void collectProperties(PsiMember psiMember, SearchContext context) {
        for(IPropertiesProvider provider : IPropertiesProvider.EP_NAME.getExtensions()) {
            provider.collectProperties(psiMember, context);
        }
    }

    private static MicroProfileProjectInfo createInfo(Module module, ClasspathKind classpathKind) {
        MicroProfileProjectInfo info = new MicroProfileProjectInfo();
        info.setProjectURI(PsiUtilsImpl.getProjectURI(module));
        info.setClasspathKind(classpathKind);
        return info;
    }

    private SearchScope createSearchScope(Module module, List<MicroProfilePropertiesScope> scopes,
                                               boolean excludeTestCode) {
        SearchScope searchScope = GlobalSearchScope.EMPTY_SCOPE;

        for (MicroProfilePropertiesScope scope : scopes) {
            switch (scope) {
                case sources:
                    searchScope = searchScope.union(module.getModuleScope(!excludeTestCode));
                    break;
                case dependencies:
                    searchScope = searchScope.union(module.getModuleWithLibrariesScope());
                    break;
            }
        }
        return searchScope;
    }

    private Query<PsiMember> createSearchQuery(SearchContext context) {
        Query<PsiMember> query = null;

        for(IPropertiesProvider provider : IPropertiesProvider.EP_NAME.getExtensions()) {
          Query<PsiMember> providerQuery = provider.createSearchPattern(context);
          if (providerQuery != null) {
              if (query == null) {
                  query = providerQuery;
              } else {
                  query = new MergeQuery<>(query, providerQuery);
              }
          }
        }
        return query;
    }
}
