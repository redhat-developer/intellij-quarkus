/*******************************************************************************
 * Copyright (c) 2019-2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.maven;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.tool.ToolDelegate;
import org.jetbrains.idea.maven.project.MavenConsole;
import org.jetbrains.idea.maven.project.MavenEmbeddersManager;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;
import org.jetbrains.idea.maven.utils.MavenProgressIndicator;

import java.util.List;
import java.util.stream.Collectors;

import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.QUARKUS_DEPLOYMENT_LIBRARY_NAME;
import static com.redhat.devtools.intellij.quarkus.tool.ToolDelegate.BINARY;
import static com.redhat.devtools.intellij.quarkus.tool.ToolDelegate.SOURCES;

public class QuarkusMavenProjectsProcessTask implements MavenProjectsProcessorTask {
    private final Module module;

    public QuarkusMavenProjectsProcessTask(Module module) {
        this.module = module;
    }

    @Override
    public void perform(Project project, MavenEmbeddersManager embeddersManager, MavenConsole console, MavenProgressIndicator indicator) throws MavenProcessCanceledException {
        ToolDelegate toolDelegate = ToolDelegate.getDelegate(module);
        if (toolDelegate != null) {
            List<VirtualFile>[] deploymentFiles = toolDelegate.getDeploymentFiles(module);
            if (!deploymentFiles[BINARY].isEmpty()) {
                ModuleRootModificationUtil.addModuleLibrary(module, QUARKUS_DEPLOYMENT_LIBRARY_NAME, deploymentFiles[BINARY].stream().map(file -> file.getUrl()).collect(Collectors.toList()), deploymentFiles[SOURCES].stream().map(file -> file.getUrl()).collect(Collectors.toList()), DependencyScope.PROVIDED);
            }
        }
    }
}
