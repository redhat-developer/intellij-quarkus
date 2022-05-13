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

import java.util.HashMap;
import java.util.Map;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;

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


}
