/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.utils;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants;
import com.redhat.qute.commons.ProjectInfo;

/**
 * JDT Qute utilities.
 *
 * @author Angelo ZERR
 *
 */
public class PsiQuteProjectUtils {

	private static final String TEMPLATES_BASE_DIR = "src/main/resources/templates/";

	private PsiQuteProjectUtils() {
	}

	public static ProjectInfo getProjectInfo(Module javaProject) {
		String projectUri = getProjectURI(javaProject);
		String templateBaseDir = LSPIJUtils.toUriAsString(QuarkusModuleUtil.getModuleDirPath(javaProject)) + TEMPLATES_BASE_DIR;
		return new ProjectInfo(projectUri, templateBaseDir);
	}

	/**
	 * Returns the project URI of the given project.
	 *
	 * @param project the project
	 * @return the project URI of the given project.
	 */
	public static String getProjectURI(Module project) {
		return LSPIJUtils.getProjectUri(project);
	}

	/**
	 * Returns the project URI of the given project.
	 *
	 * @param project the project
	 * @return the project URI of the given project.
	 */
	public static String getProjectURI(Project project) {
		return LSPIJUtils.getProjectUri(project);
	}

	public static boolean hasQuteSupport(Module javaProject) {
		return PsiTypeUtils.findType(javaProject, QuteJavaConstants.ENGINE_BUILDER_CLASS) != null;
	}

	public static String getTemplatePath(String className, String methodOrFieldName) {
		StringBuilder path = new StringBuilder(TEMPLATES_BASE_DIR);
		if (className != null) {
			path.append(className);
			path.append('/');
		}
		return path.append(methodOrFieldName).toString();
	}

	public static TemplatePathInfo getTemplatePath(String className, String methodOrFieldName, boolean ignoreFragments) {
		String fragmentId = null;
		StringBuilder templateUri = new StringBuilder(TEMPLATES_BASE_DIR);
		if (className != null) {
			templateUri.append(className);
			templateUri.append('/');
		}
		if (!ignoreFragments) {
			int fragmentIndex = methodOrFieldName != null ? methodOrFieldName.lastIndexOf('$') : -1;
			if (fragmentIndex != -1) {
				fragmentId = methodOrFieldName.substring(fragmentIndex + 1, methodOrFieldName.length());
				methodOrFieldName = methodOrFieldName.substring(0, fragmentIndex);
			}
		}
		templateUri.append(methodOrFieldName);
		return new TemplatePathInfo(templateUri.toString(), fragmentId);
	}
}
