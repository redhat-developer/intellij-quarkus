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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.codelens;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.AbstractJavaContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeLensParams;

/**
 * Java codeLens context for a given compilation unit.
 *
 * @author Angelo ZERR
 *
 */
public class JavaCodeLensContext extends AbstractJavaContext {

	private final MicroProfileJavaCodeLensParams params;

	public JavaCodeLensContext(String uri, PsiFile typeRoot, IPsiUtils utils, Module module,
							   MicroProfileJavaCodeLensParams params) {
		super(uri, typeRoot, utils, module);
		this.params = params;
	}

	public MicroProfileJavaCodeLensParams getParams() {
		return params;
	}

}
