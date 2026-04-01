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

import com.intellij.java.library.JavaLibraryUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.qute.psi.internal.QuteJavaConstants;
import com.redhat.devtools.intellij.qute.psi.internal.template.rootpath.TemplateRootPathProviderRegistry;
import com.redhat.devtools.intellij.qute.psi.template.project.ProjectFeatureProviderRegistry;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.qute.commons.ProjectFeature;
import com.redhat.qute.commons.ProjectInfo;
import com.redhat.qute.commons.TemplateRootPath;
import io.quarkus.runtime.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaResourceRootType;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
     * Value for Qute annotations indicating behaviour should be using the default.
     */
    private static final String DEFAULTED = "<<defaulted>>";

    private static final Key<Boolean> QUTE_PROJECT_KEY = Key.create("quteProject");
    private static final Key<Boolean> QUTE_SUPPORT_KEY = Key.create("quteSupport");

    private PsiQuteProjectUtils() {
    }

    /**
     * Returns true if the given project has Qute support (i.e. at least one module
     * has the Qute library in its dependencies), false otherwise.
     *
     * <p>The result is cached in the project's user data and invalidated when module roots change.</p>
     *
     * @param project the project to check, must not be null.
     * @return true if the project has Qute support, false otherwise.
     */
    public static boolean hasQuteSupport(@NotNull Project project) {
        Boolean cached = project.getUserData(QUTE_PROJECT_KEY);
        if (cached != null) {
            return cached;
        }
        boolean result = false;
        for (Module m : ModuleManager.getInstance(project).getModules()) {
            if (hasQuteSupport(m)) {
                result = true;
                break;
            }
        }
        project.putUserData(QUTE_PROJECT_KEY, result);
        return result;
    }

    /**
     * Returns true if the given module has Qute support (i.e. the Qute library is present
     * in its dependencies), false otherwise.
     *
     * <p>The result is cached in the module's user data and invalidated when module roots change.</p>
     *
     * @param javaProject the module to check, must not be null.
     * @return true if the module has Qute support, false otherwise.
     */
    public static boolean hasQuteSupport(@Nullable Module javaProject) {
        if (javaProject == null) {
            return false;
        }
        Boolean cached = javaProject.getUserData(QUTE_SUPPORT_KEY);
        if (cached != null) {
            return cached;
        }
        boolean result = ApplicationManager.getApplication().runReadAction(
                (Computable<Boolean>) () -> JavaLibraryUtil.hasAnyLibraryJar(javaProject, QuteJavaConstants.QUTE_MAVEN_COORDS));
        javaProject.putUserData(QUTE_SUPPORT_KEY, result);
        return result;
    }

    public static ProjectInfo getProjectInfo(@NotNull Module javaProject) {
        String projectUri = getProjectURI(javaProject);
        // Project dependencies
        Set<Module> projectDependencies = new HashSet<>();
        ModuleUtilCore.getDependencies(javaProject, projectDependencies);

        // Project folder
        VirtualFile moduleDir = QuarkusModuleUtil.getModuleDirPath(javaProject);
        String projectFolder = moduleDir != null ? LSPIJUtils.toUriAsString(moduleDir) : null;
        // Template root paths
        List<TemplateRootPath> templateRootPaths = TemplateRootPathProviderRegistry.getInstance()
                .getTemplateRootPaths(javaProject);
        // Source folders
        Set<String> sourceFolders = getSourceFolders(javaProject);
        // Project Features
        Set<ProjectFeature> projectFeatures = ProjectFeatureProviderRegistry.getInstance()
                .getProjectFeatures(javaProject);
        return new ProjectInfo(projectUri, projectFolder, projectDependencies
                .stream()
                .filter(projectDependency -> !javaProject.equals(projectDependency))
                .map(LSPIJUtils::getProjectUri)
                .collect(Collectors.toList()), templateRootPaths, sourceFolders, projectFeatures);
    }

    private static @NotNull Set<String> getSourceFolders(@NotNull Module javaProject) {
        Set<String> sourceFolders = new HashSet<>();
        ModuleRootManager rootManager = ModuleRootManager.getInstance(javaProject);
        for (ContentEntry entry : rootManager.getContentEntries()) {
            for (SourceFolder sourceFolder : entry.getSourceFolders()) {
                VirtualFile folder = sourceFolder.getFile();
                if (folder != null) {
                    sourceFolders.add(LSPIJUtils.toUriAsString(folder));
                }
            }
        }
        return sourceFolders;
    }

    public static String getRelativeResourcesFolder(@NotNull Module javaProject) {
        VirtualFile resourcesDir = findBestResourcesDir(javaProject);
        if (resourcesDir != null) {
            for (VirtualFile root : ModuleRootManager.getInstance(javaProject).getContentRoots()) {
                String path = VfsUtilCore.getRelativePath(resourcesDir, root);
                if (path != null) {
                    return path;
                }
            }
        }
        return RESOURCES_BASE_DIR;
    }

    /**
     * Returns the full path of the Qute templates base dir
     * '$base-dir-of-module/src/main/resources/templates' for the given module.
     *
     * @param javaProject        the Java module project, must not be null.
     * @param templateFolderName the name of the templates folder, must not be null.
     * @return the full path of the Qute templates base dir for the given module.
     */
    public static String getTemplateBaseDir(@NotNull Module javaProject, @NotNull String templateFolderName) {
        VirtualFile resourcesDir = findBestResourcesDir(javaProject);
        if (resourcesDir != null) {
            return LSPIJUtils.toUri(resourcesDir).resolve(templateFolderName).toASCIIString();
        }
        return LSPIJUtils.toUri(javaProject).resolve(RESOURCES_BASE_DIR).resolve(templateFolderName).toASCIIString();
    }

    /**
     * Returns the full path of the Qute templates base dir
     * '$base-dir-of-module/src/main/resources/templates' for the given module.
     *
     * @param javaProject the Java module project, must not be null.
     * @return the full path of the Qute templates base dir for the given module.
     */
    private static String getTemplateBaseDir(@NotNull Module javaProject) {
        VirtualFile resourcesDir = findBestResourcesDir(javaProject);
        if (resourcesDir != null) {
            return LSPIJUtils.toUri(resourcesDir).resolve(TEMPLATES_FOLDER_NAME).toASCIIString();
        }
        return LSPIJUtils.toUri(javaProject).resolve(RESOURCES_BASE_DIR).resolve(TEMPLATES_FOLDER_NAME).toASCIIString();
    }

    public static @NotNull String getRelativeTemplateBaseDir(@NotNull Module module) {
        VirtualFile resourcesDir = findBestResourcesDir(module);
        return getRelativeTemplateBaseDir(module, resourcesDir);
    }

    public static @NotNull String getRelativeTemplateBaseDir(@NotNull Module module, @Nullable VirtualFile resourcesDir) {
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

    public static @Nullable VirtualFile findBestResourcesDir(@NotNull Module javaProject) {
        return findBestResourcesDir(javaProject, TEMPLATES_FOLDER_NAME);
    }

    /**
     * Returns the best 'resources' directory for the given Java module project and null otherwise.
     *
     * @param javaProject         the Java module project, must not be null.
     * @param templatesFolderName the name of the templates folder, must not be null.
     * @return the best resources dir for the given Java module project, or null if not found.
     */
    public static @Nullable VirtualFile findBestResourcesDir(@NotNull Module javaProject, @NotNull String templatesFolderName) {
        List<VirtualFile> resourcesDirs = ModuleRootManager.getInstance(javaProject).getSourceRoots(JavaResourceRootType.RESOURCE);
        if (!resourcesDirs.isEmpty()) {
            QuarkusModuleUtil.sortRoot(resourcesDirs);
            for (var dir : resourcesDirs) {
                var templatesDir = dir.findChild(templatesFolderName);
                if (templatesDir != null && templatesDir.exists()) {
                    return dir;
                }
            }
            return resourcesDirs.get(0);
        }
        return QuarkusModuleUtil.getModuleDirPath(javaProject);
    }

    /**
     * Returns an array of content roots of the given module sorted with smallest path first
     * (to eliminate generated sources roots) from all content entries.
     *
     * @param module the module, must not be null.
     * @return the array of content roots.
     */
    public static VirtualFile[] getContentRoots(@NotNull Module module) {
        return QuarkusModuleUtil.getContentRoots(module);
    }

    /**
     * Returns the project URI of the given module.
     *
     * @param project the module, must not be null.
     * @return the project URI of the given module.
     */
    public static String getProjectURI(@NotNull Module project) {
        return LSPIJUtils.getProjectUri(project);
    }

    /**
     * Returns the project URI of the given project.
     *
     * @param project the project, must not be null.
     * @return the project URI of the given project.
     */
    public static String getProjectURI(@NotNull Project project) {
        return LSPIJUtils.getProjectUri(project);
    }

    /**
     * Returns true if the given virtual file is a Qute template in the given module, false otherwise.
     *
     * @param file   the virtual file to check, must not be null.
     * @param module the module to check against, must not be null.
     * @return true if the file is a Qute template, false otherwise.
     */
    public static boolean isQuteTemplate(@NotNull VirtualFile file, @NotNull Module module) {
        return ApplicationManager.getApplication().isReadAccessAllowed() ?
                internalIsQuteTemplate(file, module) :
                (Boolean) ReadAction.compute(() -> internalIsQuteTemplate(file, module));
    }

    public static boolean maybeBinaryQuteTemplate(@NotNull VirtualFile file) {
        return file.getPath().contains("!/templates/");
    }

    private static boolean internalIsQuteTemplate(@NotNull VirtualFile file, @NotNull Module module) {
        if (file.getPath().contains(TEMPLATES_FOLDER_NAME) &&
                ModuleRootManager.getInstance(module).getFileIndex().isInSourceContent(file)) {
            return true;
        }
        ProjectInfo projectInfo = PsiQuteProjectUtils.getProjectInfo(module);
        if (projectInfo == null) {
            return false;
        }

        if (file.isInLocalFileSystem()) {
            // Local file system
            Path templatePath = Paths.get(file.getPath());
            for (TemplateRootPath rootPath : projectInfo.getTemplateRootPaths()) {
                if (rootPath.isIncluded(templatePath)) {
                    return true;
                }
            }
            return false;
        }

        // Ex: WSL
        URI templatePath = LSPIJUtils.toUri(file);
        for (TemplateRootPath rootPath : projectInfo.getTemplateRootPaths()) {
            URI rootPathUri = URI.create(rootPath.getBaseDir());
            if (uriIncludes(rootPathUri, templatePath)) {
                return true;
            }
        }

        return false;
    }

    private static boolean uriIncludes(URI parent, URI child) {
        return child.getPath().startsWith(parent.getPath());
    }

    public static TemplatePathInfo getTemplatePath(@Nullable String templatesBaseDir, @Nullable String basePath, @Nullable String className, @Nullable String methodOrFieldName, boolean ignoreFragments, @NotNull TemplateNameStrategy templateNameStrategy) {
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

    private static String defaultedName(@NotNull TemplateNameStrategy defaultNameStrategy, @Nullable String value) {
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
     * Appends a segment to a path, adding a trailing "/" if necessary.
     *
     * @param path    the path to append to, must not be null.
     * @param segment the segment to append to the path, must not be null.
     */
    public static void appendAndSlash(@NotNull StringBuilder path, @NotNull String segment) {
        path.append(segment);
        if (!segment.endsWith("/")) {
            path.append('/');
        }
    }

    public static void invalidateCache(@NotNull Project project) {
        project.putUserData(QUTE_PROJECT_KEY, null);
        for (Module m : ModuleManager.getInstance(project).getModules()) {
            m.putUserData(QUTE_SUPPORT_KEY, null);
        }
    }

    public static URI resolveRelativePath(@NotNull VirtualFile file,
                                          @NotNull String... segments) {
        return resolveRelativePath(LSPIJUtils.toUri(file), segments);
    }

    public static URI resolveRelativePath(@NotNull URI base,
                                          @NotNull String... segments) {
        URI result = base;
        for (String segment : segments) {
            if (segment == null || segment.isEmpty()) {
                continue;
            }
            // Ensure segment ends with / to avoid replacing the last path component
            String normalized = segment.endsWith("/") ? segment : segment + "/";
            // Ensure segment does not start with / to avoid replacing the whole path
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            result = result.resolve(normalized);
        }
        return result;
    }
}