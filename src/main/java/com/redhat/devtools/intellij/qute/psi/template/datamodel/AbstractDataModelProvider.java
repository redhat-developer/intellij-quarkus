/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.qute.psi.template.datamodel;

import java.util.Map;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.EmptyQuery;
import com.intellij.util.MergeQuery;
import com.intellij.util.Query;

import com.redhat.qute.commons.datamodel.resolvers.NamespaceResolverInfo;

/**
 * Abstract class for data model provider.
 *
 * @author Angelo ZERR
 *
 */
public abstract class AbstractDataModelProvider implements IDataModelProvider {

	private NamespaceResolverInfo namespaceResolverInfo;

	@Override
	public void setNamespaceResolverInfo(NamespaceResolverInfo namespaceResolverInfo) {
		this.namespaceResolverInfo = namespaceResolverInfo;
	}

	/**
	 * Returns the namespace resolver information and null otherwise.
	 * 
	 * @return the namespace resolver information and null otherwise.
	 */
	public NamespaceResolverInfo getNamespaceResolverInfo() {
		return namespaceResolverInfo;
	}

	/**
	 * Returns the Java search pattern.
	 *
	 * @return the Java search pattern.
	 */
	protected abstract String[] getPatterns();

	@Override
	public Query<? extends Object> createSearchPattern(SearchContext context) {
		Query<? extends Object> leftPattern = null;
		String[] patterns = getPatterns();

		if (patterns == null) {
			return null;
		}

		for (String pattern : patterns) {
			if (leftPattern == null) {
				leftPattern = createSearchPattern(context, pattern);
			} else {
				Query<? extends Object> rightPattern = createSearchPattern(context, pattern);
				if (rightPattern != null) {
					leftPattern = new MergeQuery<>(leftPattern, rightPattern);
				}
			}
		}
		return leftPattern;
	}

	/**
	 * Create an instance of search pattern with the given <code>pattern</code>.
	 *
	 * @param pattern the search pattern
	 * @return an instance of search pattern with the given <code>pattern</code>.
	 */
	protected abstract Query<? extends Object> createSearchPattern(SearchContext context, String pattern);

	/**
	 * Create a search pattern for the given <code>annotationName</code> annotation
	 * name.
	 *
	 * @param annotationName the annotation name to search.
	 * @return a search pattern for the given <code>annotationName</code> annotation
	 *         name.
	 */
	protected static Query<PsiModifierListOwner> createAnnotationTypeReferenceSearchPattern(SearchContext context,
																							String annotationName) {
		PsiClass annotationClass = context.getUtils().findClass(context.getJavaProject(), annotationName);
		if (annotationClass != null) {
			return AnnotatedElementsSearch.searchElements(annotationClass, context.getJavaProject().getModuleWithDependenciesAndLibrariesScope(false), PsiModifierListOwner.class);
		} else {
			return new EmptyQuery<>();
		}
	}

	/**
	 * Create a search pattern for the given <code>className</code> class name.
	 *
	 * @param className the class name to search.
	 * @return a search pattern for the given <code>className</code> class name.
	 */
	protected static Query<PsiReference> createFieldDeclarationTypeReferenceSearchPattern(SearchContext context,
																						  String className) {
		/*return SearchPattern.createPattern(className, IJavaSearchConstants.TYPE,
				IJavaSearchConstants.FIELD_DECLARATION_TYPE_REFERENCE, SearchPattern.R_EXACT_MATCH);*/
		PsiClass templateClass = context.getUtils().findClass(context.getJavaProject(), className);
		if (templateClass != null) {
			return ReferencesSearch.search(templateClass, context.getJavaProject().getModuleWithDependenciesAndLibrariesScope(false));
		} else {
			return new EmptyQuery<>();
		}
	}

	/**
	 * Create a search pattern to retrieve IType which implement the given
	 * <code>interfaceName</code interface name.
	 *
	 * @param context the search context.
	 * @param interfaceName the interface name to search.
	 *
	 * @return a search pattern to retrieve IType which implement the given
	 *         <code>interfaceName</code interface name.
	 */
	protected static Query<PsiElement> createInterfaceImplementationSearchPattern(SearchContext context,
																				  String interfaceName) {
		PsiClass interfaceClass = context.getUtils().findClass(context.getJavaProject(), interfaceName);
		if (interfaceClass != null) {
			return DefinitionsScopedSearch.search(interfaceClass, context.getJavaProject().getModuleWithDependenciesAndLibrariesScope(false), false);
		} else {
			return new EmptyQuery<>();
		}
	}

	@Override
	public void endSearch(SearchContext context, ProgressIndicator monitor) {
		NamespaceResolverInfo info = getNamespaceResolverInfo();
		if (info != null) {
			// Register namespace information
			String namespacekey = info.getNamespaces().get(0);
			if (isNamespaceAvailable(namespacekey, context, monitor)) {
				Map<String, NamespaceResolverInfo> infos = context.getDataModelProject().getNamespaceResolverInfos();
				infos.put(namespacekey, info);
			}
		}
	}

	/**
	 * Returns true if the given namespace is available for the java project and
	 * false otherwise.
	 *
	 * @param namespace the namespace.
	 * @param context   the search context.
	 * @param monitor   the progress monitor.
	 *
	 * @return true if the given namespace is available for the java project and
	 *         false otherwise.
	 */
	protected boolean isNamespaceAvailable(String namespace, SearchContext context, ProgressIndicator monitor) {
		return true;
	}
}
