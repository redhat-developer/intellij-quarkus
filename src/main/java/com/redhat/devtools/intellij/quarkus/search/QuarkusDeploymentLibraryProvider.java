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
package com.redhat.devtools.intellij.quarkus.search;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider;
import com.intellij.openapi.roots.SyntheticLibrary;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.tool.ToolDelegate;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class QuarkusDeploymentLibraryProvider extends AdditionalLibraryRootsProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusDeploymentLibraryProvider.class);

    @NotNull
    @Override
    public Collection<SyntheticLibrary> getAdditionalProjectLibraries(@NotNull Project project) {
        long start = System.currentTimeMillis();
        List<List<VirtualFile>> files = new ArrayList<>();

        for(Module module : ModuleManager.getInstance(project).getModules()) {
            ToolDelegate toolDelegate = ToolDelegate.getDelegate(module);
            if (toolDelegate != null) {
                List<VirtualFile> moduleFiles = toolDelegate.getDeploymentFiles(module);
                if (!moduleFiles.isEmpty()) {
                    files.add(moduleFiles);
                }
            }
        }
        Set<VirtualFile> deploymentFiles = files.stream().flatMap(list -> list.stream()).collect(Collectors.toSet());
        LOGGER.info("Created Quarkus deployment synthetic library duration=" + (System.currentTimeMillis() - start) + " size=" + deploymentFiles.size());
        deploymentFiles.forEach(file -> System.out.println("Added file to Quarkus lib:" + file));
        return Collections.singletonList(SyntheticLibrary.newImmutableLibrary(Collections.emptyList(), deploymentFiles, Collections.emptySet(), null));
    }
}
