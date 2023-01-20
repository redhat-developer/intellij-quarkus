/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.utils;

import com.intellij.openapi.module.Module;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiNameValuePair;
import com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.ITypeResolver;
import com.redhat.qute.commons.ResolvedJavaTypeInfo;
import com.redhat.qute.commons.annotations.RegisterForReflectionAnnotation;
import com.redhat.qute.commons.annotations.TemplateDataAnnotation;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.REGISTER_FOR_REFLECTION_ANNOTATION_FIELDS;
import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.REGISTER_FOR_REFLECTION_ANNOTATION_METHODS;
import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.REGISTER_FOR_REFLECTION_ANNOTATION_TARGETS;
import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.TEMPLATE_DATA_ANNOTATION_IGNORE;
import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.TEMPLATE_DATA_ANNOTATION_IGNORE_SUPER_CLASSES;
import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.TEMPLATE_DATA_ANNOTATION_PROPERTIES;
import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.TEMPLATE_DATA_ANNOTATION_TARGET;
import static com.redhat.devtools.intellij.qute.psi.utils.AnnotationUtils.getValueAsArray;
import static com.redhat.devtools.intellij.qute.psi.utils.AnnotationUtils.getValueAsBoolean;
import static com.redhat.devtools.intellij.qute.psi.utils.AnnotationUtils.getValueAsString;

/**
 * Utilities for collecting @TemplateData and @RegisterForReflection.
 * 
 * @author Angelo ZERR
 *
 * @see <a href=
 *      "https://quarkus.io/guides/qute-reference#template_data">@TemplateData</a>
 * @see <a href=
 *      "https://quarkus.io/guides/writing-native-applications-tips#registerForReflection">Using
 *      the @RegisterForReflection annotation</a>
 */
public class QuteReflectionAnnotationUtils {

	private static final Logger LOGGER = Logger.getLogger(QuteReflectionAnnotationUtils.class.getName());
	private static final String TRUE_VALUE = "true";
	private static final String FALSE_VALUE = "false";

	/**
	 * Collect @TemplateData and @RegisterForReflection annotations from the given
	 * Java type.
	 *
	 * @param resolvedType the Java type to update.
	 * @param type         the JDT Java type.
	 * @param typeResolver the Java type resolver.
	 */
	public static void collectAnnotations(ResolvedJavaTypeInfo resolvedType, PsiClass type, ITypeResolver typeResolver,
										  Module javaProject) {
		List<TemplateDataAnnotation> templateDataAnnotations = null;
		RegisterForReflectionAnnotation registerForReflectionAnnotation = null;
		PsiAnnotation[] annotations = type.getAnnotations();
		for (PsiAnnotation annotation : annotations) {
			if (AnnotationUtils.isMatchAnnotation(annotation, QuteJavaConstants.TEMPLATE_DATA_ANNOTATION)) {
				// @TemplateData
				if (templateDataAnnotations == null) {
					templateDataAnnotations = new ArrayList<>();
				}
				templateDataAnnotations.add(createTemplateData(annotation, typeResolver, javaProject));
			} else if (AnnotationUtils.isMatchAnnotation(annotation,
					QuteJavaConstants.REGISTER_FOR_REFLECTION_ANNOTATION)) {
				// @RegisterForReflection
				registerForReflectionAnnotation = createRegisterForReflection(annotation, typeResolver, javaProject);
			}
		}
		resolvedType.setTemplateDataAnnotations(templateDataAnnotations);
		resolvedType.setRegisterForReflectionAnnotation(registerForReflectionAnnotation);
	}

