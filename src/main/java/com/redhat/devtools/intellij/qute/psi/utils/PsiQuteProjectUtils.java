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
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants;
import com.redhat.qute.commons.ProjectInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaResourceRootType;

import java.util.*;
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
		String templateBaseDir = getTemplateBaseDir(javaProject);
		// Project dependencies
		Set<Module> projectDependencies = new HashSet<>();
		ModuleUtilCore.getDependencies(javaProject, projectDependencies);
		return new ProjectInfo(projectUri, projectDependencies
				.stream()
				.filter(projectDependency -> !javaProject.equals(projectDependency))
				.map(LSPIJUtils::getProjectUri)
				.collect(Collectors.toList()), templateBaseDir);
	}

	private static String getTemplateBaseDir(Module javaProject) {
		List<VirtualFile> resourcesDirs = ModuleRootManager.getInstance(javaProject).getSourceRoots(JavaResourceRootType.RESOURCE);
		if (!resourcesDirs.isEmpty()) {
			for (var dir : resourcesDirs) {
				var templatesDir = dir.findChild("templates");
				if (templatesDir != null && templatesDir.exists()) {
					return LSPIJUtils.toUri(templatesDir).toASCIIString();
				}
			}
			return LSPIJUtils.toUri(resourcesDirs.get(0)).resolve("templates").toASCIIString();
		}
		return LSPIJUtils.toUri(javaProject).resolve(TEMPLATES_BASE_DIR).toASCIIString();
	}

	public List<VirtualFile> getSortedSourceRoots(Module module) {
		VirtualFile @NotNull [] roots = ModuleRootManager.getInstance(module).getContentRoots();
		Arrays.sort(roots, Comparator.comparingInt(r -> r.getPath().length()));//put root with smallest path first (eliminates generated sources roots)
		return null;
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
				fragmentId = methodOrFieldName.substring(fragmentIndex + 1);
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

	public static boolean isQuteTemplate(VirtualFile file, Module module) {
		return file.getPath().contains("templates") &&
				ModuleRootManager.getInstance(module).getFileIndex().isInSourceContent(file);
	}
}
