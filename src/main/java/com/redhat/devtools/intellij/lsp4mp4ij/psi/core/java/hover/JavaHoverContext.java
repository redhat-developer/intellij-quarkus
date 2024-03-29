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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.hover;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.AbstractJavaContext;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4j.Position;

/**
 * Java hover context for a given compilation unit.
 * 
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/java/hover/JavaHoverContext.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/java/hover/JavaHoverContext.java</a>
 *
 */
public class JavaHoverContext extends AbstractJavaContext {

	private final Position hoverPosition;

	private final PsiElement hoverElement;

	private final DocumentFormat documentFormat;
	private final boolean surroundEqualsWithSpaces;

	public JavaHoverContext(String uri, PsiFile typeRoot, IPsiUtils utils, Module module, PsiElement hoverElement,
							Position hoverPosition, DocumentFormat documentFormat, boolean surroundEqualsWithSpaces) {
		super(uri, typeRoot, utils, module);
		this.hoverElement = hoverElement;
		this.hoverPosition = hoverPosition;
		this.documentFormat = documentFormat;
		this.surroundEqualsWithSpaces = surroundEqualsWithSpaces;
	}

	public DocumentFormat getDocumentFormat() {
		return documentFormat;
	}

	public PsiElement getHoverElement() {
		return hoverElement;
	}

	public Position getHoverPosition() {
		return hoverPosition;
	}

	/**
	 * Returns true if `=` should be surrounded with spaces in hover items, and false otherwise
	 *
	 * @return true if `=` should be surrounded with spaces in hover items, and false otherwise
	 */
	public boolean isSurroundEqualsWithSpaces() {
		return surroundEqualsWithSpaces;
	}
}
