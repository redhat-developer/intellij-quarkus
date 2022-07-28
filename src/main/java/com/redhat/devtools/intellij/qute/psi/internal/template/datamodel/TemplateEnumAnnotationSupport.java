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
package com.redhat.devtools.intellij.qute.psi.internal.template.datamodel;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForTemplate;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.ITypeResolver;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.AbstractAnnotationTypeReferenceDataModelProvider;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.SearchContext;
import com.redhat.devtools.intellij.qute.psi.utils.AnnotationUtils;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverInfo;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.TEMPLATE_DATA_ANNOTATION;
import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.TEMPLATE_ENUM_ANNOTATION;

/**
 * @TemplateEnum annotation support.
 *
 *
 *               <code>
 * &#64;TemplateEnum
 * public enum Status {
 * 		ON,
 * 		OFF
 * }
 * </code>
 *
 * @see <a href="https://quarkus.io/guides/qute-reference#convenient-annotation-for-enums">https://quarkus.io/guides/qute-reference#convenient-annotation-for-enums</a>
 *
 */
public class TemplateEnumAnnotationSupport extends AbstractAnnotationTypeReferenceDataModelProvider {

	private static final Logger LOGGER = Logger.getLogger(TemplateEnumAnnotationSupport.class.getName());

	private static final String[] ANNOTATION_NAMES = {
		TEMPLATE_ENUM_ANNOTATION
	};

	@Override
	protected String[] getAnnotationNames() {
		return ANNOTATION_NAMES;
	}

	@Override
	protected void processAnnotation(PsiElement javaElement, PsiAnnotation annotation, String annotationName,
									 SearchContext context, ProgressIndicator monitor) {
		if (!(javaElement instanceof PsiClass)) {
			return;
		}
		PsiClass type = (PsiClass) javaElement;
		if (!type.isEnum()) {
			// @TemplateEnum declared on a non-enum class is ignored.
			return;
		}
		PsiAnnotation templateEnum = AnnotationUtils.getAnnotation(javaElement, TEMPLATE_ENUM_ANNOTATION);
		if (templateEnum == null) {
			return;
		}
		// Check if type is annotated with @TemplateData
		PsiAnnotation templateData = AnnotationUtils.getAnnotation(javaElement, TEMPLATE_DATA_ANNOTATION);
		if (templateData != null) {
			// Also if an enum also declares the @TemplateData annotation then the
			// @TemplateEnum annotation is ignored.
			return;
		}
		collectResolversForTemplateEnum(type, context.getDataModelProject().getValueResolvers(), monitor);
	}

	private static void collectResolversForTemplateEnum(PsiClass type, List<ValueResolverInfo> resolvers,
		ProgressIndicator monitor) {
		try {
			ITypeResolver typeResolver = QuteSupportForTemplate.createTypeResolver(type);
			PsiField[] fields = type.getFields();
			for (PsiField field : fields) {
				collectResolversForTemplateEnum(field, resolvers, typeResolver);
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error while getting methods of '" + type.getQualifiedName() + "'.", e);
		}
	}

	private static void collectResolversForTemplateEnum(PsiField field, List<ValueResolverInfo> resolvers,
		ITypeResolver typeResolver) {
		ValueResolverInfo resolver = new ValueResolverInfo();
		resolver.setSourceType(field.getContainingClass().getQualifiedName());
		resolver.setSignature(typeResolver.resolveFieldSignature(field));
		// This annotation is functionally equivalent to @TemplateData(namespace =
		// TemplateData.SIMPLENAME),
		// i.e. a namespace resolver is automatically generated for the target enum and
		// the simple name of the target enum is used as the namespace.
		resolver.setNamespace(field.getContainingClass().getName());
		if (!resolvers.contains(resolver)) {
			resolvers.add(resolver);
		}
	}
}
