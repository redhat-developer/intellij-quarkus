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
package com.redhat.devtools.intellij.quarkus.maven;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.tool.ToolDelegate;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactInfo;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.server.MavenEmbedderWrapper;
import org.jetbrains.idea.maven.server.MavenServerManager;
import org.jetbrains.idea.maven.utils.MavenArtifactUtil;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;
import org.jetbrains.idea.maven.utils.MavenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenToolDelegate implements ToolDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(MavenToolDelegate.class);

    @Override
    public boolean isValid(Module module) {
        return MavenUtil.isMavenModule(module);
    }

    @Override
    public List<VirtualFile> getDeploymentFiles(Module module) {
        MavenProject mavenProject = MavenProjectsManager.getInstance(module.getProject()).findProject(module);
        List<VirtualFile> result = new ArrayList<>();
        getDeploymentFiles(module, mavenProject, result);
        return result;
    }

    private void getDeploymentFiles(Module module, MavenProject mavenProject, List<VirtualFile> result) {
        for(MavenArtifact artifact : mavenProject.getDependencies()) {
            if (artifact.getFile() != null) {
                String deploymentIdStr = ToolDelegate.getDeploymentJarId(artifact.getFile());
                if (deploymentIdStr != null) {
                    MavenId deploymentId = new MavenId(deploymentIdStr);
                    if (mavenProject.findDependencies(deploymentId).isEmpty()) {
                        File artifactFile = MavenArtifactUtil.getArtifactFile(mavenProject.getLocalRepository(), deploymentId,"jar");
                        if (!artifactFile.exists()) {
                            processDownload(module, mavenProject, deploymentId);
                        }
                        VirtualFile f = LocalFileSystem.getInstance().findFileByIoFile(artifactFile);
                        VirtualFile jarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(f);
                        result.add(jarRoot);
                    }
                }
            }
        }
    }

    private void processDownload(Module module, MavenProject mavenProject, MavenId deploymentId) {
        try {
            MavenEmbedderWrapper serverWrapper = MavenServerManager.getInstance().createEmbedder(module.getProject(), false, null, null);
            MavenArtifactInfo info = new MavenArtifactInfo(deploymentId, "jar", null);
            serverWrapper.resolve(info, mavenProject.getRemoteRepositories());
        } catch (MavenProcessCanceledException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
    }

}
