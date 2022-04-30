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
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPIJUtils;
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
		String templateBaseDir = LSPIJUtils.toUri(QuarkusModuleUtil.getModuleDirPath(javaProject).findFileByRelativePath(TEMPLATES_BASE_DIR)).toString();
		return new ProjectInfo(projectUri, templateBaseDir);
	}

	/**
	 * Returns the project URI of the given project.
	 *
	 * @param project the java project
	 * @return the project URI of the given project.
	 */
	public static String getProjectUri(Module project) {
		return getProjectURI(project);
	}

	/**
	 * returns the project URI of the given project.
	 *
	 * @param project the project
	 * @return the project URI of the given project.
	 */
	public static String getProjectURI(Module project) {
		return project.getName(); // .getLocation().toOSString();
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
}
