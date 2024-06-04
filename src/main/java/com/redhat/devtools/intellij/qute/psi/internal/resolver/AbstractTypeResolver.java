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

import com.intellij.lang.jvm.types.JvmReferenceType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.ClassUtil;
import com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
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

	protected final PsiClass primaryType;
	private final Module javaProject;

	public AbstractTypeResolver(PsiClass primaryType, Module javaProject) {
		this.primaryType = primaryType;
		this.javaProject = javaProject;
	}


	public static String resolveJavaTypeSignature(PsiClass type) {
		if (type.getQualifiedName() != null) {

			StringBuilder typeName = new StringBuilder(ClassUtil.getJVMClassName(type));
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
			} catch (ProcessCanceledException e) {
				//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
				//TODO delete block when minimum required version is 2024.2
				throw e;
			} catch (IndexNotReadyException | CancellationException e) {
				throw e;
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error while collecting Java Types for Java type '" + typeName + "'.", e);
			}
			return typeName.toString();
		}
		return null;
	}

	@Override
	public List<String> resolveExtendedType() {
		List<String> extendedTypes = new ArrayList<>();
		try {
			if (primaryType.getSuperClassType() instanceof PsiClassType &&
					(primaryType.isInterface() || !PsiType.getJavaLangObject(primaryType.getManager(),
							GlobalSearchScope.allScope(primaryType.getProject())).equals(primaryType.getSuperClassType()))) {
				extendedTypes.add(PsiTypeUtils.resolveSignature((PsiType) primaryType.getSuperClassType(), false));
			}
			JvmReferenceType @NotNull [] superInterfaceTypeSignature = primaryType.getInterfaceTypes();
			if (superInterfaceTypeSignature != null) {
				for (JvmReferenceType string : superInterfaceTypeSignature) {
					if (string instanceof PsiClassType) {
						extendedTypes.add(PsiTypeUtils.resolveSignature((PsiType) string, false));
					}
				}
			}
			if (primaryType.isInterface() && !extendedTypes.contains("java.lang.Object")) {
				extendedTypes.add("java.lang.Object");
			}
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error while resolving super class Java Types for Java type '"
					+ primaryType.getQualifiedName(), e);
		}
		return extendedTypes;
	}


	@Override
	public String resolveFieldSignature(PsiVariable field) {
		StringBuilder signature = new StringBuilder(field.getName());
		signature.append(" : ");
		try {
			signature.append(PsiTypeUtils.resolveSignature(field.getType(), false));
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error while resolving field type '" + field.getName() + "'", e);
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
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING,
					"Error while resolving method parameters type of '" + method.getName() + "'", e);
		}
		signature.append(')');
		try {
			String returnType = PsiTypeUtils.resolveSignature(method.getReturnType(), false);
			signature.append(" : ");
			signature.append(returnType);
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error while resolving method return type of '" + method.getName() + "'",
					e);
		}
		return signature.toString();
	}

	@Override
	public String resolveLocalVariableSignature(PsiParameter parameter, boolean varargs) {
		return PsiTypeUtils.resolveSignature(parameter, primaryType, varargs);
	}

	@Override
	public String resolveTypeSignature(String typeSignature) {
		return resolveTypeSignature(typeSignature, false);
	}

	private String resolveTypeSignature(String typeSignature, boolean varargs) {
		if (typeSignature.charAt(0) == '[') {
			return doResolveTypeSignature(typeSignature.substring(1, typeSignature.length()))
					+ (varargs ? "..." : "[]");
		}
		return doResolveTypeSignature(typeSignature);
	}

	private String doResolveTypeSignature(String typeSignature) {
		return PsiTypeUtils.getFullQualifiedName(typeSignature, javaProject, new EmptyProgressIndicator());
	}


	protected abstract String resolveSimpleType(String name);
}
