/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
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

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnnotationOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNameValuePair;

import javax.annotation.Nullable;

/**
 * Java annotations utilities.
 *
 * @author Angelo ZERR
 *
 */
public class AnnotationUtils {

	private static final String ATTRIBUTE_VALUE = "value";

	public static boolean hasAnnotation(PsiElement annotatable, String... annotationNames) {
		return getAnnotation(annotatable, annotationNames) != null;
	}

	/**
	 * Returns the annotation from the given <code>annotatable</code> element with
	 * the given name <code>annotationName</code> and null otherwise.
	 *
	 * @param annotatable    the class, field which can be annotated.
	 * @param annotationNames the annotation names
	 * @return the annotation from the given <code>annotatable</code> element with
	 *         the given name <code>annotationName</code> and null otherwise.
	 */
	public static PsiAnnotation getAnnotation(PsiElement annotatable, String... annotationNames) {
		if (annotatable == null) {
			return null;
		}
		if (annotatable instanceof PsiAnnotationOwner) {
			return getAnnotation(((PsiAnnotationOwner) annotatable).getAnnotations(), annotationNames);
		} else if (annotatable instanceof PsiModifierListOwner) {
			return getAnnotation(((PsiModifierListOwner) annotatable).getAnnotations(), annotationNames);
		}
		return null;
	}

	@Nullable
	private static PsiAnnotation getAnnotation(PsiAnnotation[] annotations, String... annotationNames) {
		for (PsiAnnotation annotation : annotations) {
			for (String annotationName : annotationNames) {
				if (isMatchAnnotation(annotation, annotationName)) {
					return annotation;
				}
			}
		}
		return null;
	}

	/**
	 * Returns true if the given annotation match the given annotation name and
	 * false otherwise.
	 *
	 * @param annotation     the annotation.
	 * @param annotationName the annotation name.
	 * @return true if the given annotation match the given annotation name and
	 *         false otherwise.
	 */
	public static boolean isMatchAnnotation(PsiAnnotation annotation, String annotationName) {
		if(annotation == null || annotation.getQualifiedName() == null){
			return false;
		}
		// Annotation name is the fully qualified name of the annotation class (ex :
		// org.eclipse.microprofile.config.inject.ConfigProperties)
		// - when IAnnotation comes from binary, IAnnotation#getElementName() =
		// 'org.eclipse.microprofile.config.inject.ConfigProperties'
		// - when IAnnotation comes from source, IAnnotation#getElementName() =
		// 'ConfigProperties'
		if (!annotationName.endsWith(annotation.getQualifiedName())) {
			return false;
		}
		if (annotationName.equals(annotation.getQualifiedName())) {
			return true;
		}
		// Here IAnnotation comes from source and match only 'ConfigProperties', we must
		// check if the CU declares the proper import (ex : import
		// org.eclipse.microprofile.config.inject.ConfigProperties;)
		return isMatchAnnotationFullyQualifiedName(annotation, annotationName);
	}

	private static boolean isMatchAnnotationFullyQualifiedName(PsiAnnotation annotation, String annotationName) {

		// The clean code should use resolveType:

		// IJavaElement parent = annotation.getParent();
		// if (parent instanceof IMember) {
		// IType declaringType = parent instanceof IType ? (IType) parent : ((IMember)
		// parent).getDeclaringType();
		// String elementName = annotation.getElementName();
		// try {
		// String[][] fullyQualifiedName = declaringType.resolveType(elementName);
		// return annotationName.equals(fullyQualifiedName[0][0] + "." +
		// fullyQualifiedName[0][1]);
		// } catch (JavaModelException e) {
		// }
		// }

		// But for performance reason, we check if the import of annotation name is
		// declared
		return annotationName.equals(annotation.getQualifiedName());
	}

	/**
	 * Returns the value of the given member name of the given annotation.
	 *
	 * @param annotation the annotation.
	 * @param memberName the member name.
	 * @return the value of the given member name of the given annotation.
	 */
	public static String getAnnotationMemberValue(PsiAnnotation annotation, String memberName) {
		PsiAnnotationMemberValue member = annotation.findDeclaredAttributeValue(memberName);
		return getValueAsString(member);
	}

	private static String getValueAsString(PsiAnnotationMemberValue member) {
		String value = member != null && member.getText() != null ? member.getText() : null;
		if (value != null && value.length() > 1 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
			value = value.substring(1, value.length() - 1);
		}
		return value;
	}

	public static String getValueAsString(PsiNameValuePair pair) {
		return pair.getValue() != null ? getValueAsString(pair.getValue()) : null;
	}

	public static Boolean getValueAsBoolean(PsiNameValuePair pair) {
		if (pair.getValue() == null) {
			return null;
		}
		Object resolvedValue = JavaPsiFacade.getInstance(pair.getProject()).getConstantEvaluationHelper().computeConstantExpression(pair.getValue());
		return resolvedValue instanceof Boolean ? (Boolean) resolvedValue : Boolean.FALSE;
	}

	public static Object[] getValueAsArray(PsiNameValuePair pair) {
		if (pair.getValue() == null) {
			return null;
		}
		Object valueObject = JavaPsiFacade.getInstance(pair.getProject()).getConstantEvaluationHelper().computeConstantExpression(pair.getValue());
		if (valueObject instanceof Object[]) {
			// @TemplateData(ignore = {"title", "id"})
			return (Object[]) valueObject;
		}
		// @TemplateData(ignore = "title")
		return new Object[] { valueObject };
	}

	/**
	 * Returns the expression for the value of the given member name of the given
	 * annotation.
	 * 
	 * @param annotation the annotation.
	 * @param memberName the member name.
	 * @return the expression for the value of the given member name of the given
	 *         annotation.
	 */
	public static PsiAnnotationMemberValue  getAnnotationMemberValueExpression(PsiAnnotation annotation, String memberName) {
		return annotation.findDeclaredAttributeValue(memberName);
	}
}
