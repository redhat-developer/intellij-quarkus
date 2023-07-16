/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.internal.template;

import com.intellij.codeInsight.completion.AllClassesGetter;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.completion.impl.BetterPrefixMatcher;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.util.Query;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.AbstractTypeResolver;
import com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils;
import com.redhat.qute.commons.JavaTypeInfo;
import com.redhat.qute.commons.JavaTypeKind;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Java types search for a given pattern and project Uri.
 *
 * @author Angelo ZERR
 */
public class JavaTypesSearch {

    private static final Logger LOGGER = Logger.getLogger(JavaTypesSearch.class.getName());

    private final Module javaProject;

    private final String packageName;

    private final String typeName;

    private final GlobalSearchScope scope;

    public JavaTypesSearch(String pattern, Module javaProject) {
        this.javaProject = javaProject;
        String typeName = "";
        String packageName = "";
        GlobalSearchScope searchScope = javaProject.getModuleScope();
        if (StringUtils.isNotEmpty(pattern)) {
            searchScope = javaProject.getModuleWithLibrariesScope();
            int index = pattern.lastIndexOf('.');
            if (index != -1) {
                // ex : pattern = org.acme.qute.It
                // -> packageName = org.acme.qute
                // -> typeName = It
                packageName = pattern.substring(0, index);
                typeName = pattern.substring(index + 1);
            } else {
                packageName = pattern;
                typeName = pattern;
            }
        }

        this.typeName = typeName;
        this.packageName = packageName;
        this.scope = searchScope;
    }

    public List<JavaTypeInfo> search(ProgressIndicator monitor) {
        List<JavaTypeInfo> javaTypes = new ArrayList<>();
        PsiPackage packageRoot = collectPackages(packageName, javaProject, javaTypes);
        collectClassesAndInterfaces(packageRoot, javaTypes);
        return javaTypes;
    }

    // ------------- Packages collector

    private static PsiPackage collectPackages(String packageName, Module javaProject, List<JavaTypeInfo> javaTypes) {
        PsiPackage packageRoot = JavaPsiFacade.getInstance(javaProject.getProject()).findPackage(packageName);
        if (packageRoot != null) {
            fillWithSubPackages(packageRoot, javaTypes);
        }
        return packageRoot;
    }

    private static void fillWithSubPackages(PsiPackage packageRoot, List<JavaTypeInfo> javaTypes) {
        PsiPackage[] allPackages = packageRoot.getSubPackages();
        for (int i = 0; i < allPackages.length; i++) {
            String subPackageName = allPackages[i].getQualifiedName();
            JavaTypeInfo packageInfo = new JavaTypeInfo();
            packageInfo.setJavaTypeKind(JavaTypeKind.Package);
            packageInfo.setSignature(subPackageName);
            javaTypes.add(packageInfo);
        }
    }

    // ------------- Classes, Interfaces, etc collector

    private void collectClassesAndInterfaces(PsiPackage packageRoot, List<JavaTypeInfo> javaTypes) {
        if (packageRoot != null) {
            // Search classes from the given proper package root.
            PsiClass[] classes = packageRoot.getClasses(scope);
            for (int i = 0; i < classes.length; i++) {
                collectClass(classes[i], javaTypes);
            }
        } else {
            // Search classes by the name (without the package name)
            PrefixMatcher matcher = new CamelHumpMatcher(typeName, true, false);
            matcher = new BetterPrefixMatcher(matcher, Integer.MIN_VALUE);

            final List<String> existing = new ArrayList<>();
            AllClassesGetter.processJavaClasses(matcher, javaProject.getProject(), scope,
                    psiClass -> {
                        String qName = psiClass.getQualifiedName();
                        if (qName != null && existing.add(qName)) {
                            collectClass(psiClass, javaTypes);
                        }
                        return true;
                    });
        }
    }

    private static void collectClass(PsiClass type, List<JavaTypeInfo> javaTypes) {
        String typeSignature = AbstractTypeResolver.resolveJavaTypeSignature(type);
        if (typeSignature != null) {
            JavaTypeInfo classInfo = new JavaTypeInfo();
            classInfo.setSignature(typeSignature);
            classInfo.setJavaTypeKind(PsiTypeUtils.getJavaTypeKind(type));
            javaTypes.add(classInfo);
        }
    }


}
