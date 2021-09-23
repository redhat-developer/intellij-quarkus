/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.mp4ij.psi.core.java.completion;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.quarkus.search.core.java.AbstractJavaContext;
import com.redhat.devtools.intellij.quarkus.search.core.utils.IPsiUtils;

/**
 * Represents the context of where completion was triggered
 *
 * @author datho7561
 */
public class JavaCompletionContext extends AbstractJavaContext {

	private int offset;

	public JavaCompletionContext(String uri, PsiFile typeRoot, IPsiUtils utils, Module module, int offset) {
		super(uri, typeRoot, utils, module);
		this.offset = offset;
	}

	/**
	 * Returns the offset from the beginning of the document where completion was
	 * triggered
	 *
	 * @return the offset from the beginning of the document where completion was
	 *         triggered
	 */
	public int getOffset() {
		return offset;
	}

}
