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
package com.redhat.devtools.intellij.quarkus.gradle;

import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectImportProvider;
import com.intellij.util.io.HttpRequests;
import com.redhat.devtools.intellij.quarkus.tool.ToolDelegate;
import org.apache.commons.io.IOUtils;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportBuilder;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportProvider;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class GradleToolDelegate implements ToolDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(GradleToolDelegate.class);
    private static final String M2_REPO = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";
    private static final String CENTRAL_URL = "https://repo.maven.apache.org/maven2/";
    private static String GRADLE_LIBRARY_PREFIX = "Gradle: ";

    @Override
    public boolean isValid(Module module) {
        return ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module);
    }

    @Override
    public List<VirtualFile> getDeploymentFiles(Module module) {
        List<VirtualFile> result = new ArrayList<>();
        ModuleRootManager manager = ModuleRootManager.getInstance(module);
        manager.orderEntries().forEachLibrary(library -> {
            processLibrary(library, manager, result);
            return true;
        });
        return result;
    }

    private void processLibrary(Library library, ModuleRootManager manager, List<VirtualFile> result) {
        VirtualFile[] files = library.getFiles(OrderRootType.CLASSES);
        if (files.length == 1) {
            File file = new File(files[0].getCanonicalPath().substring(0, files[0].getCanonicalPath().length() - 2));
            String deploymentIdStr = ToolDelegate.getDeploymentJarId(file);
            if (deploymentIdStr != null && !isDependency(manager, deploymentIdStr)) {
                try {
                    File deploymentFile = getDeploymentFile(deploymentIdStr);
                    if (deploymentFile != null) {
                        if (!deploymentFile.exists()) {
                            processDownload(deploymentFile, deploymentIdStr);
                        }
                        VirtualFile f = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(deploymentFile);
                        if (f != null) {
                            VirtualFile jarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(f);
                            result.add(jarRoot);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error(e.getLocalizedMessage(), e);
                }
            }
        }
    }

    /**
     * Compute local path for the deployment file. As it is not that easy to use Gradle to compute it compared to the
     * Maven tooling, we make an assumption of using the standard M2 repository path.
     *
     * @param artifactId the deployment jar id in the form of group:artifact:version
     * @return the File object
     */
    public static File getDeploymentFile(String artifactId) {
        String path = toPath(artifactId);
        if (path != null) {
            return new File(M2_REPO, path);
        }
        return null;
    }

    private void processDownload(File file, String artifactId) throws IOException {
        String url = CENTRAL_URL + toPath(artifactId);
        try (OutputStream stream = new FileOutputStream(file)) {
            HttpRequests.request(url).connect(request -> {
                IOUtils.copy(request.getInputStream(), stream);
                return true;
            });
        }
    }

    private static String toPath(String artifactId) {
        String[] comps = artifactId.split(":");
        if (comps.length == 3) {
            StringBuffer buffer = new StringBuffer(comps[0].replace('.', File.separatorChar));
            buffer.append(File.separatorChar).append(comps[1]).append(File.separatorChar).append(comps[2]).append(File.separatorChar).append(comps[1]).append('-').append(comps[2]).append(".jar");
            return buffer.toString();
        }
        return null;
    }

    private boolean isDependency(ModuleRootManager manager, String deploymentIdStr) {
        return Stream.of(manager.getOrderEntries()).filter(entry -> entry instanceof LibraryOrderEntry && ((LibraryOrderEntry)entry).getLibraryName().equals(GRADLE_LIBRARY_PREFIX + deploymentIdStr)).findFirst().isPresent();
    }

    @Override
    public String getDisplay() {
        return "Gradle";
    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public void processImport(Module module) {
        Project project = module.getProject();
        File gradleFile = null;

        for(VirtualFile virtualFile : ModuleRootManager.getInstance(module).getContentRoots()) {
            File baseDir = VfsUtilCore.virtualToIoFile(virtualFile);
            File file = new File(baseDir, "build.gradle");
            if (file.exists()) {
                gradleFile = file;
                break;
            }
        }

        if (gradleFile != null) {
            ProjectDataManager projectDataManager = (ProjectDataManager) ServiceManager.getService(ProjectDataManager.class);
            GradleProjectImportBuilder gradleProjectImportBuilder = new GradleProjectImportBuilder(projectDataManager);
            GradleProjectImportProvider gradleProjectImportProvider = new GradleProjectImportProvider(gradleProjectImportBuilder);
            AddModuleWizard wizard = new AddModuleWizard(project, gradleFile.getPath(), new ProjectImportProvider[]{gradleProjectImportProvider});
            if (wizard.getStepCount() == 0 || wizard.showAndGet()) {
                gradleProjectImportBuilder.commit(project, (ModifiableModuleModel)null, (ModulesProvider)null);
            }
        }
    }
}
