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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codeaction;

import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;

/**
 * Similar to {@see JavaCodeActionContext}, but includes additional information
 * needed for code action resolve.
 *
 * @auhtod datho7561
 */
public class JavaCodeActionResolveContext extends JavaCodeActionContext {

	private final CodeAction unresolved;

	public JavaCodeActionResolveContext(PsiFile typeRoot, int selectionOffset, int selectionLength, IPsiUtils utils,
										MicroProfileJavaCodeActionParams params, CodeAction unresolved) {
		super(typeRoot, selectionOffset, selectionLength, utils, params);
		this.unresolved = unresolved;
		this.source = this;
	}

	public JavaCodeActionResolveContext(PsiFile typeRoot, int selectionOffset, int selectionLength, IPsiUtils utils,
										MicroProfileJavaCodeActionParams params, CodeAction unresolved, JavaCodeActionResolveContext source) {
		super(typeRoot, selectionOffset, selectionLength, utils, params);
		this.unresolved = unresolved;
		this.source = source;
	}

	/**
	 * Return a copy of the context with its own in memory compilation unit
	 *
	 * @return the new context
	 */
	@Override
	public JavaCodeActionResolveContext copy() {
		return new JavaCodeActionResolveContext(getTypeRoot().getViewProvider().clone().getPsi(getTypeRoot().getLanguage()), super.getSelectionOffset(),
				super.getSelectionLength(), getUtils(), super.getParams(), this.unresolved, getSource());
	}

	@Override
	public JavaCodeActionResolveContext getSource() {
		return (JavaCodeActionResolveContext)  super.getSource();
	}

	public CodeAction getUnresolved() {
		return this.unresolved;
	}

}
