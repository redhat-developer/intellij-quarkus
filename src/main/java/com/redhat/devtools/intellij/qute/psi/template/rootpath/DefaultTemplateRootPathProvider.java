/*******************************************************************************
* Copyright (c) 2024 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.qute.psi.template.rootpath;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils;
import com.redhat.qute.commons.TemplateRootPath;

import java.util.List;

/**
 * Default template root path provider for Qute project (src/main/resources/templates)
 */
public class DefaultTemplateRootPathProvider implements ITemplateRootPathProvider{

	private static final String ORIGIN = "core";
	public static final String TEMPLATES_FOLDER_NAME = "templates/";
	
	@Override
	public boolean isApplicable(Module project) {
		return PsiQuteProjectUtils.hasQuteSupport(project);
	}

	@Override
	public void collectTemplateRootPaths(Module javaProject, List<TemplateRootPath> rootPaths) {
		String templateBaseDir = PsiQuteProjectUtils.getTemplateBaseDir(javaProject, TEMPLATES_FOLDER_NAME);
		rootPaths.add(new TemplateRootPath(templateBaseDir, ORIGIN));		
	}

}