	private static TemplateDataAnnotation createTemplateData(PsiAnnotation templateDataAnnotation,
															 ITypeResolver typeResolver, Module javaProject) {
		TemplateDataAnnotation templateData = new TemplateDataAnnotation();
		try {
			for(PsiNameValuePair pair : templateDataAnnotation.getParameterList().getAttributes()) {
				switch (pair.getAttributeName()) {
					case TEMPLATE_DATA_ANNOTATION_IGNORE_SUPER_CLASSES: {
						// @TemplateData(ignoreSuperclasses = true)
						// public class Item
						Boolean ignoreSuperclasses = getValueAsBoolean(pair);
						if (Boolean.TRUE.equals(ignoreSuperclasses)) {
							templateData.setIgnoreSuperclasses(ignoreSuperclasses);
						}
						break;
					}
					// @TemplateData/target
					case TEMPLATE_DATA_ANNOTATION_TARGET: {
						// @TemplateData(target = BigDecimal.class)
						// public class Item
						String target = getValueAsString(pair);
						if (target != null) {
							// here target is equals to "BigDecimal", we must resolve it to have
							// "java.math.BigDecimal"
							target = resolveTarget(target, typeResolver, javaProject);
							templateData.setTarget(target);
						}
						break;
					}

					// @TemplateData/ignore
					case TEMPLATE_DATA_ANNOTATION_IGNORE: {
						List<String> ignore = null;
						Object[] values = getValueAsArray(pair);
						if (values != null && values.length > 0) {
							ignore = new ArrayList<>(values.length);
							for (int i = 0; i < values.length; i++) {
								String ignoreItem = values[i] != null ? values[i].toString() : null;
								if (ignoreItem != null) {
									ignore.add(ignoreItem);
								}
							}
						}
						templateData.setIgnore(ignore);
						break;
					}

					// @TemplateData/properties
					case TEMPLATE_DATA_ANNOTATION_PROPERTIES: {
						// @TemplateData(properties = true)
						// public class Item
						Boolean properties = getValueAsBoolean(pair);
						if (Boolean.TRUE.equals(properties)) {
							templateData.setProperties(properties);
						}
						break;
					}
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING,
					"Error while getting member values of '" + templateDataAnnotation.getQualifiedName() + "'.", e);
		}
		return templateData;
	}

	private static RegisterForReflectionAnnotation createRegisterForReflection(
			PsiAnnotation registerForReflectionAnnotation, ITypeResolver typeResolver, Module javaProject) {
		RegisterForReflectionAnnotation registerForReflection = new RegisterForReflectionAnnotation();
		try {

			// Loop for attributes of the @RegisterForReflection annotation
			for (PsiNameValuePair pair : registerForReflectionAnnotation.getParameterList().getAttributes()) {
				switch (pair.getAttributeName()) {

					// @RegisterForReflection/methods
					case REGISTER_FOR_REFLECTION_ANNOTATION_METHODS: {
						// @RegisterForReflection(methods = false)
						// public class Item
						Boolean methods = getValueAsBoolean(pair);
						if (Boolean.FALSE.equals(methods)) {
							registerForReflection.setMethods(methods);
						}
						break;
					}

					// @RegisterForReflection/fields
					case REGISTER_FOR_REFLECTION_ANNOTATION_FIELDS: {
						// @RegisterForReflection(fields = false)
						// public class Item
						Boolean fields = getValueAsBoolean(pair);
						if (Boolean.FALSE.equals(fields)) {
							registerForReflection.setFields(fields);
						}
						break;
					}

					// @RegisterForReflection/targets
					case REGISTER_FOR_REFLECTION_ANNOTATION_TARGETS: {
						// @RegisterForReflection(targets = {BigDecimal.class, String.class})
						// public class Item
						List<String> targets = null;
						if (JavaPsiFacade.getInstance(pair.getProject()).getConstantEvaluationHelper().computeConstantExpression(pair.getValue()) instanceof Object[]) {
							Object[] values = getValueAsArray(pair);
							if (values != null && values.length > 0) {
								targets = new ArrayList<>(values.length);
								for (int i = 0; i < values.length; i++) {
									String target = values[i] != null ? values[i].toString() : null;
									if (target != null) {
										// here target is equals to "BigDecimal", we must resolve it to have
										// "java.math.BigDecimal"
										target = resolveTarget(target, typeResolver, javaProject);
										targets.add(target);
									}
								}
							}
						}
						registerForReflection.setTargets(targets);
						break;
					}

				}

			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING,
					"Error while getting member values of '" + registerForReflectionAnnotation.getQualifiedName() + "'.",
					e);
		}
		return registerForReflection;
	}

	private static String resolveTarget(String target, ITypeResolver typeResolver, Module javaProject) {
		//
		return typeResolver.resolveTypeSignature(target);
	}
}
