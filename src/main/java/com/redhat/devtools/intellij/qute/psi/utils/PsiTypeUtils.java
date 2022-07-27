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

import com.intellij.openapi.module.Module;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.ClassUtil;

import java.util.Arrays;
import java.util.List;

/**
 * JDT Type utilities.
 *
 * @author Angelo ZERR
 *
 */
public class PsiTypeUtils {

	private static final List<String> NUMBER_TYPES = Arrays.asList("short", "int", "long", "double", "float");

	public static String getSimpleClassName(String className) {
		if (className.endsWith(".java")) {
			return className.substring(0, className.length() - ".java".length());
		}
		if (className.endsWith(".class")) {
			return className.substring(0, className.length() - ".class".length());
		}
		return className;
	}

	public static PsiClass findType(Module project, String name) {
		JavaPsiFacade facade = JavaPsiFacade.getInstance(project.getProject());
		return facade.findClass(name, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(project));
	}

	/**
	 * Returns the resolved type name of the <code>javaElement</code> and null
	 * otherwise
	 *
	 * @param javaElement the Java element
	 * @return the resolved type name of the <code>javaElement</code> and null
	 *         otherwise
	 */
	public static String getResolvedTypeName(PsiElement javaElement) {
		if (javaElement instanceof PsiVariable) {
			return getResolvedTypeName((PsiLocalVariable) javaElement);
		} else if (javaElement instanceof PsiField) {
			return getResolvedTypeName((PsiField) javaElement);
		}
		return null;
	}

	/**
	 * Returns the resolved type name of the given <code>localVar</code> and null
	 * otherwise
	 *
	 * @param localVar the local variable
	 * @return the resolved type name of the given <code>localVar</code> and null
	 *         otherwise
	 */
	public static String getResolvedTypeName(PsiLocalVariable localVar) {
		return localVar.getType().getCanonicalText();
	}

	/**
	 * Returns the resolved type name of the given <code>field</code> and null
	 * otherwise
	 *
	 * @param field the field
	 * @return the resolved type name of the given <code>field</code> and null
	 *         otherwise
	 */
	public static String getResolvedTypeName(PsiField field) {
		return field.getType().getCanonicalText();
	}

	public static String getPropertyType(PsiClass psiClass, String typeName) {
		return psiClass != null ? psiClass.getQualifiedName() : typeName;
	}

	/**
	 * Returns true if the given <code>javaElement</code> is from a Java binary, and
	 * false otherwise
	 *
	 * @param javaElement the Java element
	 * @return true if the given <code>javaElement</code> is from a Java binary, and
	 *         false otherwise
	 */
	public static boolean isBinary(PsiElement javaElement) {
		return javaElement instanceof PsiCompiledElement;
	}

	/**
	 * Returns the source type of the given <code>javaElement</code> and null
	 * otherwise
	 *
	 * @param psiElement the Java element
	 * @return the source type of the <code>javaElement</code>
	 */
	public static String getSourceType(PsiElement psiElement) {
		if (psiElement instanceof PsiField || psiElement instanceof PsiMethod) {
			return ClassUtil.getJVMClassName(((PsiMember)psiElement).getContainingClass());
		} else if (psiElement instanceof PsiParameter) {
			return ClassUtil.getJVMClassName(((PsiMethod)((PsiParameter)psiElement).getDeclarationScope()).getContainingClass());
		} if (psiElement instanceof PsiClass) {
			return ClassUtil.getJVMClassName((PsiClass) psiElement);
		}
		return null;
	}

	/**
	 * Returns the source type of the given local variable <code>member</code> and
	 * null otherwise
	 *
	 * @param member the local variable to get the source type from
	 * @return the source type of the given local variable <code>member</code> and
	 *         null otherwise
	 */
	public static String getSourceType(PsiLocalVariable member) {
		return getSourceType(member.getParent());
	}

	public static String getSourceMethod(PsiMethod method) {
		return method.getName() + method.getSignature(PsiSubstitutor.EMPTY);
	}

	private static String resolveSignature(PsiType type) {
		if (type instanceof PsiArrayType) {
			return resolveSignature(((PsiArrayType) type).getComponentType()) + "[]";
		}
		return type.getCanonicalText(true);
		//return ClassUtil.getBinaryPresentation(methodParameter.getType());

	}

	public static String resolveSignature(PsiParameter methodParameter, PsiClass type) {
		return resolveSignature(methodParameter.getType());
	}

	/**
	 * Return true if member is static, and false otherwise
	 *
	 * @param member the member to check for static
	 * @return
	 */
	public static boolean isStaticMember(PsiMember member) {
		return member.getModifierList().hasExplicitModifier(PsiModifier.STATIC);
	}

	/**
	 * Return true if member is private, and false otherwise
	 *
	 * @param member the member to check for private access modifier
	 * @return
	 */
	public static boolean isPrivateMember(PsiMember member) {
		return member.getModifierList().hasExplicitModifier(PsiModifier.PRIVATE);
	}

	/**
	 * Return true if member is public, and false otherwise
	 *
	 * @param member the member to check for public access modifier
	 * @return
	 */
	public static boolean isPublicMember(PsiMember member) {
		return member.getModifierList().hasExplicitModifier(PsiModifier.PUBLIC);
	}

	/**
	 * Return true if method returns `void`, and false otherwise
	 *
	 * @param method the method to check return value of
	 * @return
	 */
	public static boolean isVoidReturnType(PsiMethod method) {
		return PsiType.VOID.equals(method.getReturnType());
	}
}
