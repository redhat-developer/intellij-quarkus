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
package com.redhat.devtools.intellij.qute.psi.internal.resolver;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiVariable;

/**
 * Type resolver API.
 * 
 * @author Angelo ZERR
 *
 */
public interface ITypeResolver {
	/**
	 * Returns the Java signature from the given JDT <code>javaElement</code>.
	 *
	 * @param javaElement the Java element (field, method).
	 *
	 * @return the Java signature from the given JDT <code>javaElement</code>.
	 */
	default String resolveSignature(PsiMember javaElement) {
		if (javaElement instanceof PsiField) {
			return resolveFieldSignature((PsiField) javaElement);
		} else if (javaElement instanceof PsiMethod) {
			return resolveMethodSignature((PsiMethod) javaElement);
		}
		throw new UnsupportedOperationException("Unsupported java element type: " + javaElement.getClass());
	}


	/**
	 * Returns the resolved Java type signature from the given String <code>typeSignature</code>.
	 *
	 * Example:
	 *
	 * <code>
	 * String
	 * </code>
	 *
	 * will returns:
	 *
	 * <code>
	 * java.lang.String
	 * </code>
	 *
	 * @param typeSignature the Java type signature.
	 *
	 * @return the resolved  Java type signature from the given String <code>typeSignature</code>.
	 */
	String resolveTypeSignature(String typeSignature, Module javaProject);

	/**
	 * Returns the Java field signature from the given JDT <code>field</code>.
	 * 
	 * Example:
	 * 
	 * <code>
	 * name : java.lang.String
	 * </code>
	 * 
	 * @param field the JDT field
	 * 
	 * @return the Java field signature.
	 */
	String resolveFieldSignature(PsiVariable field);

	/**
	 * Returns the Java method signature from the given JDT <code>method</code>.
	 * 
	 * Example:
	 * 
	 * <code>
	 * find(query : java.lang.String, params : java.util.Map<java.lang.String,java.lang.Object>) : io.quarkus.hibernate.orm.panache.PanacheQuery<T>
	 * </code>
	 * 
	 * @param method the JDT method
	 * 
	 * @return the Java method signature.
	 */
	String resolveMethodSignature(PsiMethod method);

}
