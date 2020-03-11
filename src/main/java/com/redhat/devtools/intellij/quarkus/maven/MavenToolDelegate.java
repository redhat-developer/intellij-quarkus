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
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.tool.ToolDelegate;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactInfo;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.server.MavenEmbedderWrapper;
import org.jetbrains.idea.maven.server.MavenServerManager;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;
import org.jetbrains.idea.maven.utils.MavenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MavenToolDelegate implements ToolDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(MavenToolDelegate.class);

    @Override
    public boolean isValid(Module module) {
        return MavenUtil.isMavenModule(module);
    }

    @Override
    public List<VirtualFile>[] getDeploymentFiles(Module module) {
        MavenProject mavenProject = MavenProjectsManager.getInstance(module.getProject()).findProject(module);
        List<VirtualFile>[] result = ToolDelegate.initDeploymentFiles();
        if (mavenProject != null) {
            getDeploymentFiles(module, mavenProject, result);
        }
        return result;
    }


    @Override
    public String getDisplay() {
        return "Maven";
    }

    @Override
    public void processImport(Module module) {
        Project project = module.getProject();
        VirtualFile pomFile = null;
        VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
        for(VirtualFile contentRoot : contentRoots) {
            VirtualFile child = contentRoot.findChild("pom.xml");
            if (child != null) {
                pomFile = child;
                break;
            }
        }

        if (pomFile != null) {
            MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(project);
            mavenProjectsManager.addManagedFiles(Collections.singletonList(pomFile));
        }
    }

    private void getDeploymentFiles(Module module, MavenProject mavenProject, List<VirtualFile>[] result) {
        Set<MavenArtifact> downloaded = new HashSet<>();
        for(MavenArtifact artifact : mavenProject.getDependencies()) {
            if (artifact.getFile() != null) {
                String deploymentIdStr = ToolDelegate.getDeploymentJarId(artifact.getFile());
                if (deploymentIdStr != null) {
                    MavenId deploymentId = new MavenId(deploymentIdStr);
                    if (mavenProject.findDependencies(deploymentId).isEmpty()) {
                            List<MavenArtifact> binaryDependencies = ensureDownloaded(module, mavenProject, deploymentId, null);
                            for(MavenArtifact binaryDependency : binaryDependencies) {
                                if (processDependency(mavenProject, result, downloaded, binaryDependency, BINARY)) {
                                    List<MavenArtifact> sourcesDependencies = ensureDownloaded(module, mavenProject, binaryDependency.getMavenId(), "sources");
                                    for(MavenArtifact sourceDependency : sourcesDependencies) {
                                        processDependency(mavenProject, result, downloaded, sourceDependency, SOURCES);
                                    }
                                }
                            }
                    }
                }
            }
        }
    }

    private boolean processDependency(MavenProject mavenProject, List<VirtualFile>[] result, Set<MavenArtifact> downloaded, MavenArtifact dependency, int type) {
        boolean added = false;

        if (mavenProject.findDependencies(dependency.getMavenId()).isEmpty() && !downloaded.contains(dependency)) {
            downloaded.add(dependency);
            VirtualFile jarRoot = getJarFile(dependency.getFile());
            if (jarRoot != null) {
                result[type].add(jarRoot);
                added = true;
            }
        }
        return added;
    }

    private List<MavenArtifact> ensureDownloaded(Module module, MavenProject mavenProject, MavenId deploymentId, String classifier) {
        try {
            MavenEmbedderWrapper serverWrapper = MavenServerManager.getInstance().createEmbedder(module.getProject(), false, null, null);
            MavenArtifactInfo info = new MavenArtifactInfo(deploymentId, "jar", classifier);
            if (classifier != null) {
                return Collections.singletonList(serverWrapper.resolve(info, mavenProject.getRemoteRepositories()));
            } else {
                return serverWrapper.resolveTransitively(Collections.singletonList(info), mavenProject.getRemoteRepositories());
            }
        } catch (MavenProcessCanceledException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
            return Collections.emptyList();
        }
    }
}
