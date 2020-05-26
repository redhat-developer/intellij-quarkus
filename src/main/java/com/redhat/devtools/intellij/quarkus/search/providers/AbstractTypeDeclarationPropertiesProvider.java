/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search.providers;


import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMember;
import com.redhat.devtools.intellij.quarkus.search.SearchContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for properties provider based on type declaration (class,
 * interface, annotation type, etc) search.
 * 
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/AbstractTypeDeclarationPropertiesProvider.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/AbstractTypeDeclarationPropertiesProvider.java</a>
 *
 */
public abstract class AbstractTypeDeclarationPropertiesProvider extends AbstractPropertiesProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTypeDeclarationPropertiesProvider.class);

	@Override
	protected String[] getPatterns() {
		return getTypeNames();
	}

	/**
	 * Returns the type names to search.
	 * 
	 * @return the type names to search.
	 */
	protected abstract String[] getTypeNames();

	@Override
	public void collectProperties(PsiMember match, SearchContext context) {
		if (match instanceof PsiClass) {
			PsiClass type = (PsiClass) match;
			String className = type.getQualifiedName();
			String[] names = getTypeNames();
			for (String name : names) {
				if (name.equals(className)) {
					try {
						// Collect properties from the class name and stop the loop.
						processClass(type, className, context);
						break;
					} catch (Exception e) {
						LOGGER.error("Cannot compute MicroProfile properties for the Java class '" + className + "'.",
								e);
					}
				}
			}
		}
	}

	protected abstract void processClass(PsiClass type, String className, SearchContext context);
}
