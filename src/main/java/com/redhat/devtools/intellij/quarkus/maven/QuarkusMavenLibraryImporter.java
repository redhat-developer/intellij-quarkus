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
package com.redhat.devtools.intellij.quarkus.maven;

import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.module.Module;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.idea.maven.importing.MavenImporter;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class QuarkusMavenLibraryImporter extends MavenImporter {
    private static final List<String> SUPPORTED_PACKAGINGS = ContainerUtil.immutableList(new String[]{"jar", "war"});

    public QuarkusMavenLibraryImporter() {
        super("io.quarkus", "quarkus-maven-plugin");
    }

    @Override
    public void getSupportedPackagings(Collection<String> result) {
        result.addAll(SUPPORTED_PACKAGINGS);
    }

    @Override
    public void preProcess(Module module, MavenProject mavenProject, MavenProjectChanges changes, IdeModifiableModelsProvider modifiableModelsProvider) {
    }

    @Override
    public void process(IdeModifiableModelsProvider modifiableModelsProvider, Module module, MavenRootModelAdapter rootModel, MavenProjectsTree mavenModel, MavenProject mavenProject, MavenProjectChanges changes, Map<MavenProject, String> mavenProjectToModuleName, List<MavenProjectsProcessorTask> postTasks) {
        postTasks.add(new QuarkusMavenProjectsProcessTask(module));
    }
}
