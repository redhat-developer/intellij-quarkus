/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.internal.template.resolvedtype;

import static com.redhat.devtools.intellij.qute.psi.QuteSupportForTemplate.createTypeResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.*;


import com.intellij.psi.impl.java.stubs.PsiModifierListStub;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.AbstractTypeResolver;
import com.redhat.devtools.intellij.qute.psi.internal.resolver.ITypeResolver;
import com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils;
import com.redhat.devtools.intellij.qute.psi.utils.QuteReflectionAnnotationUtils;
import com.redhat.qute.commons.InvalidMethodReason;
import com.redhat.qute.commons.JavaFieldInfo;
import com.redhat.qute.commons.JavaMethodInfo;
import com.redhat.qute.commons.ResolvedJavaTypeInfo;

/**
 * Abstract class for {@link ResolvedJavaTypeInfo} factory.
 * 
 * @author Angelo ZERR
 *
 */
public abstract class AbstractResolvedJavaTypeFactory implements IResolvedJavaTypeFactory {

	private static final Logger LOGGER = Logger.getLogger(AbstractResolvedJavaTypeFactory.class.getName());

	@Override
	public ResolvedJavaTypeInfo create(PsiClass type, Module javaProject)  {
		ITypeResolver typeResolver = createTypeResolver(type, javaProject);
		String typeSignature = AbstractTypeResolver.resolveJavaTypeSignature(type);
		
		// 1) Collect fields
		List<JavaFieldInfo> fieldsInfo = new ArrayList<>();

		// Standard fields
		PsiField[] fields = type.getFields();
		for (PsiField field : fields) {
			if (isValidField(field, type)) {
				// Only public fields are available
				JavaFieldInfo info = new JavaFieldInfo();
				info.setSignature(typeResolver.resolveFieldSignature(field));
				fieldsInfo.add(info);
			}
		}

		// Record fields
		if (type.isRecord()) {
			for (PsiRecordComponent field : type.getRecordComponents()) {
				if (isValidRecordField(field, type)) {
					// All record components are valid
					JavaFieldInfo info = new JavaFieldInfo();
					info.setSignature(typeResolver.resolveFieldSignature(field));
					fieldsInfo.add(info);
				}
			}
		}

		// 2) Collect methods
		List<JavaMethodInfo> methodsInfo = new ArrayList<>();
		Map<String, InvalidMethodReason> invalidMethods = new HashMap<>();
		PsiMethod[] methods = type.getMethods();
		for (PsiMethod method : methods) {
			if (isValidMethod(method, type)) {
				try {
					InvalidMethodReason invalid = getValidMethodForQute(method, typeSignature);
					if (invalid != null) {
						invalidMethods.put(method.getName(), invalid);
					} else {
						JavaMethodInfo info = createMethod(method, typeResolver);
						methodsInfo.add(info);
					}
				} catch (ProcessCanceledException e) {
					//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
					//TODO delete block when minimum required version is 2024.2
					throw e;
				} catch (IndexNotReadyException | CancellationException e) {
					throw e;
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE,
							"Error while getting method signature of '" + method.getName() + "'.", e);
				}
			}
		}

		ResolvedJavaTypeInfo resolvedType = new ResolvedJavaTypeInfo();
		resolvedType.setBinary(type instanceof PsiCompiledElement);
		resolvedType.setSignature(typeSignature);
		resolvedType.setFields(fieldsInfo);
		resolvedType.setMethods(methodsInfo);
		resolvedType.setInvalidMethods(invalidMethods);
		resolvedType.setExtendedTypes(typeResolver.resolveExtendedType());
		resolvedType.setJavaTypeKind(PsiTypeUtils.getJavaTypeKind(type));
		QuteReflectionAnnotationUtils.collectAnnotations(resolvedType, type, typeResolver, javaProject);
		return resolvedType;
	}

	protected JavaMethodInfo createMethod(PsiMethod method, ITypeResolver typeResolver) {
		JavaMethodInfo info = new JavaMethodInfo();
		info.setSignature(typeResolver.resolveMethodSignature(method));
		return info;
	}

	protected abstract boolean isValidRecordField(PsiRecordComponent field, PsiClass type);

	protected abstract boolean isValidField(PsiField field, PsiClass type);

	protected boolean isValidMethod(PsiMethod method, PsiClass type) {
		try {
			if (method.isConstructor() || !method.isValid() || isSynthetic(method)) {
				return false;
			}
			if (!type.isInterface() && !method.getModifierList().hasExplicitModifier(PsiModifier.PUBLIC)) {
				return false;
			}
		} catch (ProcessCanceledException e) {
			//Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
			//TODO delete block when minimum required version is 2024.2
			throw e;
		} catch (IndexNotReadyException | CancellationException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error while checking if '" + method.getName() + "' is valid.", e);
			return false;
		}
		return true;
	}

	private static boolean isSynthetic(PsiMember member) {
		var modifiers = member.getModifierList();
		var result = false;

		if (modifiers instanceof StubBasedPsiElementBase) {
			PsiModifierListStub stub = (PsiModifierListStub) ((StubBasedPsiElementBase<?>) modifiers).getGreenStub();
			result = stub != null && (stub.getModifiersMask() & 0x00001000) == 0x00001000;
		}
		return result;
	}

	protected abstract InvalidMethodReason getValidMethodForQute(PsiMethod method, String typeName);
}