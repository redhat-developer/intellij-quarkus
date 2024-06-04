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

import static com.redhat.devtools.intellij.qute.psi.utils.AnnotationUtils.isMatchAnnotation;

import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationOwner;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.util.Query;

/**
 * Abstract class for data model provider based on annotation search.
 *
 * @author Angelo ZERR
 */
public abstract class AbstractAnnotationTypeReferenceDataModelProvider extends AbstractDataModelProvider {

    private static final Logger LOGGER = Logger
            .getLogger(AbstractAnnotationTypeReferenceDataModelProvider.class.getName());

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
    protected Query<? extends Object> createSearchPattern(SearchContext context, String annotationName) {
        return createAnnotationTypeReferenceSearchPattern(context, annotationName);
    }

    @Override
    public void collectDataModel(Object match, SearchContext context, ProgressIndicator monitor) {
        PsiElement javaElement = null;
        try {
            Object element = getMatchedElement(match);
            if (element instanceof PsiAnnotation) {
                // ex : for Local variable
                PsiAnnotation annotation = ((PsiAnnotation) element);
                javaElement = (PsiElement) annotation.getOwner();
                processAnnotation(javaElement, context, monitor, annotation);
            } else if (element instanceof PsiAnnotationOwner || element instanceof PsiModifierListOwner) {
                javaElement = (PsiElement) element;
                processAnnotation(javaElement, context, monitor);
            }
        } catch (ProcessCanceledException e) {
            //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
            //TODO delete block when minimum required version is 2024.2
            throw e;
        } catch (IndexNotReadyException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING,
                        "Cannot collect Qute data model for the Java element '" + javaElement != null
                                ? javaElement.getText()
                                : match.toString() + "'.",
                        e);
            }
        }
    }

    /**
     * Return the element associated with the given <code>match</code> and null
     * otherwise
     *
     * @param match the match
     * @return
     */
    private static Object getMatchedElement(Object match) {
        return match;
    }

    /**
     * Processes the annotations bound to the current <code>javaElement</code> and
     * adds item metadata if needed
     *
     * @param javaElement the Java element
     * @param context     the context
     * @param monitor     the monitor
     */
    protected void processAnnotation(PsiElement javaElement, SearchContext context, ProgressIndicator monitor) {
        PsiAnnotation[] annotations = javaElement instanceof PsiAnnotationOwner ? ((PsiAnnotationOwner) javaElement).getAnnotations() : ((PsiModifierListOwner) javaElement).getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            processAnnotation(javaElement, context, monitor, annotation);
        }
    }

    /**
     * Processes the current <code>annotation</code> bound to current
     * <code>javaElement</code> and adds item metadata if needed
     *
     * @param javaElement the Java element
     * @param context     the context
     * @param monitor     the monitor
     * @param annotation  the annotation
     */
    private void processAnnotation(PsiElement javaElement, SearchContext context, ProgressIndicator monitor,
                                   PsiAnnotation annotation) {
        String[] names = getAnnotationNames();
        for (String annotationName : names) {
            if (isMatchAnnotation(annotation, annotationName)) {
                processAnnotation(javaElement, annotation, annotationName, context, monitor);
                break;
            }
        }
    }

    protected abstract void processAnnotation(PsiElement javaElement, PsiAnnotation annotation, String annotationName,
                                              SearchContext context, ProgressIndicator monitor);

}
