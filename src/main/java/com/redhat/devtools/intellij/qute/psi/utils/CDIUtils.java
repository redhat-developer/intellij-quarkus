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
package com.redhat.devtools.intellij.qute.psi.utils;

import java.beans.Introspector;
import java.util.function.Supplier;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNamedElement;
import org.apache.commons.lang3.StringUtils;

/**
 * CDI utilities.
 * 
 * @author Angelo ZERR
 * 
 * @see <a href="https://github.com/jbosstools/jbosstools-javaee/blob/8fc233ad8d90dbf44fc2f5475e45393fb9f9f4b1/cdi/plugins/org.jboss.tools.cdi.seam.solder.core/src/org/jboss/tools/cdi/seam/solder/core/CDISeamSolderCoreExtension.java#L261">https://github.com/jbosstools/jbosstools-javaee/blob/8fc233ad8d90dbf44fc2f5475e45393fb9f9f4b1/cdi/plugins/org.jboss.tools.cdi.seam.solder.core/src/org/jboss/tools/cdi/seam/solder/core/CDISeamSolderCoreExtension.java#L261</a>
 */
public class CDIUtils {

	private CDIUtils() {
	}

	public static String getSimpleName(PsiElement javaElement, String annotationNamedValue) {
		if (javaElement instanceof PsiNamedElement) {
			return getSimpleName(((PsiNamedElement) javaElement).getName(), annotationNamedValue, javaElement.getClass(), () -> {
				return BeanUtil.isGetter((PsiMethod) javaElement);
			});
		}
		return null;
	}

	public static String getSimpleName(String javaElementName, String annotationNamedValue, Class javaElementType) {
		return getSimpleName(javaElementName, annotationNamedValue, javaElementType, () -> false);
	}

	public static String getSimpleName(String javaElementName, String annotationNamedValue, Class javaElementType,
			Supplier<Boolean> isGetterMethod) {
		if (StringUtils.isNotEmpty(annotationNamedValue)) {
			// A @Named is defined with value. Ex:
			// @Named("flash")
			// private Flash fieldFlash;
			// --> returns 'flash'
			return annotationNamedValue;
		}
		if (javaElementType.isAssignableFrom(PsiClass.class)) {
			// MyClass --> myClass
			return Introspector.decapitalize(javaElementName);
		} else if (javaElementType.isAssignableFrom(PsiField.class)) {
			return javaElementName;
		} else if (javaElementType.isAssignableFrom(PsiMethod.class)) {
			if (isGetterMethod.get()) {
				return BeanUtil.getPropertyName(javaElementName);
			}
			return javaElementName;
		}
		return javaElementName;
	}
}
