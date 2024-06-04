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

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForTemplate;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.ITypeResolver;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.AbstractAnnotationTypeReferenceDataModelProvider;
import com.redhat.devtools.intellij.qute.psi.template.datamodel.SearchContext;
import com.redhat.devtools.intellij.qute.psi.utils.AnnotationUtils;
import com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverInfo;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverKind;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.TEMPLATE_GLOBAL_ANNOTATION;
import static com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants.TEMPLATE_GLOBAL_ANNOTATION_NAME;

/**
 * @TemplateGlobal annotation support.
 *
 *                 <code>
 * &#64;TemplateGlobal
 * public class Globals {
 *
 * 		static int age = 40;
 *
 * 		static Color[] myColors() {
 * 			return new Color[] { Color.RED, Color.BLUE };
 * 		}
 *
 * 		&#64;TemplateGlobal(name = "currentUser")
 * 		static String user() {
 * 			return "Mia";
 * 		}
 * }
 * </code>
 *
 *
 * @see <a href="https://quarkus.io/guides/qute-reference#global_variables">https://quarkus.io/guides/qute-reference#global_variables</a>
 *
 */
public class TemplateGlobalAnnotationSupport extends AbstractAnnotationTypeReferenceDataModelProvider {

	private static final Logger LOGGER = Logger.getLogger(TemplateGlobalAnnotationSupport.class.getName());

	private static final String[] ANNOTATION_NAMES = {
		TEMPLATE_GLOBAL_ANNOTATION
	};

	@Override
	protected String[] getAnnotationNames() {
		return ANNOTATION_NAMES;
	}

	@Override
	protected void processAnnotation(PsiElement javaElement, PsiAnnotation annotation, String annotationName,
									 SearchContext context, ProgressIndicator monitor) {
		if (!(javaElement instanceof PsiModifierListOwner)) {
			return;
		}
		if (annotation == null) {
			return;
		}
		ITypeResolver typeResolver = QuteSupportForTemplate.createTypeResolver((PsiMember) javaElement,
				context.getJavaProject());
		if (javaElement instanceof PsiClass) {
			PsiClass type = (PsiClass) javaElement;
			collectResolversForTemplateGlobal(type, annotation, context.getDataModelProject().getValueResolvers(),
				typeResolver, monitor);
		} else if (javaElement instanceof PsiField
			|| javaElement instanceof PsiMethod) {
			PsiMember member = (PsiMember) javaElement;
			collectResolversForTemplateGlobal(member, annotation, context.getDataModelProject().getValueResolvers(),
				typeResolver, monitor);
		}
	}

	private void collectResolversForTemplateGlobal(PsiClass type, PsiAnnotation templateGlobal,
		List<ValueResolverInfo> resolvers, ITypeResolver typeResolver, ProgressIndicator monitor) {
		try {
			PsiField[] fields = type.getFields();
			for (PsiField field : fields) {
				if (!AnnotationUtils.hasAnnotation(field, TEMPLATE_GLOBAL_ANNOTATION)
					&& isTemplateGlobalMember(field)) {
					collectResolversForTemplateGlobal(field, templateGlobal, resolvers, typeResolver, monitor);
				}
			}
			PsiMethod[] methods = type.getMethods();
			for (PsiMethod method : methods) {
				if (!AnnotationUtils.hasAnnotation(method, TEMPLATE_GLOBAL_ANNOTATION)
					&& isTemplateGlobalMember(method)) {
					collectResolversForTemplateGlobal(method, templateGlobal, resolvers, typeResolver, monitor);
				}
			}
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error while getting methods of '" + type.getQualifiedName() + "'.", e);
		}
	}

	private void collectResolversForTemplateGlobal(PsiMember member, PsiAnnotation templateGlobal,
		List<ValueResolverInfo> resolvers, ITypeResolver typeResolver, ProgressIndicator monitor) {
		if (isTemplateGlobalMember(member)) {
			String sourceType = member.getContainingClass().getQualifiedName();
			ValueResolverInfo resolver = new ValueResolverInfo();
			resolver.setSourceType(sourceType);
			resolver.setSignature(typeResolver.resolveSignature(member));
			resolver.setKind(ValueResolverKind.TemplateGlobal);
			// Constant value for {@link #name()} indicating that the field/method name
			// should be used
			try {
				resolver.setNamed(
					AnnotationUtils.getAnnotationMemberValue(templateGlobal, TEMPLATE_GLOBAL_ANNOTATION_NAME));
			} catch (ProcessCanceledException e) {
				//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
				//TODO delete block when minimum required version is 2024.2
				throw e;
			} catch (IndexNotReadyException | CancellationException e) {
				throw e;
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error while getting annotation member value of 'name'.", e);
			}
			resolver.setGlobalVariable(true);
			if (!resolvers.contains(resolver)) {
				resolvers.add(resolver);
			}
		}
	}

	/**
	 * Returns true if the given member is supported by @TemplateGlobal and false
	 * otherwise.
	 *
	 * A global variable method:
	 *
	 * <ul>
	 * <li>must not be private</li>
	 * <li>must be static,</li>
	 * <li>must not accept any parameter,</li>
	 * <li>must not return {@code void},</li>
	 * </ul>
	 *
	 * A global variable field:
	 *
	 * <ul>
	 * <li>must not be private</li>
	 * <li>must be static,</li>
	 * </ul>
	 *
	 * @param member the member to check.
	 * @return true if the given member <code>member</code> is a template global
	 *         member and false otherwise.
	 */
	private static boolean isTemplateGlobalMember(PsiMember member) {
		try {
			// every non-void non-private static method that declares no parameters and
			// every non-private static field is considered a global variable
			if (!PsiTypeUtils.isPrivateMember(member) && PsiTypeUtils.isStaticMember(member)) {
				if (member instanceof PsiField) {
					return true;
				} else if (member instanceof PsiMethod) {
					PsiMethod method = (PsiMethod) member;
					return method.getParameterList().isEmpty() && !PsiTypeUtils.isVoidReturnType(method);
				}
			}
			return false;
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error while getting method information of '" + member.getName() + "'.", e);
			return false;
		}
	}
}
