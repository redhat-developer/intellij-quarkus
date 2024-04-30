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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.hover;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotation;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotationMemberValue;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiParameter;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.MicroProfileConfigPropertyInformation;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProject;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.util.Ranges;
import org.eclipse.lsp4mp.commons.DocumentFormat;

/**
 *
 * Properties hover participant to hover properties values declared in
 * properties files.
 *
 * @author Angelo ZERR
 *
 */
public class PropertiesHoverParticipant implements IJavaHoverParticipant {

	private final String annotationName;

	private final String defaultValueAnnotationMemberName;

	private final String[] annotationMembers;

	private Function<String, String> propertyReplacer;

	public PropertiesHoverParticipant(String annotationName, String annotationMemberName) {
		this(annotationName, annotationMemberName, null);
	}

	/**
	 * The definition participant constructor.
	 *
	 * @param annotationName                   the annotation name (ex :
	 *                                         org.eclipse.microprofile.config.inject.ConfigProperty)
	 * @param annotationMemberName             the annotation member name (ex :
	 *                                         name)
	 * @param defaultValueAnnotationMemberName the annotation member name for
	 *                                         default value and null otherwise.
	 */
	public PropertiesHoverParticipant(String annotationName, String annotationMemberName,
									  String defaultValueAnnotationMemberName) {
		this(annotationName, new String[] {
				annotationMemberName
		}, defaultValueAnnotationMemberName);
	}

	/**
	 * The definition participant constructor.
	 *
	 * @param annotationName                   the annotation name (ex :
	 *                                         io.quarkus.scheduler.Scheduled)
	 * @param annotationMembers                a list of annotation members (ex :
	 *                                         cron, every, etc.
	 * @param defaultValueAnnotationMemberName the annotation member name for
	 *                                         default value and null otherwise.
	 *
	 */
	public PropertiesHoverParticipant(String annotationName, String[] annotationMembers,
									  String defaultValueAnnotationMemberName) {
		this.annotationName = annotationName;
		this.annotationMembers = annotationMembers;
		this.defaultValueAnnotationMemberName = defaultValueAnnotationMemberName;
	}

	/**
	 * The definition participant constructor with a property replacer.
	 *
	 * @param annotationName                   the annotation name (ex :
	 *                                         io.quarkus.scheduler.Scheduled)
	 * @param annotationMembers                a list of annotation members (ex :
	 *                                         cron, every, etc.
	 * @param defaultValueAnnotationMemberName the annotation member name for
	 *                                         default value and null otherwise.
	 * @param propertyReplacer                 the replacer function for property
	 *                                         expressions
	 */
	public PropertiesHoverParticipant(String annotationName, String[] annotationMembers,
									  String defaultValueAnnotationMemberName, Function<String, String> propertyReplacer) {
		this(annotationName, annotationMembers, defaultValueAnnotationMemberName);
		this.propertyReplacer = propertyReplacer;
	}

	@Override
	public boolean isAdaptedForHover(JavaHoverContext context) {
		// Definition is done only if the annotation is on the classpath
		Module javaProject = context.getJavaProject();
		return PsiTypeUtils.findType(javaProject, annotationName) != null;
	}

