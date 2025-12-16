/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java;

import java.util.*;

import com.intellij.openapi.module.Module;
import com.intellij.psi.*;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProject;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.eclipse.lsp4mp.commons.runtime.EnumConstantsProvider;
import org.eclipse.lsp4mp.commons.runtime.MicroProfileProjectRuntime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract class for Java context for a given compilation unit.
 * 
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/java/AbtractJavaContext.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/java/AbtractJavaContext.java</a>
 *
 */
public abstract class AbstractJavaContext {

	private final String uri;

	private final PsiFile typeRoot;

	private final IPsiUtils utils;
	private final Module module;

	private Map<String, Object> cache;

	private @Nullable MicroProfileProjectRuntime projectRuntime;

	public AbstractJavaContext(String uri, PsiFile typeRoot, IPsiUtils utils, Module module) {
		this.uri = uri;
		this.typeRoot = typeRoot;
		this.utils = utils;
		this.module = module;
	}

	public String getUri() {
		return uri;
	}

	public PsiFile getTypeRoot() {
		return typeRoot;
	}

	public Module getJavaProject() {
		return module;
	}

	public IPsiUtils getUtils() {
		return utils;
	}
	

	/**
	 * Associates the specified value with the specified key in the cache.
	 * 
	 * @param key   the key.
	 * @param value the value.
	 */
	public void put(String key, Object value) {
		if (cache == null) {
			cache = new HashMap<>();
		}
		cache.put(key, value);
	}

	/**
	 * Returns the value to which the specified key is mapped, or {@code null} if
	 * this map contains no mapping for the key.
	 * 
	 * @param key the key.
	 * @return the value to which the specified key is mapped, or {@code null} if
	 *         this map contains no mapping for the key.
	 */
	public Object get(String key) {
		if (cache == null) {
			return null;
		}
		return cache.get(key);
	}

	public PsiFile getASTRoot() {
		return getTypeRoot();
	}

	/**
	 * @param root The ASTRoot to set.
	 */
	public void setASTRoot(PsiFile root) {
	}

	public MicroProfileProjectRuntime getProjectRuntime() {
		if (projectRuntime != null) {
			return projectRuntime;
		}
		PsiMicroProfileProject mpProject = getMicroProfileProject();
		projectRuntime = mpProject.getProjectRuntime();
		return projectRuntime;
	}

	public @NotNull PsiMicroProfileProject getMicroProfileProject() {
		return PsiMicroProfileProjectManager.getInstance(getJavaProject().getProject())
				.getMicroProfileProject(getJavaProject());
	}

	protected static String toQualifiedTypeString(@NotNull PsiType type,
												@NotNull EnumConstantsProvider.SimpleEnumConstantsProvider provider) {

		if (type instanceof PsiPrimitiveType) {
			return type.getPresentableText(); // int, char, etc.
		}

		if (type instanceof PsiArrayType arrayType) {
			return toQualifiedTypeString(arrayType.getComponentType(), provider) + "[]";
		}

		if (type instanceof PsiClassType classType) {
			PsiClass psiClass = classType.resolve();
			if (psiClass == null) {
				return type.getCanonicalText();
			}

			String qualifiedName = getBinaryName(psiClass);
			if (qualifiedName == null) {
				qualifiedName = psiClass.getName();
			}

			// ENUM SUPPORT
			if (psiClass.isEnum()) {
				List<String> enumConstants = new ArrayList<>();
				for (PsiField field : psiClass.getFields()) {
					if (field instanceof PsiEnumConstant enumConst) {
						enumConstants.add(enumConst.getName());
					}
				}
				provider.addEnumConstants(qualifiedName, enumConstants);
			}

			// GENERIC TYPE ARGUMENTS
			PsiType[] parameters = classType.getParameters();
			if (parameters.length == 0) {
				return qualifiedName;
			}

			StringBuilder sb = new StringBuilder(qualifiedName);
			sb.append("<");
			for (int i = 0; i < parameters.length; i++) {
				if (i > 0) sb.append(", ");
				sb.append(toQualifiedTypeString(parameters[i], provider));
			}
			sb.append(">");
			return sb.toString();
		}

		return type.getCanonicalText();
	}

	/**
	 * Returns the JVM binary name of a PsiClass.
	 *
	 * Example:
	 *   - Top-level class: com.example.Foo            -> com.example.Foo
	 *   - Inner class:     com.example.Outer.Inner    -> com.example.Outer$Inner
	 *   - Nested inner:    Outer.Inner.Deep           -> com.example.Outer$Inner$Deep
	 *
	 * This replicates the behavior of JDT's ITypeBinding.getBinaryName().
	 */
	private static @Nullable String getBinaryName(@NotNull PsiClass psiClass) {
		String qualifiedName = psiClass.getQualifiedName();
		if (qualifiedName == null) {
			return null;
		}

		// If the class is top-level, the qualified name is already correct.
		PsiClass outerClass = psiClass.getContainingClass();
		if (outerClass == null) {
			return qualifiedName;
		}

		// Build the binary name by collecting all containing classes
		// and joining them using '$'.
		List<String> classNames = new ArrayList<>();
		PsiClass current = psiClass;

		while (current != null) {
			String name = current.getName();
			if (name != null) {
				classNames.add(name);
			}
			current = current.getContainingClass();
		}

		// Reverse to get Outer -> Inner -> Deep
		Collections.reverse(classNames);

		// Extract the package prefix (everything before the first class name)
		int lastDot = qualifiedName.indexOf(classNames.get(0));
		String packagePrefix = lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";

		// Combine package and nested class names using '$'
		return packagePrefix + String.join("$", classNames);
	}

}
