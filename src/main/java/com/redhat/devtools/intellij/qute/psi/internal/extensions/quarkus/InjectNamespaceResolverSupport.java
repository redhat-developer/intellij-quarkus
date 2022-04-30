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
package com.redhat.devtools.intellij.qute.psi.internal.extensions.quarkus;

import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.JAVAX_INJECT_NAMED_ANNOTATION;
import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.VALUE_ANNOTATION_NAME;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForTemplate;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.ITypeResolver;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.AbstractAnnotationTypeReferenceDataModelProvider;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.SearchContext;
import com.redhat.devtools.intellij.qute.psi.utils.AnnotationUtils;

import com.redhat.devtools.intellij.qute.psi.utils.CDIUtils;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverInfo;

/**
 * Injecting Beans Directly In Templates support.
 * 
 * @author Angelo ZERR
 * 
 * @see <a href="https://quarkus.io/guides/qute-reference#injecting-beans-directly-in-templates">https://quarkus.io/guides/qute-reference#injecting-beans-directly-in-templates</a>
 *
 */
public class InjectNamespaceResolverSupport extends AbstractAnnotationTypeReferenceDataModelProvider {

	private static final Logger LOGGER = Logger.getLogger(InjectNamespaceResolverSupport.class.getName());

	private static final String INJECT_NAMESPACE = "inject";

	private static final String[] ANNOTATION_NAMES = { JAVAX_INJECT_NAMED_ANNOTATION };

	@Override
	protected String[] getAnnotationNames() {
		return ANNOTATION_NAMES;
	}

	@Override
	protected void processAnnotation(PsiElement javaElement, PsiAnnotation annotation, String annotationName,
									 SearchContext context, ProgressIndicator monitor) {
		if (javaElement instanceof PsiClass) {
			// @Named("flash")
			// public class Flash;
			// becomes --> inject:flash

			PsiClass type = (PsiClass) javaElement;
			String named = getNamed(type);
			collectResolversForInject(type, named, context.getDataModelProject().getValueResolvers());
		} else if (javaElement instanceof PsiField || javaElement instanceof PsiMethod) {
			// @Named
			// private String foo;
			// becomes --> inject:foo

			// @Named("user")
			// private String getUser() {...
			// becomes --> inject:user

			PsiMember javaMember = (PsiMember) javaElement;
			String named = getNamed(javaMember);
			ITypeResolver typeResolver = QuteSupportForTemplate.createTypeResolver(javaMember);
			collectResolversForInject(javaMember, named, context.getDataModelProject().getValueResolvers(),
					typeResolver);
		}
	}

	private static String getNamed(PsiElement javaElement) {
		String named = getAnnotationNamedValue(javaElement);
		return CDIUtils.getSimpleName(javaElement, named);
	}

	private static String getAnnotationNamedValue(PsiElement javaElement) {
		try {
			PsiAnnotation namedAnnotation = AnnotationUtils.getAnnotation(javaElement,
					JAVAX_INJECT_NAMED_ANNOTATION);
			if (namedAnnotation != null) {
				return AnnotationUtils.getAnnotationMemberValue(namedAnnotation, VALUE_ANNOTATION_NAME);
			}
		} catch (RuntimeException e) {
			LOGGER.log(Level.SEVERE, "Error while getting @Named annotation value.", e);
			return null;
		}
		return null;
	}

	private static void collectResolversForInject(PsiClass type, String named, List<ValueResolverInfo> resolvers) {
		ValueResolverInfo resolver = new ValueResolverInfo();
		resolver.setNamed(named);
		resolver.setSourceType(type.getQualifiedName());
		resolver.setSignature(type.getQualifiedName());
		resolver.setNamespace(INJECT_NAMESPACE);
		resolvers.add(resolver);
	}

	private static void collectResolversForInject(PsiMember javaMember, String named, List<ValueResolverInfo> resolvers,
			ITypeResolver typeResolver) {
		ValueResolverInfo resolver = new ValueResolverInfo();
		resolver.setNamed(named);
		resolver.setSourceType(javaMember.getContainingClass().getQualifiedName());
		resolver.setSignature(getSignature(javaMember, typeResolver));
		resolver.setNamespace(INJECT_NAMESPACE);
		resolvers.add(resolver);
	}

	private static String getSignature(PsiMember javaMember, ITypeResolver typeResolver) {
		if (javaMember instanceof PsiField) {
			return typeResolver.resolveFieldSignature((PsiField) javaMember);
		} else if (javaMember instanceof PsiMethod) {
			return typeResolver.resolveMethodSignature((PsiMethod) javaMember);
		}
		return null;
	}

}
