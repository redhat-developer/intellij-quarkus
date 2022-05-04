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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.config.properties;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiModifierListOwner;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.SearchContext;

import java.util.HashSet;
import java.util.Set;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.CONFIG_PROPERTIES_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.CONFIG_PROPERTIES_ANNOTATION_PREFIX;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.CONFIG_PROPERTIES_ANNOTATION_UNCONFIGURED_PREFIX;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotation;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotationMemberValue;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.findType;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getResolvedTypeName;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.isSimpleFieldType;

/**
 * Properties provider to collect MicroProfile properties from the Java fields
 * annotated with "org.eclipse.microprofile.config.inject.ConfigProperties"
 * annotation.
 * 
 * <code>
 * &#64;ConfigProperties(prefix="server")
&#64;Dependent
public class Details {
    public String host; // the value of the configuration property server.host
    public int port;   // the value of the configuration property server.port
    private String endpoint; //the value of the configuration property server.endpoint
    public @ConfigProperty(name="old.location")
    String location; //the value of the configuration property server.old.location
    ...
 * </code>
 * 
 * @author Angelo ZERR
 * 
 * @see <a href="https://download.eclipse.org/microprofile/microprofile-config-2.0/microprofile-config-spec-2.0.html#_aggregate_related_properties_into_a_cdi_bean">https://download.eclipse.org/microprofile/microprofile-config-2.0/microprofile-config-spec-2.0.html#_aggregate_related_properties_into_a_cdi_bean</a>
 * @see <a href="https://github.com/eclipse/microprofile-config/blob/master/api/src/main/java/org/eclipse/microprofile/config/inject/ConfigProperties.java">https://github.com/eclipse/microprofile-config/blob/master/api/src/main/java/org/eclipse/microprofile/config/inject/ConfigProperties.java</a>
 */
public class MicroProfileConfigPropertiesProvider extends MicroProfileConfigPropertyProvider {

	private static final String[] ANNOTATION_NAMES = { CONFIG_PROPERTIES_ANNOTATION };

	@Override
	protected String[] getAnnotationNames() {
		return ANNOTATION_NAMES;
	}

	@Override
	protected void processAnnotation(PsiModifierListOwner javaElement, PsiAnnotation configPropertiesAnnotation,
									 String annotationName, SearchContext context) {
		if (javaElement instanceof PsiClass) {
			// @ConfigProperties(prefix = "server3")
			// public class ServerConfigProperties
			generatePropertiesFromClassType((PsiClass) javaElement, configPropertiesAnnotation, context);
		} else if (javaElement instanceof PsiField) {
			// @ConfigProperties(prefix = "cloud")
			// ServerConfigProperties configPropertiesCloud;
			generatePropertiesFromField((PsiField) javaElement, configPropertiesAnnotation, context);
		}
	}

	/**
	 * Generate properties from the given class type annotated
	 * with @ConfigProperties.
	 * 
	 * <code>
	 * &#64;ConfigProperties(prefix = "server3")
	   public class ServerConfigProperties
	 * </code>
	 * 
	 * @param classType                  the class type.
	 * @param configPropertiesAnnotation the @ConfigProperties annotation.
	 * @param context                    the search context.
	 */
	private void generatePropertiesFromClassType(PsiClass classType, PsiAnnotation configPropertiesAnnotation,
			SearchContext context) {
		String prefix = getPrefixFromAnnotation(configPropertiesAnnotation);
		populateConfigObject(classType, prefix, new HashSet<>(), context);
	}

	/**
	 * Generate properties from the given field annotated with @ConfigProperties.
	 * 
	 * <code>
	 * &#64;ConfigProperties(prefix = "cloud")
	 * ServerConfigProperties configPropertiesCloud;
	 * </code>
	 * 
	 * @param field                      the Java field.
	 * @param configPropertiesAnnotation the @ConfigProperties annotation.
	 * @param context                    the search context.
	 */
	private void generatePropertiesFromField(PsiField field, PsiAnnotation configPropertiesAnnotation,
			SearchContext context) {

		String fieldTypeName = getResolvedTypeName(field);
		PsiClass fieldType = findType(field.getManager(), fieldTypeName);
		if (isSimpleFieldType(fieldType, fieldTypeName)) {
			return;
		}

		String prefix = getPrefixFromAnnotation(configPropertiesAnnotation);
		if (prefix == null) {
			// @ConfigProperties
			// ServerConfigProperties configProperties;
			//
			// @ConfigProperties(prefix = "server3")
			// public static class ServerConfigProperties {
			// public String host3;
			// ...

			// In this case the configProperties field must generate 'host3' property, we
			// ignore it to avoid duplicate properties, because 'host3' will be generated
			// in generatePropertiesFromClassType step
			return;
		}
		populateConfigObject(fieldType, prefix, new HashSet<>(), context);

	}

	private static String getPrefixFromAnnotation(PsiAnnotation configPropertiesAnnotation) {
		String prefix = getAnnotationMemberValue(configPropertiesAnnotation, CONFIG_PROPERTIES_ANNOTATION_PREFIX);
		return prefix == null || CONFIG_PROPERTIES_ANNOTATION_UNCONFIGURED_PREFIX.equals(prefix) ? null : prefix;
	}

	private void populateConfigObject(PsiClass configPropertiesType, String prefix, Set<PsiClass> typesAlreadyProcessed,
									  SearchContext context) {
		if (typesAlreadyProcessed.contains(configPropertiesType)) {
			return;
		}
		typesAlreadyProcessed.add(configPropertiesType);
		PsiElement[] elements = configPropertiesType.getChildren();
		// Loop for each Java fields.
		for (PsiElement child : elements) {
			if (child instanceof PsiField || child instanceof PsiLocalVariable) {
				String fieldTypeName = getResolvedTypeName(child);
				PsiClass fieldClass = findType(child.getManager(), fieldTypeName);
				if (isSimpleFieldType(fieldClass, fieldTypeName)) {
					// Java simple type (int, String, etc...) generate a property.
					PsiAnnotation configPropertyAnnotation = getAnnotation(child,
							CONFIG_PROPERTY_ANNOTATION);
					super.collectProperty((PsiModifierListOwner) child, configPropertyAnnotation, prefix, true, context);
				} else {
					// Class type, generate properties from this class type.
					PsiAnnotation configPropertyAnnotation = getAnnotation(child,
							CONFIG_PROPERTY_ANNOTATION);
					String propertyName = super.getPropertyName(child, configPropertyAnnotation, prefix, true);
					populateConfigObject(fieldClass, propertyName, typesAlreadyProcessed, context);
				}
			}
		}
	}
}
