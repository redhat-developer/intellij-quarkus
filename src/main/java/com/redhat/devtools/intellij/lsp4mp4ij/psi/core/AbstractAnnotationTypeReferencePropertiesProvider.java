/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
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
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.util.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.isMatchAnnotation;

/**
 * Abstract class for properties provider based on annotation search.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/AbstractAnnotationTypeReferencePropertiesProvider.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/AbstractAnnotationTypeReferencePropertiesProvider.java</a>
 */
public abstract class AbstractAnnotationTypeReferencePropertiesProvider extends AbstractPropertiesProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAnnotationTypeReferencePropertiesProvider.class.getName());

	@Override
	protected String[] getPatterns() {
		return getAnnotationNames();
	}

	/**
	 * Returns the annotation names to search.
	 * 
	 * @return the annotation names to search.
	 */
	protected abstract String[] getAnnotationNames();

	@Override
	protected Query<PsiModifierListOwner> createSearchPattern(SearchContext context, String annotationName) {
		return createAnnotationTypeReferenceSearchPattern(context, annotationName);
	}

	@Override
	public void collectProperties(PsiModifierListOwner match, SearchContext context) {
		processAnnotation(match, context);
	}

	protected void processAnnotation(PsiModifierListOwner psiElement, SearchContext context) {
		try {
			String[] names = getAnnotationNames();
			PsiAnnotation[] annotations = psiElement.getAnnotations();
			for (PsiAnnotation annotation : annotations) {
				for (String annotationName : names) {
					if (isMatchAnnotation(annotation, annotationName)) {
						processAnnotation(psiElement, annotation, annotationName, context);
						break;
					}
				}
			}
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
				LOGGER.error("Cannot compute MicroProfile properties for the Java element '"
						+ psiElement + "'.", e);
		}
	}

	protected abstract void processAnnotation(PsiModifierListOwner psiElement, PsiAnnotation annotation, String annotationName,
			SearchContext context);
}
