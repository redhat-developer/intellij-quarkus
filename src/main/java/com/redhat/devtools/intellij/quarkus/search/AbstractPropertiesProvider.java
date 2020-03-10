/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search;


import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiMember;
import com.intellij.psi.impl.source.PsiClassImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.ClassUtil;
import com.intellij.util.ArrayQuery;
import com.intellij.util.EmptyQuery;
import com.intellij.util.MergeQuery;
import com.intellij.util.Query;
import com.redhat.microprofile.commons.metadata.ItemHint;
import com.redhat.microprofile.commons.metadata.ItemHint.ValueHint;
import com.redhat.microprofile.commons.metadata.ItemMetadata;

/**
 * Abstract class for properties provider.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/AbstractPropertiesProvider.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/AbstractPropertiesProvider.java</a>
 */
public abstract class AbstractPropertiesProvider implements IPropertiesProvider {

	/**
	 * Returns the Java search pattern.
	 * 
	 * @return the Java search pattern.
	 */
	protected abstract String[] getPatterns();

	/**
	 * Return an instance of search pattern.
	 */
	public Query<PsiMember> createSearchQuery(SearchContext context) {
		Query<PsiMember> query = null;
		String[] patterns = getPatterns();
		for (String pattern : patterns) {
			if (query == null) {
				query = createSearchQuery(context, pattern);
			} else {
				Query<PsiMember> rightQuery = createSearchQuery(context, pattern);
				if (rightQuery != null) {
					query = new MergeQuery<>(query, rightQuery);
				}
			}
		}
		return query;
	}

	/**
	 * Create an instance of search pattern with the given <code>pattern</code>.
	 * 
	 * @param pattern the search pattern
	 * @return an instance of search pattern with the given <code>pattern</code>.
	 */
	protected abstract Query<PsiMember> createSearchQuery(SearchContext context, String pattern);

	/**
	 * Create a search pattern for the given <code>annotationName</code> annotation
	 * name.
	 * 
	 * @param annotationName the annotation name to search.
	 * @return a search pattern for the given <code>annotationName</code> annotation
	 *         name.
	 */
	protected static Query<PsiMember> createAnnotationTypeReferenceSearchPattern(SearchContext context, String annotationName) {
		JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(context.getModule().getProject());
		PsiClass annotationClass = javaPsiFacade.findClass(annotationName, GlobalSearchScope.allScope(context.getModule().getProject()));
		if (annotationClass != null) {
			return AnnotatedElementsSearch.searchElements(annotationClass, context.getScope(), PsiMember.class);
		} else {
			return new EmptyQuery<>();
		}
	}

	/**
	 * Create a search pattern for the given <code>className</code> class name.
	 * 
	 * @param annotationName the class name to search.
	 * @return a search pattern for the given <code>className</code> class name.
	 */
	protected static Query<PsiClass> createAnnotationTypeDeclarationSearchPattern(SearchContext context, String annotationName) {
		JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(context.getModule().getProject());
		PsiClass annotationClass = javaPsiFacade.findClass(annotationName, GlobalSearchScope.allScope(context.getModule().getProject()));
		return new ArrayQuery<>(annotationClass);
	}

	/**
	 * Add item metadata.
	 * 
	 * @param collector     the properties collector.
	 * @param name          the property name.
	 * @param type          the type of the property.
	 * @param description   the description of the property.
	 * @param sourceType    the source type (class or interface) of the property.
	 * @param sourceField   the source field (field name) and null otherwise.
	 * @param sourceMethod  the source method (signature method) and null otherwise.
	 * @param defaultValue  the default vaue and null otherwise.
	 * @param extensionName the extension name and null otherwise.
	 * @param binary        true if the property comes from a JAR and false
	 *                      otherwise.
	 * @param phase         teh Quarkus config phase.
	 * @return the item metadata.
	 */
	protected ItemMetadata addItemMetadata(IPropertiesCollector collector, String name, String type, String description,
			String sourceType, String sourceField, String sourceMethod, String defaultValue, String extensionName,
			boolean binary, int phase) {
		return collector.addItemMetadata(name, type, description, sourceType, sourceField, sourceMethod, defaultValue,
				extensionName, binary, phase);
	}

	/**
	 * Add item metadata.
	 * 
	 * @param collector     the properties collector.
	 * @param name          the property name.
	 * @param type          the type of the property.
	 * @param description   the description of the property.
	 * @param sourceType    the source type (class or interface) of the property.
	 * @param sourceField   the source field (field name) and null otherwise.
	 * @param sourceMethod  the source method (signature method) and null otherwise.
	 * @param defaultValue  the default vaue and null otherwise.
	 * @param extensionName the extension name and null otherwise.
	 * @param binary        true if the property comes from a JAR and false
	 *                      otherwise.
	 * @return the item metadata.
	 */
	protected ItemMetadata addItemMetadata(IPropertiesCollector collector, String name, String type, String description,
			String sourceType, String sourceField, String sourceMethod, String defaultValue, String extensionName,
			boolean binary) {
		return addItemMetadata(collector, name, type, description, sourceType, sourceField, sourceMethod, defaultValue,
				extensionName, binary, 0);
	}

	/**
	 * Get or create the update hint from the given type.
	 * 
	 * @param collector
	 * @param type      the type.
	 * @return the hint name.
	 */
	protected String updateHint(IPropertiesCollector collector, PsiClass type) {
		if (type == null) {
			return null;
		}
		if (type.isEnum()) {
			// Register Enumeration in "hints" section
			String hint = ClassUtil.getJVMClassName(type);
			if (!collector.hasItemHint(hint)) {
				ItemHint itemHint = collector.getItemHint(hint);
				itemHint.setSourceType(hint);
				if (type instanceof PsiClassImpl) {
					itemHint.setSource(Boolean.TRUE);
				}
				PsiElement[] children = type.getChildren();
				for (PsiElement c : children) {
					if (c instanceof PsiEnumConstant) {
						String enumName = ((PsiEnumConstant) c).getName();
						// TODO: extract Javadoc
						String description = null;
						ValueHint value = new ValueHint();
						value.setValue(enumName);
						itemHint.getValues().add(value);
					}
				}
			}
			return hint;
		}
		return null;
	}
}
