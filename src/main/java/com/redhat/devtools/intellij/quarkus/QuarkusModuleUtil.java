/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.RootPolicy;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.redhat.devtools.intellij.quarkus.search.PsiUtilsImpl;
import com.redhat.devtools.intellij.quarkus.search.QuarkusModuleComponent;
import com.redhat.devtools.intellij.quarkus.tool.ToolDelegate;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

public class QuarkusModuleUtil {
    /**
     * Check if the Quarkus library needs to be recomputed and update it if required.
     *
     * @param module the module to check
     */
    public static void ensureQuarkusLibrary(Module module) {
        System.out.println("Ensuring library to " + module.getName());
        long start = System.currentTimeMillis();
        ToolDelegate toolDelegate = ToolDelegate.getDelegate(module);
        if (toolDelegate != null) {
            System.out.println("Tool delegate found for " + module.getName());
            if (isQuarkusModule(module)) {
                System.out.println("isQuarkus module " + module.getName());
                Integer previousHash = module.getComponent(QuarkusModuleComponent.class).getHash();
                Integer actualHash = computeHash(module);
                if (actualHash != null && !actualHash.equals(previousHash)) {
                    ModuleRootModificationUtil.updateModel(module, model -> {
                        LibraryTable table = model.getModuleLibraryTable();
                        Library library = table.getLibraryByName(QuarkusConstants.QUARKUS_DEPLOYMENT_LIBRARY_NAME);
                        while (library != null) {
                            table.removeLibrary(library);
                            library = table.getLibraryByName(QuarkusConstants.QUARKUS_DEPLOYMENT_LIBRARY_NAME);
                        }
                        List<VirtualFile>[] files = toolDelegate.getDeploymentFiles(module);
                        System.out.println("Adding library to " + module.getName() + "previousHash=" + previousHash + " newHash=" + actualHash);
                        addLibrary(model, files);
                    });
                    module.getComponent(QuarkusModuleComponent.class).setHash(actualHash);
                }
            }
        }
        System.out.println("ensureQuarkusLibrary ran in " + (System.currentTimeMillis() - start));
    }

    private static void addLibrary(ModifiableRootModel model, List<VirtualFile>[] files) {
        LibraryEx library = (LibraryEx)model.getModuleLibraryTable().createLibrary(QuarkusConstants.QUARKUS_DEPLOYMENT_LIBRARY_NAME);
        LibraryEx.ModifiableModelEx libraryModel = library.getModifiableModel();

        for (VirtualFile rootFile : files[ToolDelegate.BINARY]) {
            libraryModel.addRoot(rootFile, OrderRootType.CLASSES);
        }
        for (VirtualFile rootFile : files[ToolDelegate.SOURCES]) {
            libraryModel.addRoot(rootFile, OrderRootType.SOURCES);
        }

        LibraryOrderEntry entry = model.findLibraryOrderEntry(library);
        assert entry != null : library;
        entry.setScope(DependencyScope.PROVIDED);
        entry.setExported(false);
        ApplicationManager.getApplication().invokeAndWait(() -> WriteAction.run(libraryModel::commit));
    }

    private static Integer computeHash(Module module) {
        ModuleRootManager manager = ModuleRootManager.getInstance(module);
        Set<String> files = manager.processOrder(new RootPolicy<Set<String>>() {
            @Override
            public Set<String> visitLibraryOrderEntry(@NotNull LibraryOrderEntry libraryOrderEntry, Set<String> value) {
                if (libraryOrderEntry.getLibraryName().startsWith("Maven:") || libraryOrderEntry.getLibraryName().startsWith("Gradle:")) {
                    for(VirtualFile file : libraryOrderEntry.getFiles(OrderRootType.CLASSES)) {
                        value.add(file.getPath());
                    }
                }
                return value;
            }
        }, new HashSet<>());
        return files.isEmpty()?null:files.hashCode();
    }

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
                return value | libraryOrderEntry.getLibraryName().contains("io.quarkus:quarkus-core:");
            }
        }, false);
    }

    public static Set<String> getModulesURIs(Project project) {
        Set<String> uris = new HashSet<>();
        for(Module module : ModuleManager.getInstance(project).getModules()) {
            uris.add(PsiUtilsImpl.getProjectURI(module));
        }
        return uris;
    }
}
