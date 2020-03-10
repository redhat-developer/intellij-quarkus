/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiMember;

/**
 * Java annotations utilities.
 * 
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/utils/AnnotationUtils.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/utils/AnnotationUtils.java</a>
 *
 */
public class AnnotationUtils {

	/**
	 * Returns the annotation from the given <code>annotatable</code> element with
	 * the given name <code>annotationName</code> and null otherwise.
	 * 
	 * @param annotatable    the class, field which can be annotated.
	 * @param annotationName the annotation name
	 * @return the annotation from the given <code>annotatable</code> element with
	 *         the given name <code>annotationName</code> and null otherwise.
	 */
	public static PsiAnnotation getAnnotation(PsiMember annotatable, String annotationName) {
		if (annotatable == null) {
			return null;
		}
		PsiAnnotation[] annotations = annotatable.getAnnotations();
		for (PsiAnnotation annotation : annotations) {
			if (isMatchAnnotation(annotation, annotationName)) {
				return annotation;
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
		return annotationName.endsWith(annotation.getQualifiedName());
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
		String value = member != null && member.getText() != null ? member.getText() : null;
		if (value != null && value.length() > 1 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
			value = value.substring(1, value.length() - 1);
		}
		return value;
	}
}
