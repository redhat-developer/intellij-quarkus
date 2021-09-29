/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
* 
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.microprofile.psi.internal.quarkus.providers;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.AbstractStaticPropertiesProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.SearchContext;
import com.redhat.microprofile.psi.quarkus.PsiQuarkusUtils;

/**
 * Abstract class for static Quarkus properties.
 * 
 * @author Angelo ZERR
 *
 */
public abstract class AbstractStaticQuarkusPropertiesProvider extends AbstractStaticPropertiesProvider {

	private static final String PLUGIN_ID = "com.redhat.microprofile.jdt.quarkus";

	public AbstractStaticQuarkusPropertiesProvider(String path) {
		super(path);
	}

	@Override
	protected boolean isAdaptedFor(SearchContext context) {
		Module javaProject = context.getJavaProject();
		return PsiQuarkusUtils.isQuarkusProject(javaProject);
	}

}
