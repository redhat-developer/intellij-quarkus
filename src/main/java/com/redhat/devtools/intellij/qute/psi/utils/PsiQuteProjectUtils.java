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
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants;
import com.redhat.qute.commons.ProjectInfo;
import io.quarkus.runtime.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaResourceRootType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JDT Qute utilities.
 *
 * @author Angelo ZERR
 */
public class PsiQuteProjectUtils {

    public static final String RESOURCES_BASE_DIR = "src/main/resources";

    public static final String TEMPLATES_FOLDER_NAME = "templates";

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

    /**
     * Returns the full path of the Qute templates base dir '$base-dir-of-module/src/main/resources/templates' for the given module.
     *
     * @param javaProject the Java module project.
     * @return the full path of the Qute templates base dir '$base-dir-of-module/src/main/resources/templates' for the given module.
     */
    private static String getTemplateBaseDir(Module javaProject) {
        VirtualFile resourcesDir = findBestResourcesDir(javaProject);
        if (resourcesDir != null) {
            return LSPIJUtils.toUri(resourcesDir).resolve(TEMPLATES_FOLDER_NAME).toASCIIString();
        }
        return LSPIJUtils.toUri(javaProject).resolve(RESOURCES_BASE_DIR).resolve(TEMPLATES_FOLDER_NAME).toASCIIString();
    }

    public static @NotNull String getRelativeTemplateBaseDir(Module module) {
        VirtualFile resourcesDir = findBestResourcesDir(module);
        return getRelativeTemplateBaseDir(module, resourcesDir);
    }

    public static @NotNull String getRelativeTemplateBaseDir(Module module, @Nullable VirtualFile resourcesDir) {
        String relativeResourcesPath = RESOURCES_BASE_DIR;
        if (resourcesDir != null) {
            for (VirtualFile root : ModuleRootManager.getInstance(module).getContentRoots()) {
                String path = VfsUtilCore.getRelativePath(resourcesDir, root);
                if (path != null) {
                    relativeResourcesPath = path;
                    break;
                }
            }
        }
        return relativeResourcesPath + "/" + TEMPLATES_FOLDER_NAME + "/";
    }

    /**
     * Returns the best 'resources' directory for the given Java module project and null otherwise.
     *
     * @param javaProject the Java module project.
     * @return the best resources dir for the given Java module project.
     */
    public static @Nullable VirtualFile findBestResourcesDir(@NotNull Module javaProject) {
        List<VirtualFile> resourcesDirs = ModuleRootManager.getInstance(javaProject).getSourceRoots(JavaResourceRootType.RESOURCE);
        if (!resourcesDirs.isEmpty()) {
            QuarkusModuleUtil.sortRoot(resourcesDirs); // put root with smallest path first (eliminates generated sources roots)
            // The module configure 'Resources folder'
            // 1) loop for each configured resources dir and returns the first which contains 'templates' folder.
            for (var dir : resourcesDirs) {
                var templatesDir = dir.findChild(TEMPLATES_FOLDER_NAME);
                if (templatesDir != null && templatesDir.exists()) {
                    return dir;
                }
            }
            // 2) no resources directories contains the 'templates' folder,returns the first.
            return resourcesDirs.get(0);
        }
        // Corner usecase, the module doesn't configure 'Resources folder', use the first content roots
        return QuarkusModuleUtil.getModuleDirPath(javaProject);
    }

    /**
     * Returns an array of content roots of the given module sorted with smallest path first (to eliminate generated sources roots) from all content entries.
     *
     * @param module the module
     * @return the array of content roots.
     */
    public static VirtualFile[] getContentRoots(Module module) {
        return QuarkusModuleUtil.getContentRoots(module);
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

    public static TemplatePathInfo getTemplatePath(String templatesBaseDir, String basePath, String className, String methodOrFieldName, boolean ignoreFragments, TemplateNameStrategy templateNameStrategy) {
        String fragmentId = null;
        StringBuilder templateUri = new StringBuilder(templatesBaseDir != null ? templatesBaseDir : "");
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
        templateUri.append(defaultedName(templateNameStrategy, methodOrFieldName));
        return new TemplatePathInfo(templateUri.toString(), fragmentId);
    }

    /**
     * @param defaultNameStrategy
     * @param value
     * @return
     * @see <a href=
     * "https://github.com/quarkusio/quarkus/blob/32392afcd5cbbed86fe119ed90d4c679d4d52123/extensions/qute/deployment/src/main/java/io/quarkus/qute/deployment/QuteProcessor.java#L562C5-L578C6">QuteProcessor#defaultName</a>
     */
    private static String defaultedName(TemplateNameStrategy defaultNameStrategy, String value) {
        switch (defaultNameStrategy) {
            case ELEMENT_NAME:
                return value;
            case HYPHENATED_ELEMENT_NAME:
                return StringUtil.hyphenate(value);
            case UNDERSCORED_ELEMENT_NAME:
                return String.join("_", new Iterable<String>() {
                    @Override
                    public Iterator<String> iterator() {
                        return StringUtil.lowerCase(StringUtil.camelHumpsIterator(value));
                    }
                });
            default:
                return value;
        }
    }

    /**
     * Appends a segment to a path, add trailing "/" if necessary
     *
     * @param path    the path to append to
     * @param segment the segment to append to the path
     */
    public static void appendAndSlash(@NotNull StringBuilder path, @NotNull String segment) {
        path.append(segment);
        if (!segment.endsWith("/")) {
            path.append('/');
        }
    }

    public static boolean isQuteTemplate(VirtualFile file, Module module) {
        return file.getPath().contains(TEMPLATES_FOLDER_NAME) &&
                ModuleRootManager.getInstance(module).getFileIndex().isInSourceContent(file);
    }
}
