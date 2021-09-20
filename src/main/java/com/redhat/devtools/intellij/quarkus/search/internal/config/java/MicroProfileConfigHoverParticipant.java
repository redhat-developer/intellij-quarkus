/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
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
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.quarkus.search.core.project.MicroProfileConfigPropertyInformation;
import com.redhat.devtools.intellij.quarkus.search.core.project.PsiMicroProfileProject;
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

import java.util.List;

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
		PsiElement hoverElement = PsiTreeUtil.getParentOfType(context.getHoverElement(), PsiVariable.class);
		if (!(hoverElement instanceof PsiVariable)) {
			return null;
		}

		PsiFile typeRoot = context.getTypeRoot();
		IPsiUtils utils = context.getUtils();

		Position hoverPosition = context.getHoverPosition();
		PsiVariable hoverField = (PsiVariable) hoverElement;

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

		PsiMicroProfileProject mpProject = PsiMicroProfileProjectManager.getInstance()
				.getJDTMicroProfileProject(javaProject);
		List<MicroProfileConfigPropertyInformation> propertyInformation = getConfigPropertyInformation(propertyKey,
				annotation, mpProject);
		DocumentFormat documentFormat = context.getDocumentFormat();
		return new Hover(getDocumentation(propertyInformation, context.getDocumentFormat(), context.isSurroundEqualsWithSpaces()), propertyKeyRange);
	}

	/**
	 * Returns all the config property information for the given property key.
	 *
	 * Includes the information for all the different profiles.
	 *
	 * @param propertyKey              the property key without the profile
	 * @param configPropertyAnnotation the annotation that defines the config property
	 * @param project                  the project
	 * @return the config property information for the given property key
	 */
	public static List<MicroProfileConfigPropertyInformation> getConfigPropertyInformation(String propertyKey,
																						   PsiAnnotation configPropertyAnnotation, PsiMicroProfileProject project)  {

		List<MicroProfileConfigPropertyInformation> infos = project.getPropertyInformations(propertyKey);
		boolean defaultProfileDefined = false;

		for (MicroProfileConfigPropertyInformation info : infos) {
			if (info.getPropertyNameWithProfile().equals(propertyKey)) {
				defaultProfileDefined = true;
			}
		}

		if (!defaultProfileDefined) {
			infos.add(new MicroProfileConfigPropertyInformation(propertyKey,
					getAnnotationMemberValue(configPropertyAnnotation, CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE),
					configPropertyAnnotation.getContainingFile().getName()));
		}

		return infos;
	}

	/**
	 * Returns documentation about the property keys and values provided in
	 * <code>propertyMap</code>
	 *
	 * @param propertyInformation the microprofile property information
	 * @param documentFormat      the document format
	 * @param insertSpacing       true if spacing should be inserted around the
	 *                            equals sign and false otherwise
	 *
	 * @return documentation about the property keys and values provided in
	 *         <code>propertyMap</code>
	 */
	public static MarkupContent getDocumentation(List<MicroProfileConfigPropertyInformation> propertyInformation,
												 DocumentFormat documentFormat, boolean insertSpacing) {
		StringBuilder content = new StringBuilder();

		boolean markdown = DocumentFormat.Markdown.equals(documentFormat);
		buildDocumentation(propertyInformation, markdown, insertSpacing, content);
		return new MarkupContent(markdown ? MarkupKind.MARKDOWN : MarkupKind.PLAINTEXT, content.toString());
	}

	private static void buildDocumentation(List<MicroProfileConfigPropertyInformation> propertyInformation,
										   boolean markdownSupported, boolean insertSpacing, StringBuilder content) {

		for (MicroProfileConfigPropertyInformation info : propertyInformation) {

			if (content.length() > 0) {
				content.append("  \n");
			}

			if (markdownSupported) {
				content.append("`");
			}

			content.append(info.getPropertyNameWithProfile());

			if (info.getValue() == null) {
				if (markdownSupported) {
					content.append("`");
				}
				content.append(" is not set");
			} else {
				if (insertSpacing) {
					content.append(" = ");
				} else {
					content.append("=");
				}
				content.append(info.getValue());
				if (markdownSupported) {
					content.append("`");
				}
				if (info.getConfigFileName() != null) {
					content.append(" ");
					if (markdownSupported) {
						content.append("*");
					}
					content.append("in");
					if (markdownSupported) {
						content.append("*");
					}
					content.append(" ");
					content.append(info.getConfigFileName());
				}
			}
		}
	}
}