	@Override
	public Hover collectHover(JavaHoverContext context) {
		PsiElement hoverElement = context.getHoverElement();
		if (!isAdaptableFor(hoverElement)) {
			return null;
		}

		PsiFile typeRoot = context.getTypeRoot();
		IPsiUtils utils = context.getUtils();

		Position hoverPosition = context.getHoverPosition();
		PsiElement hoverField = (PsiElement) hoverElement;

		PsiAnnotation annotation = getAnnotation(hoverField, annotationName);

		if (annotation == null) {
			return null;
		}

		String annotationSource = annotation.getText();
		String propertyKey = null;
		Range propertyKeyRange = null;
		boolean found = false;
		for (String annotationMemberName : annotationMembers) {
			propertyKey = getAnnotationMemberValue(annotation, annotationMemberName);
			if (propertyKey != null) {
				TextRange r = annotation.getTextRange();
				Pattern memberPattern = Pattern.compile(".*[^\"]\\s*(" + annotationMemberName + ")\\s*=.*",
						Pattern.DOTALL);
				Matcher match = memberPattern.matcher(annotationSource);
				if (match.matches()) {
					int offset = annotationSource.indexOf(propertyKey);
					propertyKeyRange = utils.toRange(typeRoot, r.getStartOffset() + offset, propertyKey.length());

					if (!hoverPosition.equals(propertyKeyRange.getEnd())
							&& Ranges.containsPosition(propertyKeyRange, hoverPosition)) {
						found = true;
						break;
					}
				}
			}
		}

		if (!found) {
			return null;
		}

		Module javaProject = context.getJavaProject();

		if (javaProject == null) {
			return null;
		}

		String defaultValue = null;
		// property references may be surrounded by curly braces in Java file
		if (propertyReplacer != null) {
			propertyKey = propertyReplacer.apply(propertyKey);
			int colonIdx = propertyKey.indexOf(":");
			if (colonIdx > -1) {
				defaultValue = propertyKey.substring(colonIdx+1);
				propertyKey = propertyKey.substring(0,colonIdx);
			}
		}

		String defaultAnnotationValue = getAnnotationMemberValue(annotation, defaultValueAnnotationMemberName);

		PsiMicroProfileProject mpProject = PsiMicroProfileProjectManager.getInstance(javaProject.getProject())
				.getMicroProfileProject(javaProject);
		List<MicroProfileConfigPropertyInformation> propertyInformation = getConfigPropertyInformation(propertyKey,
				annotation,
				defaultAnnotationValue == null? defaultValue : defaultAnnotationValue,
				typeRoot, mpProject, utils);
		return new Hover(getDocumentation(propertyInformation, context.getDocumentFormat(),
				context.isSurroundEqualsWithSpaces()), propertyKeyRange);
	}

	/**
	 * Returns true if the given hovered Java element is adapted for this
	 * participant and false otherwise.
	 * 
	 * <p>
	 * 
	 * By default this method returns true if the hovered annotation belongs to a
	 * Java field or local variable and false otherwise.
	 * 
	 * </p>
	 * 
	 * @param hoverElement the hovered Java element.
	 * 
	 * @return true if the given hovered Java element is adapted for this
	 *         participant and false otherwise.
	 */
	protected boolean isAdaptableFor(PsiElement hoverElement) {
		return hoverElement instanceof PsiField
				|| hoverElement instanceof PsiParameter;
	}

	/**
	 * Returns all the config property information for the given property key.
	 * <p>
	 * Includes the information for all the different profiles.
	 *
	 * @param propertyKey  the property key without the profile
	 * @param annotation   the annotation that defines the
	 *                     config property
	 * @param defaultValue default value and null otherwise.
	 * @param project      the project
	 * @param utils        the JDT LS utilities.
	 * @return the config property information for the given property key
	 */
	private static List<MicroProfileConfigPropertyInformation> getConfigPropertyInformation(String propertyKey,
																							PsiAnnotation annotation,
																							String defaultValue,
																							PsiFile typeRoot,
																							PsiMicroProfileProject project,
																							IPsiUtils utils) {

		List<MicroProfileConfigPropertyInformation> infos = project.getPropertyInformations(propertyKey);
		boolean defaultProfileDefined = infos.stream().anyMatch(info -> propertyKey.equals(info.getPropertyNameWithProfile()));

		if (!defaultProfileDefined) {
			infos.add(new MicroProfileConfigPropertyInformation(propertyKey,
					defaultValue, utils.toUri(typeRoot),
					annotation.getContainingFile().getName()));
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
	private static MarkupContent getDocumentation(List<MicroProfileConfigPropertyInformation> propertyInformation,
			DocumentFormat documentFormat, boolean insertSpacing) {
		StringBuilder content = new StringBuilder();

		boolean markdown = DocumentFormat.Markdown.equals(documentFormat);
		buildDocumentation(propertyInformation, markdown, insertSpacing, content);
		return new MarkupContent(markdown ? MarkupKind.MARKDOWN : MarkupKind.PLAINTEXT, content.toString());
	}

	private static void buildDocumentation(List<MicroProfileConfigPropertyInformation> propertyInformation,
			boolean markdownSupported, boolean insertSpacing, StringBuilder content) {

		for (MicroProfileConfigPropertyInformation info : propertyInformation) {

			if (!content.isEmpty()) {
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
					String href = info.getSourceConfigFileURI();
					if (markdownSupported && href != null) {
						content.append("[");
					}
					content.append(info.getConfigFileName());
					if (markdownSupported && href != null) {
						content.append("]");
						content.append("(");
						content.append(href);
						content.append(")");
					}
				}
			}
		}
	}
}
