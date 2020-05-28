/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search.providers;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.search.GlobalSearchScope;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;
import com.redhat.devtools.intellij.quarkus.search.IPropertiesCollector;
import com.redhat.devtools.intellij.quarkus.search.SearchContext;
import com.redhat.devtools.intellij.quarkus.search.core.utils.AnnotationUtils;
import com.redhat.devtools.intellij.quarkus.search.core.utils.PsiTypeUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Properties provider to collect MicroProfile properties from the Java fields
 * annotated with "org.eclipse.microprofile.config.inject.ConfigProperty"
 * annotation.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/providers/MicroProfileConfigPropertyProvider.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/providers/MicroProfileConfigPropertyProvider.java</a>
 */
public class MicroProfileConfigPropertyProvider extends AbstractAnnotationTypeReferencePropertiesProvider {

	private static final String[] ANNOTATION_NAMES = { QuarkusConstants.CONFIG_PROPERTY_ANNOTATION };

	@Override
	protected String[] getAnnotationNames() {
		return ANNOTATION_NAMES;
	}

	@Override
	protected void processAnnotation(PsiModifierListOwner psiElement, PsiAnnotation configPropertyAnnotation,
									 String annotationName, SearchContext context) {
		if (psiElement instanceof PsiField || psiElement instanceof PsiMethod || psiElement instanceof PsiParameter) {
			IPropertiesCollector collector = context.getCollector();
			String name = AnnotationUtils.getAnnotationMemberValue(configPropertyAnnotation,
					QuarkusConstants.CONFIG_PROPERTY_ANNOTATION_NAME);
			if (StringUtils.isNotEmpty(name)) {
				String propertyTypeName = "";
				if (psiElement instanceof PsiField) {
					propertyTypeName = PsiTypeUtils.getResolvedTypeName((PsiField) psiElement);
				} else if (psiElement instanceof PsiMethod) {
					propertyTypeName = PsiTypeUtils.getResolvedResultTypeName((PsiMethod) psiElement);
				} else if (psiElement instanceof PsiVariable) {
					propertyTypeName = PsiTypeUtils.getResolvedTypeName((PsiVariable) psiElement);
				}
				PsiClass fieldClass = JavaPsiFacade.getInstance(psiElement.getProject()).findClass(propertyTypeName, GlobalSearchScope.allScope(psiElement.getProject()));

				String type = PsiTypeUtils.getPropertyType(fieldClass, propertyTypeName);
				String description = null;
				String sourceType = PsiTypeUtils.getSourceType(psiElement);
				String sourceField = null;
				String sourceMethod = null;
				if (psiElement instanceof PsiField || psiElement instanceof PsiMethod) {
					sourceField = PsiTypeUtils.getSourceField((PsiMember) psiElement);
				} else if (psiElement instanceof PsiParameter) {
					PsiMethod method = (PsiMethod) ((PsiParameter)psiElement).getDeclarationScope();
						sourceMethod = PsiTypeUtils.getSourceMethod(method);
				}
				String defaultValue = AnnotationUtils.getAnnotationMemberValue(configPropertyAnnotation,
						QuarkusConstants.CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE);
				String extensionName = null;

				super.updateHint(collector, fieldClass);

				addItemMetadata(collector, name, type, description, sourceType, sourceField, sourceMethod, defaultValue,
						extensionName, PsiTypeUtils.isBinary(psiElement));
			}
		}
	}

}
