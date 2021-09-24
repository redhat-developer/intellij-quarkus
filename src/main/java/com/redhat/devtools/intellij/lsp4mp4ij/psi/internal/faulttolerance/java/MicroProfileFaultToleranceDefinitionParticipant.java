/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.java;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.definition.AbstractAnnotationDefinitionParticipant;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.definition.JavaDefinitionContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PositionUtils;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4mp.commons.MicroProfileDefinition;

import java.util.Arrays;
import java.util.List;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.MicroProfileFaultToleranceConstants.FALLBACK_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.MicroProfileFaultToleranceConstants.FALLBACK_METHOD_FALLBACK_ANNOTATION_MEMBER;

/**
 *
 * MicroProfile Fallback Tolerance Definition to navigate from Java
 * file @Fallback/fallbackMethod to the Java method name.
 *
 * @author Angelo ZERR
 *
 * @See https://github.com/eclipse/microprofile-config
 *
 */
public class MicroProfileFaultToleranceDefinitionParticipant extends AbstractAnnotationDefinitionParticipant {

	public MicroProfileFaultToleranceDefinitionParticipant() {
		super(FALLBACK_ANNOTATION, FALLBACK_METHOD_FALLBACK_ANNOTATION_MEMBER);
	}

	@Override
	protected List<MicroProfileDefinition> collectDefinitions(String annotationValue, Range annotationValueRange,
															  PsiAnnotation annotation, JavaDefinitionContext context) {
		PsiClass type = getOwnerType(annotation);
		if (type != null) {
			PsiFile typeRoot = context.getTypeRoot();
			IPsiUtils utils = context.getUtils();
			for (PsiMethod method : type.getMethods()) {
				if (annotationValue.equals(method.getName())) {
					Range methodNameRange = PositionUtils.toNameRange(method, utils);
					MicroProfileDefinition definition = new MicroProfileDefinition();
					LocationLink location = new LocationLink();
					definition.setLocation(location);
					location.setTargetUri(utils.toUri(typeRoot));
					location.setTargetRange(methodNameRange);
					location.setTargetSelectionRange(methodNameRange);
					location.setOriginSelectionRange(annotationValueRange);
					return Arrays.asList(definition);
				}
			}
		}
		return null;
	}

	private static PsiClass getOwnerType(PsiElement element) {
		while (element != null) {
			if (element instanceof PsiClass) {
				return (PsiClass) element;
			}
			element = element.getParent();
		}
		return null;
	}

	@Override
	protected boolean isAdaptableFor(PsiElement definitionElement) {
		return definitionElement instanceof PsiMethod;
	}
}
