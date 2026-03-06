/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus;

import com.intellij.ProjectTopics;
import com.intellij.facet.FacetManager;
import com.intellij.java.library.JavaLibraryUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProject;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.quarkus.facet.QuarkusFacet;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuarkusModuleUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusModuleUtil.class);

    private static final Pattern QUARKUS_CORE_PATTERN = Pattern.compile("quarkus-core-(\\d[a-zA-Z\\d-.]+?).jar");

    public static final Pattern QUARKUS_STANDARD_VERSIONING = Pattern.compile("(\\d+).(\\d+).(\\d+)(.Final)?(-redhat-\\\\d+)?$");

    public static final Pattern APPLICATION_PROPERTIES = Pattern.compile("application(-.+)?\\.properties");

    public static final Pattern MICROPROFILE_CONFIG_PROPERTIES = Pattern.compile("microprofile-config(-.+)?\\.properties");

    public static final Pattern APPLICATION_YAML = Pattern.compile("application(-.+)?\\.ya?ml");

    private static final Comparator<VirtualFile> ROOT_COMPARATOR = Comparator.comparingInt(r -> r.getPath().length());

    private static final Key<Boolean> QUARKUS_MODULE_KEY = Key.create("quarkusModule");
    private static final Key<Boolean> QUARKUS_WEB_APP_MODULE_KEY = Key.create("quarkusWebAppModule");

    static {
        // Invalidate module caches when module roots change
        // (e.g. Quarkus library added or removed).
        ApplicationManager.getApplication().getMessageBus()
                .connect()
                .subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
                    @Override
                    public void rootsChanged(@NotNull ModuleRootEvent event) {
                        Object source = event.getSource();
                        if (source instanceof Project project) {
                            for (Module m : ModuleManager.getInstance(project).getModules()) {
                                m.putUserData(QUARKUS_MODULE_KEY, null);
                                m.putUserData(QUARKUS_WEB_APP_MODULE_KEY, null);
                            }
                        }
                    }
                });
    }

    /**
     * Check if the module is a Quarkus project by checking if the Quarkus core library
     * is present in its dependencies.
     *
     * <p>The result is cached in the module's user data and invalidated when module roots change.</p>
     *
     * @param module the module to check, may be null.
     * @return true if the module is a Quarkus project, false otherwise.
     */
    public static boolean isQuarkusModule(@Nullable Module module) {
        if (module == null) {
            return false;
        }
        Boolean cached = module.getUserData(QUARKUS_MODULE_KEY);
        if (cached != null) {
            return cached;
        }
        boolean result = ApplicationManager.getApplication().runReadAction(
                (Computable<Boolean>) () -> JavaLibraryUtil.hasAnyLibraryJar(module, QuarkusConstants.QUARKUS_CORE_MAVEN_COORDS));
        module.putUserData(QUARKUS_MODULE_KEY, result);
        return result;
    }

    /**
     * Check if the module is a Quarkus Web Application project by checking if the
     * Quarkus vertx-http library is present in its dependencies.
     *
     * <p>The result is cached in the module's user data and invalidated when module roots change.</p>
     *
     * @param module the module to check, may be null.
     * @return true if the module is a Quarkus Web Application project, false otherwise.
     */
    public static boolean isQuarkusWebAppModule(@Nullable Module module) {
        if (module == null) {
            return false;
        }
        Boolean cached = module.getUserData(QUARKUS_WEB_APP_MODULE_KEY);
        if (cached != null) {
            return cached;
        }
        boolean result = ApplicationManager.getApplication().runReadAction(
                (Computable<Boolean>) () -> JavaLibraryUtil.hasAnyLibraryJar(module, QuarkusConstants.QUARKUS_VERTX_HTTP_MAVEN_COORDS));
        module.putUserData(QUARKUS_WEB_APP_MODULE_KEY, result);
        return result;
    }

    /**
     * Checks whether the Quarkus version used in this module matches the given predicate.
     * If we're unable to detect the Quarkus version, this method always returns false.
     * The predicate is based on a matcher that is based on the QUARKUS_STANDARD_VERSIONING regular expression,
     * that means that {@code matcher.group(1)} returns the major version, {@code matcher.group(2)} returns
     * the minor version, {@code matcher.group(3)} returns the patch version.
     * If the detected Quarkus version does not follow the standard versioning, the matcher does not match at all.
     *
     * @param module                      the module to check, must not be null.
     * @param predicate                   the predicate to apply to the version matcher, must not be null.
     * @param returnIfNoQuarkusDetected   the value to return if no Quarkus version is detected.
     * @return true if the Quarkus version matches the predicate, false otherwise.
     */
    public static boolean checkQuarkusVersion(@NotNull Module module, @NotNull Predicate<Matcher> predicate, boolean returnIfNoQuarkusDetected) {
        Optional<VirtualFile> quarkusCoreJar = Arrays.stream(ModuleRootManager.getInstance(module).orderEntries()
                        .runtimeOnly()
                        .classes()
                        .getRoots())
                .filter(f -> Pattern.matches(QUARKUS_CORE_PATTERN.pattern(), f.getName()))
                .findFirst();
        if (quarkusCoreJar.isPresent()) {
            Matcher quarkusCoreArtifactMatcher = QUARKUS_CORE_PATTERN.matcher(quarkusCoreJar.get().getName());
            if (quarkusCoreArtifactMatcher.matches()) {
                String quarkusVersion = quarkusCoreArtifactMatcher.group(1);
                LOGGER.debug("Detected Quarkus version = {}", quarkusVersion);
                Matcher quarkusVersionMatcher = QUARKUS_STANDARD_VERSIONING.matcher(quarkusVersion);
                return predicate.test(quarkusVersionMatcher);
            } else {
                return false;
            }
        } else {
            return returnIfNoQuarkusDetected;
        }
    }

    public static Set<String> getModulesURIs(@NotNull Project project) {
        Set<String> uris = new HashSet<>();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            uris.add(PsiUtilsLSImpl.getProjectURI(module));
        }
        return uris;
    }

    public static boolean isQuarkusPropertiesFile(@NotNull VirtualFile file, @NotNull Project project) {
        if (APPLICATION_PROPERTIES.matcher(file.getName()).matches() ||
                MICROPROFILE_CONFIG_PROPERTIES.matcher(file.getName()).matches()) {
            return isQuarkusModule(file, project);
        }
        return false;
    }

    public static boolean isQuarkusYamlFile(@NotNull VirtualFile file) {
        return APPLICATION_YAML.matcher(file.getName()).matches();
    }

    public static boolean isQuarkusYamlFile(@NotNull VirtualFile file, @NotNull Project project) {
        if (isQuarkusYamlFile(file)) {
            return isQuarkusModule(file, project);
        }
        return false;
    }

    private static boolean isQuarkusModule(@NotNull VirtualFile file, @NotNull Project project) {
        Module module = LSPIJUtils.getModule(file, project);
        return module != null && (FacetManager.getInstance(module).getFacetByType(QuarkusFacet.FACET_TYPE_ID) != null || QuarkusModuleUtil.isQuarkusModule(module));
    }

    public static @Nullable VirtualFile getModuleDirPath(@NotNull Module module) {
        VirtualFile[] roots = getContentRoots(module);
        if (roots.length > 0) {
            return roots[0];
        }
        return VfsUtil.findFileByIoFile(new File(ModuleUtilCore.getModuleDirPath(module)), true);
    }

    /**
     * Returns an array of content roots of the given module sorted with smallest path first
     * (to eliminate generated sources roots) from all content entries.
     *
     * @param module the module, must not be null.
     * @return the array of content roots.
     */
    public static VirtualFile[] getContentRoots(@NotNull Module module) {
        VirtualFile[] roots = ModuleRootManager.getInstance(module).getContentRoots();
        if (roots.length <= 1) {
            return roots;
        }
        sortRoot(roots);
        return roots;
    }

    public static void sortRoot(@NotNull List<VirtualFile> roots) {
        Collections.sort(roots, ROOT_COMPARATOR);
    }

    public static void sortRoot(@NotNull VirtualFile[] roots) {
        Arrays.sort(roots, ROOT_COMPARATOR);
    }

    public static String getApplicationUrl(@NotNull PsiMicroProfileProject mpProject) {
        int port = getPort(mpProject);
        String path = mpProject.getProperty("quarkus.http.root-path", "/");
        return "http://localhost:" + port + normalize(path);
    }

    public static String getDevUIUrl(@NotNull PsiMicroProfileProject mpProject) {
        int port = getPort(mpProject);
        String path = mpProject.getProperty("quarkus.http.non-application-root-path", "q");
        if (!path.startsWith("/")) {
            String rootPath = mpProject.getProperty("quarkus.http.root-path", "/");
            path = normalize(rootPath) + path;
        }
        return "http://localhost:" + port + normalize(path) + "dev";
    }

    private static String normalize(@NotNull String path) {
        StringBuilder builder = new StringBuilder(path);
        if (builder.isEmpty() || builder.charAt(0) != '/') {
            builder.insert(0, '/');
        }
        if (builder.charAt(builder.length() - 1) != '/') {
            builder.append('/');
        }
        return builder.toString();
    }

    private static int getPort(@NotNull PsiMicroProfileProject mpProject) {
        int port = mpProject.getPropertyAsInteger("quarkus.http.port", 8080);
        return mpProject.getPropertyAsInteger("%dev.quarkus.http.port", port);
    }
}