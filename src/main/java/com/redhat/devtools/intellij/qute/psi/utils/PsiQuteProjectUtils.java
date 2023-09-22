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
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants;
import com.redhat.qute.commons.ProjectInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JDT Qute utilities.
 *
 * @author Angelo ZERR
 *
 */
public class PsiQuteProjectUtils {

	private static final String TEMPLATES_BASE_DIR = "src/main/resources/templates/";

	/**
	 * Value for Qute annotations indicating behaviour should be using the default
	 */
	private static final String DEFAULTED = "<<defaulted>>";

	private PsiQuteProjectUtils() {
	}

	public static ProjectInfo getProjectInfo(Module javaProject) {
		String projectUri = getProjectURI(javaProject);
		String templateBaseDir =  LSPIJUtils.toUri(javaProject).resolve(TEMPLATES_BASE_DIR).toASCIIString();
		// Project dependencies
		Set<Module> projectDependencies = new HashSet<>();
		ModuleUtilCore.getDependencies(javaProject, projectDependencies);
		return new ProjectInfo(projectUri, projectDependencies
				.stream()
				.filter(projectDependency -> !javaProject.equals(projectDependency))
				.map(projectDependency -> LSPIJUtils.getProjectUri(projectDependency))
				.collect(Collectors.toList()), templateBaseDir);
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

	public static String getTemplatePath(String basePath, String className, String methodOrFieldName) {
		StringBuilder path = new StringBuilder(TEMPLATES_BASE_DIR);
		if (basePath != null && !DEFAULTED.equals(basePath)) {
			appendAndSlash(path, basePath);
		} else if (className != null) {
			appendAndSlash(path, className);
		}
		return path.append(methodOrFieldName).toString();
	}

	public static TemplatePathInfo getTemplatePath(String basePath, String className, String methodOrFieldName, boolean ignoreFragments) {
		String fragmentId = null;
		StringBuilder templateUri = new StringBuilder(TEMPLATES_BASE_DIR);
		if (basePath != null && !DEFAULTED.equals(basePath)) {
			appendAndSlash(templateUri, basePath);
		} else if (className != null) {
			appendAndSlash(templateUri, className);
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

	/**
	 * Appends a segment to a path, add trailing "/" if necessary
	 * @param path the path to append to
	 * @param segment the segment to append to the path
	 */
	public static void appendAndSlash(@NotNull StringBuilder path, @NotNull String segment) {
		path.append(segment);
		if (!segment.endsWith("/")) {
			path.append('/');
		}
	}
}
