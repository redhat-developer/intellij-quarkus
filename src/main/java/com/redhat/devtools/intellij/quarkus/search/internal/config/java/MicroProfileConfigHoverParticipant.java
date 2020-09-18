/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search.internal.config.java;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.quarkus.search.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.quarkus.search.core.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.quarkus.search.core.java.hover.IJavaHoverParticipant;
import com.redhat.devtools.intellij.quarkus.search.core.java.hover.JavaHoverContext;
import com.redhat.devtools.intellij.quarkus.search.core.project.PsiMicroProfileProjectManager;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.util.Ranges;

import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CONFIG_PROPERTY_ANNOTATION;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CONFIG_PROPERTY_ANNOTATION_NAME;
import static com.redhat.devtools.intellij.quarkus.search.core.utils.AnnotationUtils.getAnnotation;
import static com.redhat.devtools.intellij.quarkus.search.core.utils.AnnotationUtils.getAnnotationMemberValue;

/**
 *
 * MicroProfile Config Hover
 * 
 * @author Angelo ZERR
 * 
 * @see <a href="https://github.com/eclipse/microprofile-config">https://github.com/eclipse/microprofile-config</a>
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/config/java/MicroProfileConfigHoverParticipant.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/config/java/MicroProfileConfigHoverParticipant.java</a>
 *
 */
public class MicroProfileConfigHoverParticipant implements IJavaHoverParticipant {

	@Override
	public boolean isAdaptedForHover(JavaHoverContext context) {
		// Hover is done only if microprofile-config is on the classpath
		Module javaProject = context.getJavaProject();
		return PsiTypeUtils.findType(javaProject, CONFIG_PROPERTY_ANNOTATION) != null;
	}

	@Override
	public Hover collectHover(JavaHoverContext context) {
		PsiElement hoverElement = PsiTreeUtil.getParentOfType(context.getHoverElement(), PsiField.class);
		if (!(hoverElement instanceof PsiField)) {
			return null;
		}

		PsiFile typeRoot = context.getTypeRoot();
		IPsiUtils utils = context.getUtils();

		Position hoverPosition = context.getHoverPosition();
		PsiField hoverField = (PsiField) hoverElement;

		PsiAnnotation annotation = getAnnotation(hoverField, CONFIG_PROPERTY_ANNOTATION);

		if (annotation == null) {
			return null;
		}

		String annotationSource = annotation.getText();
		String propertyKey = getAnnotationMemberValue(annotation, CONFIG_PROPERTY_ANNOTATION_NAME);

		if (propertyKey == null) {
			return null;
		}

		TextRange r = annotation.getTextRange();
		int offset = annotationSource.indexOf(propertyKey);
		final Range propertyKeyRange = utils.toRange(typeRoot, r.getStartOffset() + offset, propertyKey.length());

		if (hoverPosition.equals(propertyKeyRange.getEnd())
				|| !Ranges.containsPosition(propertyKeyRange, hoverPosition)) {
			return null;
		}

		Module javaProject = context.getJavaProject();

		if (javaProject == null) {
			return null;
		}

		String propertyValue = PsiMicroProfileProjectManager.getInstance().getJDTMicroProfileProject(javaProject)
				.getProperty(propertyKey, null);
		if (propertyValue == null) {
			propertyValue = getAnnotationMemberValue(annotation, CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE);
			if (propertyValue != null && propertyValue.length() == 0) {
				propertyValue = null;
			}
		}
		DocumentFormat documentFormat = context.getDocumentFormat();
		return new Hover(getDocumentation(propertyKey, propertyValue, documentFormat, true), propertyKeyRange);
	}

	/**
	 * Returns documentation about the provided <code>propertyKey</code>'s value,
	 * <code>propertyValue</code>
	 * 
	 * @param propertyKey   the property key
	 * @param propertyValue the property key's value
	 * @param documentFormat      documentation format (markdown/text)
	 * @param insertSpacing true if spacing should be inserted around the equals
	 *                      sign and false otherwise
	 * @return
	 */
	public static MarkupContent getDocumentation(String propertyKey, String propertyValue,
			DocumentFormat documentFormat, boolean insertSpacing) {
		boolean markdown = DocumentFormat.Markdown.equals(documentFormat);
		StringBuilder content = new StringBuilder();

		if (markdown) {
			content.append("`");
		}

		content.append(propertyKey);

		if (propertyValue == null) {
			if (markdown) {
				content.append("`");
			}
			content.append(" is not set.");
		} else {
			if (insertSpacing) {
				content.append(" = ");
			} else {
				content.append("=");
			}
			content.append(propertyValue);
			if (markdown) {
				content.append("`");
			}
		}
		return new MarkupContent(markdown ? MarkupKind.MARKDOWN : MarkupKind.PLAINTEXT, content.toString());
	}
}
