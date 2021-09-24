/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.definition;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.AbstractJavaContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.eclipse.lsp4j.Position;

/**
 * Java definition context for a given compilation unit.
 *
 * @author Angelo ZERR
 *
 */
public class JavaDefinitionContext extends AbstractJavaContext {

	private final PsiElement hyperlinkedElement;

	private final Position hyperlinkedPosition;

	public JavaDefinitionContext(String uri, PsiFile typeRoot, IPsiUtils utils, Module module, PsiElement hyperlinkeElement,
								 Position hyperlinkePosition) {
		super(uri, typeRoot, utils, module);
		this.hyperlinkedElement = hyperlinkeElement;
		this.hyperlinkedPosition = hyperlinkePosition;
	}

	/**
	 * Returns the hyperlinked Java element.
	 * 
	 * @return the hyperlinked Java element.
	 */
	public PsiElement getHyperlinkedElement() {
		return hyperlinkedElement;
	}

	/**
	 * Returns the hyperlinked position.
	 * 
	 * @return the hyperlinked position.
	 */
	public Position getHyperlinkedPosition() {
		return hyperlinkedPosition;
	}

}
