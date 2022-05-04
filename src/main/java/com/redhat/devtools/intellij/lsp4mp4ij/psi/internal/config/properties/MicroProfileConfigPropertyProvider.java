/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.config.properties;

import com.intellij.lang.jvm.JvmMember;
import com.intellij.openapi.module.Module;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.AbstractAnnotationTypeReferencePropertiesProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.IPropertiesCollector;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.SearchContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;
import org.apache.commons.lang3.StringUtils;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.CONFIG_PROPERTIES_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION_NAME;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.getAnnotationMemberValue;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils.hasAnnotation;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.findType;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getEnclosedType;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getPropertyType;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getResolvedTypeName;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getSourceField;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getSourceMethod;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.getSourceType;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils.isBinary;

/**
 * Properties provider to collect MicroProfile properties from the Java fields
 * annotated with "org.eclipse.microprofile.config.inject.ConfigProperty"
 * annotation.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/providers/MicroProfileConfigPropertyProvider.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/providers/MicroProfileConfigPropertyProvider.java</a>
 */
public class MicroProfileConfigPropertyProvider extends AbstractAnnotationTypeReferencePropertiesProvider {

	private static final String[] ANNOTATION_NAMES = { CONFIG_PROPERTY_ANNOTATION };

	@Override
	protected String[] getAnnotationNames() {
		return ANNOTATION_NAMES;
	}

	@Override
	protected void processAnnotation(PsiModifierListOwner javaElement, PsiAnnotation configPropertyAnnotation,
									 String annotationName, SearchContext context) {
		if (javaElement instanceof PsiField || javaElement instanceof PsiVariable) {
			// Generate the property only class is not annotated with @ConfigProperties
			PsiClass classType = PsiTreeUtil.getParentOfType(javaElement, PsiClass.class);
			boolean hasConfigPropertiesAnnotation = hasAnnotation(classType, CONFIG_PROPERTIES_ANNOTATION);
			if (!hasConfigPropertiesAnnotation) {
				collectProperty(javaElement, configPropertyAnnotation, null, false, context);
			}
		}
	}

	protected void collectProperty(PsiModifierListOwner javaElement, PsiAnnotation configPropertyAnnotation, String prefix,
								   boolean useFieldNameIfAnnotationIsNotPresent, SearchContext context) {
		String propertyName = getPropertyName(javaElement, configPropertyAnnotation, prefix,
				useFieldNameIfAnnotationIsNotPresent);
		if (propertyName != null && !propertyName.isEmpty()) {
			String defaultValue = configPropertyAnnotation != null
					? getAnnotationMemberValue(configPropertyAnnotation, CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE)
					: null;
			collectProperty(javaElement, propertyName, defaultValue, context);
		}
	}

	protected String getPropertyName(PsiElement javaElement, PsiAnnotation configPropertyAnnotation, String prefix,
									 boolean useFieldNameIfAnnotationIsNotPresent) {
		if (configPropertyAnnotation != null) {
			return getPropertyName(getAnnotationMemberValue(configPropertyAnnotation, CONFIG_PROPERTY_ANNOTATION_NAME),
					prefix);
		} else if (useFieldNameIfAnnotationIsNotPresent) {
			return getPropertyName(getName(javaElement), prefix);
		}
		return null;
	}

	private static String getName(PsiElement javaElement) {
		if (javaElement instanceof PsiMember) {
			return ((PsiMember) javaElement).getName();
		} else if (javaElement instanceof JvmMember) {
			return ((JvmMember) javaElement).getName();
		}
		return null;
	}

	public static String getPropertyName(String propertyName, String prefix) {
		return StringUtils.isNotEmpty(prefix) ? (prefix + "." + propertyName) : propertyName;
	}

	private void collectProperty(PsiModifierListOwner javaElement, String name, String defaultValue,
								 SearchContext context) {
		Module javaProject = context.getJavaProject();
		String varTypeName = getResolvedTypeName(javaElement);
		PsiClass varType = findType(javaProject, varTypeName);
		String type = getPropertyType(varType, varTypeName);
		String description = null;
		String sourceType = getSourceType(javaElement);
		String sourceField = null;
		String sourceMethod = null;

		String extensionName = null;

		if (javaElement instanceof PsiField) {
			sourceField = getSourceField((PsiMember) javaElement);
		} else if (javaElement instanceof PsiVariable) {
			PsiVariable localVariable = (PsiVariable) javaElement;
			PsiMethod method = PsiTreeUtil.getParentOfType(localVariable, PsiMethod.class);
			sourceMethod = getSourceMethod(method);
		}

		// Enumerations
		PsiClass enclosedType = getEnclosedType(varType, type, javaElement.getManager());
		super.updateHint(context.getCollector(), enclosedType);

		boolean binary = isBinary(javaElement);
		super.addItemMetadata(context.getCollector(), name, type, description, sourceType, sourceField, sourceMethod, defaultValue,
				extensionName, binary);
	}


	private void oldMethod(PsiModifierListOwner psiElement, PsiAnnotation configPropertyAnnotation, SearchContext context) {
		IPropertiesCollector collector = context.getCollector();
		String name = getAnnotationMemberValue(configPropertyAnnotation,
				QuarkusConstants.CONFIG_PROPERTY_ANNOTATION_NAME);
		if (StringUtils.isNotEmpty(name)) {
			String propertyTypeName = "";
			if (psiElement instanceof PsiField) {
				propertyTypeName = getResolvedTypeName((PsiField) psiElement);
			} else if (psiElement instanceof PsiMethod) {
				propertyTypeName = PsiTypeUtils.getResolvedResultTypeName((PsiMethod) psiElement);
			} else if (psiElement instanceof PsiVariable) {
				propertyTypeName = getResolvedTypeName((PsiVariable) psiElement);
			}
			PsiClass fieldClass = JavaPsiFacade.getInstance(psiElement.getProject()).findClass(propertyTypeName, GlobalSearchScope.allScope(psiElement.getProject()));

			String type = getPropertyType(fieldClass, propertyTypeName);
			String description = null;
			String sourceType = getSourceType(psiElement);
			String sourceField = null;
			String sourceMethod = null;
			if (psiElement instanceof PsiField || psiElement instanceof PsiMethod) {
				sourceField = getSourceField((PsiMember) psiElement);
			} else if (psiElement instanceof PsiParameter) {
				PsiMethod method = (PsiMethod) ((PsiParameter) psiElement).getDeclarationScope();
					sourceMethod = getSourceMethod(method);
			}
			String defaultValue = getAnnotationMemberValue(configPropertyAnnotation,
					QuarkusConstants.CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE);
			String extensionName = null;

			super.updateHint(collector, fieldClass);

			addItemMetadata(collector, name, type, description, sourceType, sourceField, sourceMethod, defaultValue,
					extensionName, isBinary(psiElement));
		}
	}

}
