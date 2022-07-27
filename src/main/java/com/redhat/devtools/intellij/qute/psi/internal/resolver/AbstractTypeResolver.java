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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiVariable;
import com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class for {@link ITypeResolver}.
 * 
 * @author Angelo ZERR
 *
 */
public abstract class AbstractTypeResolver implements ITypeResolver {

	private static final Logger LOGGER = Logger.getLogger(AbstractTypeResolver.class.getName());

	public static String resolveJavaTypeSignature(PsiClass type) {
		if (type.getQualifiedName() != null) {
			StringBuilder typeName = new StringBuilder(type.getQualifiedName());
			try {
				PsiTypeParameter[] parameters = type.getTypeParameters();
				if (parameters.length > 0) {
					typeName.append("<");
					for (int i = 0; i < parameters.length; i++) {
						if (i > 0) {
							typeName.append(",");
						}
						typeName.append(parameters[i].getName());
					}
					typeName.append(">");
				}
				return typeName.toString();
			} catch (RuntimeException e) {
				LOGGER.log(Level.SEVERE, "Error while collecting Java Types for Java type '" + typeName + "'.", e);
			}
			return typeName.toString();
		}
		return null;
	}

	@Override
	public String resolveFieldSignature(PsiVariable field) {
		StringBuilder signature = new StringBuilder(field.getName());
		signature.append(" : ");
		try {
			signature.append(field.getType().getCanonicalText(true));
		} catch (RuntimeException e) {
			LOGGER.log(Level.SEVERE, "Error while resolving field type '" + field.getName() + "'", e);
		}
		return signature.toString();
	}

	@Override
	public String resolveMethodSignature(PsiMethod method) {
		StringBuilder signature = new StringBuilder(method.getName());
		signature.append('(');
		try {
			PsiParameter[] parameters = method.getParameterList().getParameters();
			if (parameters.length > 0) {
				boolean varargs = method.isVarArgs();
				for (int i = 0; i < parameters.length; i++) {
					if (i > 0) {
						signature.append(", ");
					}
					PsiParameter parameter = parameters[i];
					signature.append(parameter.getName());
					signature.append(" : ");
					signature.append(PsiTypeUtils.resolveSignature(parameter, method.getContainingClass(),
							varargs && i == parameters.length - 1));
				}
			}
		} catch (RuntimeException e) {
			LOGGER.log(Level.SEVERE,
					"Error while resolving method parameters type of '" + method.getName() + "'", e);
		}
		signature.append(')');
		try {
			String returnType = method.getReturnType().getCanonicalText(true);
			signature.append(" : ");
			signature.append(returnType);
		} catch (RuntimeException e) {
			LOGGER.log(Level.SEVERE, "Error while resolving method return type of '" + method.getName() + "'",
					e);
		}
		return signature.toString();
	}

	protected abstract String resolveSimpleType(String name);
}
