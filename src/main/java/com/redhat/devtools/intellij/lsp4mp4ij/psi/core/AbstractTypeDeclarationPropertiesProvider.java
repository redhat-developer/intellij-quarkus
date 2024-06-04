/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core;


import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifierListOwner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;

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
	public void collectProperties(PsiModifierListOwner match, SearchContext context) {
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
					} catch (ProcessCanceledException e) {
						//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
						//TODO delete block when minimum required version is 2024.2
						throw e;
					} catch (IndexNotReadyException | CancellationException e) {
						throw e;
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
