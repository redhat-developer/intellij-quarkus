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
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForTemplate;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.ITypeResolver;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.AbstractAnnotationTypeReferenceDataModelProvider;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.SearchContext;
import com.redhat.devtools.intellij.qute.psi.utils.AnnotationUtils;
import com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.TEMPLATE_DATA_ANNOTATION;
import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.TEMPLATE_DATA_ANNOTATION_NAMESPACE;

/**
 * @TemplateData annotation support.
 *
 *               <code>
 * &#64;TemplateData
 * class Item {
 *
 * 		public final BigDecimal price;
 *
 * 		public Item(BigDecimal price) {
 * 			this.price = price;
 * 		}
 *
 * 		public BigDecimal getDiscountedPrice() {
 * 			return price.multiply(new BigDecimal("0.9"));
 * 		}
 * }
 * </code>
 *
 *
 * @see <a href="https://quarkus.io/guides/qute-reference#template_data">https://quarkus.io/guides/qute-reference#template_data</a>
 *
 */
public class TemplateDataAnnotationSupport extends AbstractAnnotationTypeReferenceDataModelProvider {

	private static final Logger LOGGER = Logger.getLogger(TemplateDataAnnotationSupport.class.getName());

	private static final String[] ANNOTATION_NAMES = {
			TEMPLATE_DATA_ANNOTATION
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
		// Check if the given Java type defines a @TemplateData/namespace

		// Loop for @TemplateData annotations:
		// Ex :
		// @TemplateData
		// @TemplateData(namespace = "foo")
		// public class Item
		//
		// should resolve namespace to "foo"
		String namespace = null;
		boolean hasTemplateData = false;
		for (PsiAnnotation typeAnnotation : type.getAnnotations()) {
			if (AnnotationUtils.isMatchAnnotation(typeAnnotation, TEMPLATE_DATA_ANNOTATION)) {
				hasTemplateData = true;
				namespace = AnnotationUtils.getAnnotationMemberValue(typeAnnotation,
						TEMPLATE_DATA_ANNOTATION_NAMESPACE);
				if (StringUtils.isNotEmpty(namespace)) {
					break;
				}
			}
		}
		if (!hasTemplateData) {
			return;
		}

		ITypeResolver typeResolver = QuteSupportForTemplate.createTypeResolver(type);

		// Loop for static fields
		PsiField[] fields = type.getFields();
		for (PsiField field : fields) {
			collectResolversForTemplateData(field, namespace, context.getDataModelProject().getValueResolvers(),
					typeResolver, monitor);
		}

		// Loop for static methods
		PsiMethod[] methods = type.getMethods();
		for (PsiMethod method : methods) {
			collectResolversForTemplateData(method, namespace, context.getDataModelProject().getValueResolvers(),
					typeResolver, monitor);
		}
	}

	private void collectResolversForTemplateData(PsiMember member, String namespace, List<ValueResolverInfo> resolvers,
												 ITypeResolver typeResolver, ProgressIndicator monitor) {
		try {
			if (PsiTypeUtils.isPublicMember(member) && PsiTypeUtils.isStaticMember(member)) {
				// The field or method is public and static
				String sourceType = member.getContainingClass().getQualifiedName();
				ValueResolverInfo resolver = new ValueResolverInfo();
				resolver.setSourceType(sourceType);
				resolver.setSignature(typeResolver.resolveSignature(member));
				resolver.setNamespace(StringUtils.isNotEmpty(namespace) ? namespace : sourceType.replace('.', '_'));
				if (!resolvers.contains(resolver)) {
					resolvers.add(resolver);
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,
					"Error while getting annotation member value of '" + member.getName() + "'.", e);
		}
	}
}
