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

import com.intellij.facet.FacetManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.RootPolicy;
import com.intellij.openapi.roots.impl.OrderEntryUtil;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.quarkus.facet.QuarkusFacet;
import com.redhat.devtools.intellij.quarkus.search.QuarkusModuleComponent;
import com.redhat.devtools.intellij.quarkus.tool.ToolDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuarkusModuleUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusModuleUtil.class);

    public static final Pattern APPLICATION_PROPERTIES = Pattern.compile("application(-.+)?\\.properties");

    public static final Pattern MICROPROFILE_CONFIG_PROPERTIES = Pattern.compile("microprofile-config(-.+)?\\.properties");

    public static final Pattern APPLICATION_YAML = Pattern.compile("application(-.+)?\\.ya?ml");

    /**
     * Check if the module is a Quarkus project. Should check if some class if present
     * but it seems PSI is not available when the module is added thus we rely on the
     * library names.
     *
     * @param module the module to check
     * @return yes if module is a Quarkus project
     */
    public static boolean isQuarkusModule(Module module) {
        OrderEnumerator libraries = ModuleRootManager.getInstance(module).orderEntries().librariesOnly();
        return libraries.process(new RootPolicy<Boolean>() {
            @Override
            public Boolean visitLibraryOrderEntry(@NotNull LibraryOrderEntry libraryOrderEntry, Boolean value) {
                return value || isQuarkusLibrary(libraryOrderEntry);
            }
        }, false);
    }

    public static boolean isQuarkusLibrary(@NotNull LibraryOrderEntry libraryOrderEntry) {
        return libraryOrderEntry.getLibraryName() != null &&
                libraryOrderEntry.getLibraryName().contains(QuarkusConstants.QUARKUS_CORE_PREFIX);
    }

    private static final Pattern QUARKUS_CORE_PATTERN = Pattern.compile("quarkus-core-(\\d[a-zA-Z\\d-.]+?).jar");
    public static final Pattern QUARKUS_STANDARD_VERSIONING = Pattern.compile("(\\d+).(\\d+).(\\d+)(.Final)?(-redhat-\\\\d+)?$");

    /**
     * Checks whether the quarkus version used in this module matches the given predicate.
     * If we're unable to detect the Quarkus version, this method always returns false.
     * The predicate is based on a matcher that is based on the QUARKUS_STANDARD_VERSIONING regular expression,
     * that means that `matcher.group(1)` returns the major version, `matcher.group(2)` returns the minor version,
     * `matcher.group(3)` returns the patch version.
     * If the detected Quarkus version does not follow the standard versioning, the matcher does not match at all.
     * If we can't detect the Quarkus version, the returned value will be the value of the `returnIfNoQuarkusDetected` parameter.
     */
    public static boolean checkQuarkusVersion(Module module, Predicate<Matcher> predicate, boolean returnIfNoQuarkusDetected) {
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

    public static Set<String> getModulesURIs(Project project) {
        Set<String> uris = new HashSet<>();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            uris.add(PsiUtilsLSImpl.getProjectURI(module));
        }
        return uris;
    }

    public static boolean isQuarkusPropertiesFile(VirtualFile file, Project project) {
        if (APPLICATION_PROPERTIES.matcher(file.getName()).matches() ||
                MICROPROFILE_CONFIG_PROPERTIES.matcher(file.getName()).matches()) {
            return isQuarkusModule(file, project);
        }
        return false;
    }

    public static boolean isQuarkusYAMLFile(VirtualFile file, Project project) {
        if (APPLICATION_YAML.matcher(file.getName()).matches()) {
            return isQuarkusModule(file, project);
        }
        return false;
    }

    private static boolean isQuarkusModule(VirtualFile file, Project project) {
        Module module = ModuleUtilCore.findModuleForFile(file, project);
        return module != null && (FacetManager.getInstance(module).getFacetByType(QuarkusFacet.FACET_TYPE_ID) != null || QuarkusModuleUtil.isQuarkusModule(module));
    }

    public static VirtualFile getModuleDirPath(Module module) {
        ModuleRootManager manager = ModuleRootManager.getInstance(module);
        VirtualFile[] roots = manager.getContentRoots();
        if (roots.length > 0) {
            return roots[0];
        } else {
            return VfsUtil.findFileByIoFile(new File(module.getModuleFilePath()).getParentFile(), true);
        }
    }
}
