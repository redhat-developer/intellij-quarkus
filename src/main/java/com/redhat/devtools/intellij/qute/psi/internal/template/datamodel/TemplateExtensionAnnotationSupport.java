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

import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.TEMPLATE_EXTENSION_ANNOTATION;
import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.TEMPLATE_EXTENSION_ANNOTATION_MATCH_NAME;
import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.TEMPLATE_EXTENSION_ANNOTATION_NAMESPACE;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationOwner;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForTemplate;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.ITypeResolver;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.AbstractAnnotationTypeReferenceDataModelProvider;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.SearchContext;
import com.redhat.devtools.intellij.qute.psi.utils.AnnotationUtils;
import com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverKind;
import org.apache.commons.lang3.StringUtils;

import com.redhat.qute.commons.datamodel.resolvers.ValueResolverInfo;

/**
 * Template extension support.
 * 
 * @author Angelo ZERR
 * 
 * @see <a href="https://quarkus.io/guides/qute-reference#template_extension_methods">https://quarkus.io/guides/qute-reference#template_extension_methods</a>
 *
 */
public class TemplateExtensionAnnotationSupport extends AbstractAnnotationTypeReferenceDataModelProvider {

	private static final Logger LOGGER = Logger.getLogger(TemplateExtensionAnnotationSupport.class.getName());

	private static final String[] ANNOTATION_NAMES = { TEMPLATE_EXTENSION_ANNOTATION };

	@Override
	protected String[] getAnnotationNames() {
		return ANNOTATION_NAMES;
	}

	@Override
	protected void processAnnotation(PsiElement javaElement, PsiAnnotation annotation, String annotationName,
									 SearchContext context, ProgressIndicator monitor) {
		if (!(javaElement instanceof PsiAnnotationOwner || javaElement instanceof PsiModifierListOwner)) {
			return;
		}
		PsiAnnotation templateExtension = AnnotationUtils.getAnnotation(javaElement,
				TEMPLATE_EXTENSION_ANNOTATION);
		if (templateExtension == null) {
			return;
		}
		if (javaElement instanceof PsiClass) {
			PsiClass type = (PsiClass) javaElement;
			collectResolversForTemplateExtension(type, templateExtension,
					context.getDataModelProject().getValueResolvers(), context.getJavaProject(), monitor);
		} else if (javaElement instanceof PsiMethod) {
			PsiMethod method = (PsiMethod) javaElement;
			collectResolversForTemplateExtension(method, templateExtension,
					context.getDataModelProject().getValueResolvers(), context.getJavaProject(), monitor);
		}
	}

	private static void collectResolversForTemplateExtension(PsiClass type, PsiAnnotation templateExtension,
															 List<ValueResolverInfo> resolvers, Module project,
															 ProgressIndicator monitor) {
		try {
			ITypeResolver typeResolver = QuteSupportForTemplate.createTypeResolver(type, project);
			PsiMethod[] methods = type.getMethods();
			int resolversLengthPreAdd = resolvers.size();
			for (PsiMethod method : methods) {
				if (isTemplateExtensionMethod(method)) {
					PsiAnnotation methodTemplateExtension = AnnotationUtils.getAnnotation(method,
							TEMPLATE_EXTENSION_ANNOTATION);
					collectResolversForTemplateExtension(method,
							methodTemplateExtension != null ? methodTemplateExtension : templateExtension, resolvers,
							typeResolver, ValueResolverKind.TemplateExtensionOnMethod);
				}
			}
			if (resolversLengthPreAdd == resolvers.size()) {
				// Add a dummy ValueResolverInfo to indicate that this is a template extensions
				// class
				addDummyResolverForTemplateExtensionsClass(type, resolvers);
			}
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error while getting methods of '" + type.getName() + "'.", e);
		}
	}

	/**
	 * Returns true if the given method <code>method</code> is a template extension
	 * method and false otherwise.
	 * 
	 * A template extension method:
	 * 
	 * <ul>
	 * <li>must not be private</li>
	 * <li>must be static,</li>
	 * <li>must not return void.</li>
	 * </ul>
	 * 
	 * @param method the method to check.
	 * @return true if the given method <code>method</code> is a template extension
	 *         method and false otherwise.
	 */
	private static boolean isTemplateExtensionMethod(PsiMethod method) {
		try {
			return !method.isConstructor() /* && Flags.isPublic(method.getFlags()) */
					&& !PsiTypeUtils.isVoidReturnType(method);
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error while getting method information of '" + method.getName() + "'.", e);
			return false;
		}
	}

	public static void collectResolversForTemplateExtension(PsiMethod method, PsiAnnotation templateExtension,
			List<ValueResolverInfo> resolvers, Module project, ProgressIndicator monitor) {
		if (isTemplateExtensionMethod(method)) {
			ITypeResolver typeResolver = QuteSupportForTemplate.createTypeResolver(method, project);
			collectResolversForTemplateExtension(method, templateExtension, resolvers, typeResolver,
					ValueResolverKind.TemplateExtensionOnMethod);
		}
	}

	private static void collectResolversForTemplateExtension(PsiMethod method, PsiAnnotation templateExtension,
															 List<ValueResolverInfo> resolvers,
															 ITypeResolver typeResolver, ValueResolverKind kind) {
		ValueResolverInfo resolver = new ValueResolverInfo();
		resolver.setSourceType(method.getContainingClass().getQualifiedName());
		resolver.setSignature(typeResolver.resolveMethodSignature(method));
		resolver.setKind(kind);
		resolver.setBinary(method.getContainingClass() instanceof PsiCompiledElement);
		try {
			String namespace = AnnotationUtils.getAnnotationMemberValue(templateExtension,
					TEMPLATE_EXTENSION_ANNOTATION_NAMESPACE);
			String matchName = AnnotationUtils.getAnnotationMemberValue(templateExtension,
					TEMPLATE_EXTENSION_ANNOTATION_MATCH_NAME);
			resolver.setNamespace(namespace);
			if (StringUtils.isNotEmpty(matchName)) {
				resolver.setMatchName(matchName);
			}
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (RuntimeException e) {
			LOGGER.log(Level.WARNING,
					"Error while getting annotation member value of '" + method.getName() + "'.", e);
		}
		if (!resolvers.contains(resolver)) {
			resolvers.add(resolver);
		}
	}

	private static void addDummyResolverForTemplateExtensionsClass(PsiClass type, List<ValueResolverInfo> resolvers) {
		ValueResolverInfo resolver = new ValueResolverInfo();
		resolver.setSourceType(type.getQualifiedName());
		resolver.setKind(ValueResolverKind.TemplateExtensionOnClass);
		resolver.setBinary(type instanceof PsiCompiledElement);
		// Needed to prevent NPE
		resolver.setSignature("");
		resolvers.add(resolver);
	}
}
