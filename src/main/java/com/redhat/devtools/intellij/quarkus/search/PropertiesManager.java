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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.ClassUtil;
import com.intellij.util.MergeQuery;
import com.intellij.util.Query;
import com.redhat.devtools.intellij.quarkus.search.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.quarkus.search.core.utils.PsiTypeUtils;
import com.redhat.microprofile.commons.ClasspathKind;
import com.redhat.microprofile.commons.DocumentFormat;
import com.redhat.microprofile.commons.MicroProfileProjectInfo;
import com.redhat.microprofile.commons.MicroProfileProjectInfoParams;
import com.redhat.microprofile.commons.MicroProfilePropertiesScope;
import com.redhat.microprofile.commons.MicroProfilePropertyDefinitionParams;
import org.eclipse.lsp4j.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MicroProfileProjectInfo getMicroProfileProjectInfo(VirtualFile file, List<MicroProfilePropertiesScope> scopes, IPsiUtils utils, DocumentFormat documentFormat) {
        Module module = ApplicationManager.getApplication().runReadAction((Computable<Module>) () -> utils.getModule(file));
        ClasspathKind classpathKind = PsiUtilsImpl.getClasspathKind(file, module);
        return getMicroProfileProjectInfo(module, scopes, classpathKind, utils, documentFormat);
    }

    public MicroProfileProjectInfo getMicroProfileProjectInfo(Module module,
                                                              List<MicroProfilePropertiesScope> scopes, ClasspathKind classpathKind, IPsiUtils utils,
                                                              DocumentFormat documentFormat) {
        MicroProfileProjectInfo info = createInfo(module, classpathKind);
        long startTime = System.currentTimeMillis();
        boolean excludeTestCode = classpathKind == ClasspathKind.SRC;
        PropertiesCollector collector = new PropertiesCollector(info, scopes);
        SearchScope scope = createSearchScope(module, scopes, classpathKind == ClasspathKind.TEST);
        SearchContext context = new SearchContext(module, scope, collector, utils, documentFormat);
        DumbService.getInstance(module.getProject()).runReadActionInSmartMode(() -> {
            Query<PsiModifierListOwner> query = createSearchQuery(context);
            beginSearch(context);
            query.forEach((Consumer<? super PsiModifierListOwner>) psiMember -> collectProperties(psiMember, context));
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

    private void collectProperties(PsiModifierListOwner psiMember, SearchContext context) {
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

    private Query<PsiModifierListOwner> createSearchQuery(SearchContext context) {
        Query<PsiModifierListOwner> query = null;

        for(IPropertiesProvider provider : IPropertiesProvider.EP_NAME.getExtensions()) {
          Query<PsiModifierListOwner> providerQuery = provider.createSearchPattern(context);
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
        return DumbService.getInstance(module.getProject()).runReadActionInSmartMode(() -> {
            PsiMember fieldOrMethod = findDeclaredProperty(module, sourceType, sourceField, sourceMethod, utils);
            if (fieldOrMethod != null) {
                PsiFile classFile = fieldOrMethod.getContainingFile();
                if (classFile != null) {
                    // Try to download source if required
                    if (utils != null) {
                        utils.discoverSource(classFile);
                    }
                }
                return utils.toLocation(fieldOrMethod);
            }
            return null;
        });
    }

    /** * Returns the Java field from the given property source
     *
     * @param module  the Java project
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
            for(PsiMethod method : type.findMethodsByName(methodName, true)) {
                String signature = PsiTypeUtils.getSourceMethod(method);
                if (signature.equals(sourceMethod)) {
                    return method;
                }
            }
         }
        return type;
     }
}
