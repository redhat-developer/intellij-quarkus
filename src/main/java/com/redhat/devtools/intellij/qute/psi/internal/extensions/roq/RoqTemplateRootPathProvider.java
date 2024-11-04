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
package com.redhat.devtools.intellij.qute.psi.internal.extensions.roq;

import java.util.List;


import com.intellij.java.library.JavaLibraryUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.qute.psi.template.rootpath.ITemplateRootPathProvider;
import com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils;
import com.redhat.devtools.intellij.qute.psi.utils.PsiTypeUtils;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.qute.commons.TemplateRootPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Roq template root path provider for Roq project.
 */
public class RoqTemplateRootPathProvider implements ITemplateRootPathProvider {

	private static final String ORIGIN = "roq";

	private static final String[] TEMPLATES_BASE_DIRS = { "templates/", "content/", "src/main/resources/content/" };

	@Override
	public boolean isApplicable(Module javaProject) {
		return RoqUtils.isRoqProject(javaProject);
	}

	@Override
	public void collectTemplateRootPaths(Module javaProject, List<TemplateRootPath> rootPaths) {
		VirtualFile moduleDir = QuarkusModuleUtil.getModuleDirPath(javaProject);
		if (moduleDir != null) {
			// templates
			String templateBaseDir = LSPIJUtils.toUri(moduleDir).resolve("templates").toASCIIString();
			rootPaths.add(new TemplateRootPath(templateBaseDir, ORIGIN));
			// content
			String contentBaseDir = LSPIJUtils.toUri(moduleDir).resolve("content").toASCIIString();
			rootPaths.add(new TemplateRootPath(contentBaseDir, ORIGIN));
		}
		// src/main/resources/content
		VirtualFile resourcesContentDir = PsiQuteProjectUtils.findBestResourcesDir(javaProject, "content");
		if (resourcesContentDir != null) {
			String contentBaseDir = LSPIJUtils.toUri(resourcesContentDir).resolve("content").toASCIIString();
			rootPaths.add(new TemplateRootPath(contentBaseDir, ORIGIN));
		}
	}

}
