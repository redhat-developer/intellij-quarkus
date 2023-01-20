/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.AbstractStaticPropertiesProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.SearchContext;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;

/**
 * MicroProfile static properties provider.
 *
 */
public class StaticPropertyProvider extends AbstractStaticPropertiesProvider {

	private String type;

	public StaticPropertyProvider(String source) {
		super(source);
	}

	/**
	 * Sets the type to be checked that it is on the classpath before collecting its
	 * static properties.
	 * 
	 * @param type type to check that it is on the classpath
	 * @return all the providers.
	 */
	public void setType(String type) {
		this.type = type;
	}

	@Override
	protected boolean isAdaptedFor(SearchContext context) {
		if (type == null) {
			return true;
		} else {
			Module javaProject = context.getJavaProject();
			return (PsiTypeUtils.findType(javaProject, type) != null);
		}
	}

}
