/*******************************************************************************
 * Copyright (c) 2019-2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.buildtool.maven;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.NonUrgentExecutor;
import com.intellij.util.messages.MessageBusConnection;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.quarkus.buildtool.BuildToolDelegate;
import com.redhat.devtools.intellij.quarkus.buildtool.ProjectImportListener;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.execution.MavenRunConfiguration;
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType;
import org.jetbrains.idea.maven.execution.MavenRunnerSettings;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactInfo;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenImportListener;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.server.MavenEmbedderWrapper;
import org.jetbrains.idea.maven.server.MavenServerManager;
import org.jetbrains.idea.maven.utils.MavenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MavenToolDelegate implements BuildToolDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(MavenToolDelegate.class);

    @Override
    public boolean isValid(Module module) {
        return MavenUtil.isMavenModule(module);
    }

    @Override
    public List<VirtualFile>[] getDeploymentFiles(Module module, ProgressIndicator progressIndicator) {
        MavenProject mavenProject = MavenProjectsManager.getInstance(module.getProject()).findProject(module);
        List<VirtualFile>[] result = BuildToolDelegate.initDeploymentFiles();
        if (mavenProject != null) {
            getDeploymentFiles(module, mavenProject, result, progressIndicator);
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
        VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
        Arrays.stream(contentRoots).map(contentRoot -> contentRoot.findChild("pom.xml")).filter(Objects::nonNull).findFirst().ifPresent(pomFile -> MavenUtil.runWhenInitialized(project, (DumbAwareRunnable) () -> {
            MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(project);
            mavenProjectsManager.addManagedFiles(Collections.singletonList(pomFile));
        }));

    }

    private void getDeploymentFiles(Module module, MavenProject mavenProject, List<VirtualFile>[] result, ProgressIndicator progressIndicator) {
        Set<MavenArtifact> downloaded = new HashSet<>();
        Set<MavenId> toDownload = new HashSet<>();

        List<MavenArtifact> dependencies = mavenProject.getDependencies();
        double counter = 80d / 100d / 3d;
        double i = counter / dependencies.size();
        double p = 0;

        // Step1: searching deployment JAR
        for (MavenArtifact artifact : dependencies) {
            progressIndicator.checkCanceled();
            progressIndicator.setText2("Searching deployment descriptor in '" + artifact.getArtifactId() + "' in Maven project dependencies.");
            if (artifact.getFile() != null) {
                String deploymentIdStr = BuildToolDelegate.getDeploymentJarId(artifact.getFile());
                if (deploymentIdStr != null) {
                    MavenId deploymentId = new MavenId(deploymentIdStr);
                    if (mavenProject.findDependencies(deploymentId).isEmpty()) {
                        toDownload.add(deploymentId);
                    }
                }
            }
            p += i;
            progressIndicator.setFraction(p);
        }

        // Step2
        progressIndicator.checkCanceled();
        progressIndicator.setText2("Collecting Quarkus deployment dependencies from '" + toDownload.size() + "' binary dependencies");
        Set<MavenArtifact> binaryDependencies = resolveDeploymentArtifacts(module, mavenProject, toDownload, null, progressIndicator);
        i = counter / binaryDependencies.size();
        toDownload.clear();
        for (MavenArtifact binaryDependency : binaryDependencies) {
            progressIndicator.checkCanceled();
            progressIndicator.setText2("Searching deployment descriptor in '" + binaryDependency.getArtifactId() + "' binary");
            if (!"test".equals(binaryDependency.getScope())) {
                if (processDependency(mavenProject, result, downloaded, binaryDependency, BINARY)) {
                    toDownload.add(binaryDependency.getMavenId());
                }
            }
            p += i;
            progressIndicator.setFraction(p);
        }

        // Step3
        progressIndicator.checkCanceled();
        progressIndicator.setText2("Collecting Quarkus deployment dependencies from '" + toDownload.size() + "' source dependencies");
        Set<MavenArtifact> sourcesDependencies = resolveDeploymentArtifacts(module, mavenProject, toDownload, "sources", progressIndicator);
        i = counter / sourcesDependencies.size();
        for (MavenArtifact sourceDependency : sourcesDependencies) {
            progressIndicator.checkCanceled();
            progressIndicator.setText2("Searching deployment descriptor in '" + sourceDependency.getArtifactId() + "' sources");
            processDependency(mavenProject, result, downloaded, sourceDependency, SOURCES);
            p += i;
            progressIndicator.setFraction(p);
        }
    }

    private boolean processDependency(MavenProject mavenProject, List<VirtualFile>[] result, Set<MavenArtifact> downloaded, MavenArtifact dependency, int type) {
        boolean added = false;
        if (mavenProject.findDependencies(dependency.getGroupId(), dependency.getArtifactId()).isEmpty() && !downloaded.contains(dependency)) {
            downloaded.add(dependency);
            VirtualFile jarRoot = getJarFile(dependency.getFile());
            if (jarRoot != null) {
                result[type].add(jarRoot);
                added = true;
            }
        }
        return added;
    }

    private Set<MavenArtifact> resolveDeploymentArtifacts(Module module, MavenProject mavenProject, Set<MavenId> deploymentIds, String classifier, ProgressIndicator progressIndicator) {
        Set<MavenArtifact> deploymentArtifacts = new HashSet<>();
        try {
            MavenEmbedderWrapper serverWrapper = MavenServerManager.getInstance().createEmbedder(module.getProject(), true, mavenProject.getDirectory());
            if (classifier != null) {
                for (MavenId id : deploymentIds) {
                    deploymentArtifacts.add(serverWrapper.resolve(new MavenArtifactInfo(id, "jar", classifier), mavenProject.getRemoteRepositories()));
                }
            } else {
                for (var deploymentId : deploymentIds) {
                    boolean shouldResolveArtifactTransitively = BuildToolDelegate.shouldResolveArtifactTransitively(deploymentId);
                    progressIndicator.checkCanceled();
                    progressIndicator.setText2("Resolving " + (shouldResolveArtifactTransitively ? " (Transitively) " : "") + "'" + deploymentId + "'");
                    if (shouldResolveArtifactTransitively) {
                        // Resolving the deployment artifact and their dependencies
                        List<MavenArtifactInfo> infos = List.of(new MavenArtifactInfo(deploymentId, "jar", classifier));
                        List<MavenArtifact> resolvedArtifacts = serverWrapper.resolveArtifactTransitively(infos, mavenProject.getRemoteRepositories()).mavenResolvedArtifacts;
                        for (var resolvedArtifact : resolvedArtifacts) {
                            addDeploymentArtifact(resolvedArtifact, deploymentArtifacts);
                        }
                    } else {
                        // Resolving only the deployment artifact
                        MavenArtifact resolvedArtifact = serverWrapper.resolve(new MavenArtifactInfo(deploymentId, "jar", classifier), mavenProject.getRemoteRepositories()); //.mavenResolvedArtifacts;
                        addDeploymentArtifact(resolvedArtifact, deploymentArtifacts);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }
        return deploymentArtifacts;
    }

    private static void addDeploymentArtifact(MavenArtifact resolvedArtifact, Set<MavenArtifact> result) {
        if (resolvedArtifact != null && !result.contains(resolvedArtifact)) {
            result.add(resolvedArtifact);
        }
    }

    @Override
    public RunnerAndConfigurationSettings getConfigurationDelegate(Module module, QuarkusRunConfiguration configuration, @Nullable Integer debugPort) {
        RunnerAndConfigurationSettings settings = RunManager.getInstance(module.getProject()).createConfiguration(module.getName() + " Quarkus (Maven)", MavenRunConfigurationType.class);
        MavenRunConfiguration mavenConfiguration = (MavenRunConfiguration) settings.getConfiguration();
        mavenConfiguration.getRunnerParameters().setResolveToWorkspace(true);
        mavenConfiguration.getRunnerParameters().setGoals(Collections.singletonList("quarkus:dev"));
        mavenConfiguration.getRunnerParameters().setWorkingDirPath(VfsUtilCore.virtualToIoFile(QuarkusModuleUtil.getModuleDirPath(module)).getAbsolutePath());
        ensureRunnerSettings(mavenConfiguration);
        mavenConfiguration.getRunnerSettings().setEnvironmentProperties(configuration.getEnv());
        if (StringUtils.isNotBlank(configuration.getProfile())) {
            mavenConfiguration.getRunnerSettings().getMavenProperties().put("quarkus.profile", configuration.getProfile());
        }
        if (debugPort != null) {
            mavenConfiguration.getRunnerSettings().getMavenProperties().put("debug", Integer.toString(debugPort));
        }
        mavenConfiguration.setBeforeRunTasks(configuration.getBeforeRunTasks());
        return settings;
    }

    @Override
    public void addProjectImportListener(@NotNull Project project, @NotNull MessageBusConnection connection, @NotNull ProjectImportListener listener) {
        connection.subscribe(MavenImportListener.TOPIC, new MavenImportListener() {
            @Override
            public void importFinished(@NotNull Collection<MavenProject> importedProjects, @NotNull List<Module> newModules) {
                ReadAction.nonBlocking(() -> {
                            List<Module> modules = new ArrayList<>();
                            for (MavenProject mavenProject : importedProjects) {
                                MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(project);
                                Module module = projectsManager.findModule(mavenProject);
                                if (module != null) {
                                    modules.add(module);
                                }
                            }
                            listener.importFinished(modules);
                        })
                        .inSmartMode(project)
                        .submit(NonUrgentExecutor.getInstance());
            }
        });

    }

    private void ensureRunnerSettings(MavenRunConfiguration mavenConfiguration) {
        if (mavenConfiguration.getRunnerSettings() == null) {
            mavenConfiguration.setRunnerSettings(new MavenRunnerSettings());
        }
    }
}
