/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.definition;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLocalVariable;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.util.Ranges;
import org.eclipse.lsp4mp.commons.MicroProfileDefinition;

import java.util.List;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotation;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotationMemberValue;


/**
 *
 * Abstract class for collecting Java definition participant from a given Java
 * annotation member.
 *
 * @author Angelo ZERR
 *
 */
public abstract class AbstractAnnotationDefinitionParticipant implements IJavaDefinitionParticipant {

	private final String annotationName;

	private final String annotationMemberName;

	/**
	 * The definition participant constructor.
	 * 
	 * @param annotationName       the annotation name (ex :
	 *                             org.eclipse.microprofile.config.inject.ConfigProperty)
	 * @param annotationMemberName the annotation member name (ex : name)
	 */
	public AbstractAnnotationDefinitionParticipant(String annotationName, String annotationMemberName) {
		this.annotationName = annotationName;
		this.annotationMemberName = annotationMemberName;
	}

	@Override
	public boolean isAdaptedForDefinition(JavaDefinitionContext context) {
		// Definition is done only if the annotation is on the classpath
		Module javaProject = context.getJavaProject();
		return PsiTypeUtils.findType(javaProject, annotationName) != null;
	}

	@Override
	public List<MicroProfileDefinition> collectDefinitions(JavaDefinitionContext context) {
		PsiFile typeRoot = context.getTypeRoot();
		Module javaProject = context.getJavaProject();
		if (javaProject == null) {
			return null;
		}

		// Get the hyperlinked element.
		// If user hyperlinks an annotation, member annotation which is bound a Java
		// field, the hyperlinked Java element is the Java field (not the member or the
		// annotation).
		PsiElement hyperlinkedElement = context.getHyperlinkedElement();
		if (!isAdaptableFor(hyperlinkedElement)) {
			return null;
		}

		Position definitionPosition = context.getHyperlinkedPosition();

		// Try to get the annotation
		PsiAnnotation annotation = getAnnotation(hyperlinkedElement, annotationName);

		if (annotation == null) {
			return null;
		}

		// Try to get the annotation member value
		String annotationSource = annotation.getText();
		String annotationMemberValue = getAnnotationMemberValue(annotation, annotationMemberName);

		if (annotationMemberValue == null) {
			return null;
		}

		// Get the annotation member value range
		TextRange r = annotation.getTextRange();
		int offset = annotationSource.indexOf(annotationMemberValue);
		IPsiUtils utils = context.getUtils();
		final Range annotationMemberValueRange = utils.toRange(typeRoot, r.getStartOffset() + offset,
				annotationMemberValue.length());

		if (definitionPosition.equals(annotationMemberValueRange.getEnd())
				|| !Ranges.containsPosition(annotationMemberValueRange, definitionPosition)) {
			return null;
		}

		// Collect definitions
		return collectDefinitions(annotationMemberValue, annotationMemberValueRange, annotation, context);
	}

	/**
	 * Returns true if the given hyperlinked Java element is adapted for this
	 * participant and false otherwise.
	 * 
	 * <p>
	 * 
	 * By default this method returns true if the hyperlinked annotation belongs to
	 * a Java field or local variable and false otherwise.
	 * 
	 * </p>
	 * 
	 * @param hyperlinkedElement the hyperlinked Java element.
	 * 
	 * @return true if the given hyperlinked Java element is adapted for this
	 *         participant and false otherwise.
	 */
	protected boolean isAdaptableFor(PsiElement hyperlinkedElement) {
		return hyperlinkedElement instanceof PsiField
				|| hyperlinkedElement instanceof PsiLocalVariable;
	}

	/**
	 * Returns the definitions for the given annotation member value and null
	 * otherwise.
	 * 
	 * @param annotationMemberValue      the annotation member value content.
	 * @param annotationMemberValueRange the annotation member value range.
	 * @param annotation                 the hyperlinked annotation.
	 * @param context                    the definition context.
	 * @return the definitions for the given annotation member value and null
	 *         otherwise.
	 */
	protected abstract List<MicroProfileDefinition> collectDefinitions(String annotationMemberValue,
			Range annotationMemberValueRange, PsiAnnotation annotation, JavaDefinitionContext context);
}
