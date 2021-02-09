/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.util.Query;

/**
 * Properties provider API.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/IPropertiesProvider.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/IPropertiesProvider.java</a>
 */
public interface IPropertiesProvider {
	public static final ExtensionPointName<IPropertiesProvider> EP_NAME = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.propertiesProvider");

	/**
	 * Begin the search.
	 * 
	 * @param context the search context
	 */
	default void beginSearch(SearchContext context) {
	}

	/**
	 * End the search.
	 * 
	 * @param context the search context
	 */
	default void endSearch(SearchContext context) {
	}

	/**
	 * Create the search query.
	 * 
	 * @return the search query.
	 */
	Query<PsiModifierListOwner> createSearchPattern(SearchContext context);

	/**
	 * Collect properties from the given Java search match.
	 * 
	 * @param match   the java search match.
	 * @param context the search context.
	 */
	void collectProperties(PsiModifierListOwner match, SearchContext context);
}
