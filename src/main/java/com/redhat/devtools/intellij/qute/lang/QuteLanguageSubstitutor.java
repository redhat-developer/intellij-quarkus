/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.lang;

import com.intellij.lang.Language;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.RootPolicy;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.LanguageSubstitutor;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils.isQuteTemplate;

/**
 * Qute language substitutor to force some language file (ex:HTML, YAML, etc) to "_Qute" language when:
 * <ul>
 *     <li>the HTML, YAML, etc file is hosted in a Qute project.</li>
 *     <li>the HTML, YAML, etc file is hosted in the src/main/resources/templates folder.</li>
 * </ul>
 */
public class QuteLanguageSubstitutor extends LanguageSubstitutor {
    protected boolean isTemplate(VirtualFile file, Module module) {
        return isQuteTemplate(file, module) &&
                ModuleRootManager.getInstance(module).getFileIndex().isInSourceContent(file);
    }

    protected boolean isQuteModule(Module module) {
        OrderEnumerator libraries = ModuleRootManager.getInstance(module).orderEntries().librariesOnly();
        return libraries.process(new RootPolicy<Boolean>() {
            @Override
            public Boolean visitLibraryOrderEntry(@NotNull LibraryOrderEntry libraryOrderEntry, Boolean value) {
                return value || isQuteLibrary(libraryOrderEntry);
            }
        }, false);

    }

    public static boolean isQuteLibrary(@NotNull LibraryOrderEntry libraryOrderEntry) {
        return libraryOrderEntry != null &&
                libraryOrderEntry.getLibraryName() != null &&
                libraryOrderEntry.getLibraryName().contains("io.quarkus.qute:qute-core:");
    }

    private Module findModule(VirtualFile file) {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                if (ModuleUtilCore.moduleContainsFile(module, file, false)) {
                    return module;
                }
            }
        }
        return null;
    }

    @Override
    public @Nullable Language getLanguage(@NotNull VirtualFile file, @NotNull Project project) {
        Module module = LSPIJUtils.getModule(file, project);
        if (module != null) {
            if (isTemplate(file, module) && isQuteModule(module)) {
                return QuteLanguage.INSTANCE;
            }
        }
        return null;
    }
}
