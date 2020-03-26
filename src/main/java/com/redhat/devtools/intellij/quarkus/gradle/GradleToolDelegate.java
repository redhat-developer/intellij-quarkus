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
package com.redhat.devtools.intellij.quarkus.gradle;

import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.intellij.openapi.externalSystem.service.ExternalSystemFacadeManager;
import com.intellij.openapi.externalSystem.service.RemoteExternalSystemFacade;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.remote.RemoteExternalSystemTaskManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectImportBuilder;
import com.intellij.projectImport.ProjectImportProvider;
import com.redhat.devtools.intellij.quarkus.tool.ToolDelegate;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class GradleToolDelegate implements ToolDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(GradleToolDelegate.class);
    private static final String M2_REPO = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";

    private static final String QUARKUS_DOWNLOAD_TASK_DEFINITION =
            "task listQuarkusDependencies() {" + System.lineSeparator() +
                    "    File f = new File('%1$s')" + System.lineSeparator() +
                    "    f.withPrintWriter('UTF8') { writer ->" + System.lineSeparator() +
                    "        configurations.quarkusDeployment.files.each { writer.println it }" + System.lineSeparator() +
                    "        def componentIds = configurations.quarkusDeployment.incoming.resolutionResult.allDependencies.collect { it.selected.id }" + System.lineSeparator() +
                    "        ArtifactResolutionResult result = dependencies.createArtifactResolutionQuery()" + System.lineSeparator() +
                    "            .forComponents(componentIds)" + System.lineSeparator() +
                    "            .withArtifacts(JvmLibrary, SourcesArtifact)" + System.lineSeparator() +
                    "            .execute()" + System.lineSeparator() +
                    "        def sourceArtifacts = []" + System.lineSeparator() +
                    "        result.resolvedComponents.each { ComponentArtifactsResult component ->" + System.lineSeparator() +
                    "            Set<ArtifactResult> sources = component.getArtifacts(SourcesArtifact)" + System.lineSeparator() +
                    "            sources.each { ArtifactResult ar ->" + System.lineSeparator() +
                    "                if (ar instanceof ResolvedArtifactResult) {" + System.lineSeparator() +
                    "                    writer.println ar.file" + System.lineSeparator() +
                    "                }" + System.lineSeparator() +
                    "            }" + System.lineSeparator() +
                    "        }" + System.lineSeparator() +
                    "    }" + System.lineSeparator() +
                    "}";
    
    private static String GRADLE_LIBRARY_PREFIX = "Gradle: ";

    @Override
    public boolean isValid(Module module) {
        return ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module);
    }

    @Override
    public List<VirtualFile>[] getDeploymentFiles(Module module) {
        List<VirtualFile>[] result = ToolDelegate.initDeploymentFiles();
        ModuleRootManager manager = ModuleRootManager.getInstance(module);
        Set<String> deploymentIds = new HashSet<>();
        try {
            manager.orderEntries().forEachLibrary(library -> {
                processLibrary(library, manager, deploymentIds);
                return true;
            });
            if (!deploymentIds.isEmpty()) {
                processDownload(module, deploymentIds, result);
            }
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return result;
    }

    /**
     * Collect all deployment JARs and dependencies including the sources JARs. Will run a specific Gradle task.
     *
     * @param module the module
     * @param deploymentIds the Maven coordinates of the module deployment JARs
     * @param result the list where to place results to
     * @throws IOException if an error occurs running Gradle
     */
    private void processDownload(Module module, Set<String> deploymentIds, List<VirtualFile>[] result) throws IOException {
        Path outputPath = Files.createTempFile(null, ".txt");
        Path customPath = generateCustomGradleBuild(ModuleUtilCore.getModuleDirPath(module), outputPath, deploymentIds);
        collectDependencies(module, customPath, outputPath, result);
        Files.delete(outputPath);
        Files.delete(customPath);
    }

    /**
     * Collect all deployment JARs and dependencies through a Gradle specific task.
     *
     * @param module the module to analyze
     * @param customPath the custom Gradle build file with the specific task
     * @param outputPath the file where the result of the specific task is stored
     * @param result the list where to place results to
     * @throws IOException if an error occurs running Gradle
     */
    private void collectDependencies(Module module, Path customPath, Path outputPath, List<VirtualFile>[] result) throws IOException {
        try {
            final ExternalSystemFacadeManager manager = ServiceManager.getService(ExternalSystemFacadeManager.class);

            ExternalSystemExecutionSettings settings = ExternalSystemApiUtil.getExecutionSettings(module.getProject(),
                    module.getProject().getBasePath(),
                    GradleConstants.SYSTEM_ID);

            RemoteExternalSystemFacade facade = manager.getFacade(module.getProject(), ModuleUtilCore.getModuleDirPath(module), GradleConstants.SYSTEM_ID);

            RemoteExternalSystemTaskManager taskManager = facade.getTaskManager();
            final List<String> arguments = Arrays.asList("-b", customPath.toString(), "-q", "--console", "plain");
            settings
                    .withArguments(arguments);
            ExternalSystemTaskId taskId = ExternalSystemTaskId.create(GradleConstants.SYSTEM_ID, ExternalSystemTaskType.EXECUTE_TASK, module.getProject());
            taskManager.executeTasks(taskId, Arrays.asList("listQuarkusDependencies"), ModuleUtilCore.getModuleDirPath(module), settings, null);
            try (BufferedReader reader = Files.newBufferedReader(outputPath)) {
                reader.lines().forEach(line -> {
                    VirtualFile jarFile = getJarFile(line);
                    if (jarFile != null) {
                        result[line.endsWith("sources.jar")?SOURCES:BINARY].add(jarFile);
                    }
                });
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Generate the custom build file from the module build file and adding the specific task.
     *
     * @param basePath the base path where the module build file is in
     * @param outputPath the path to the task result file
     * @param deploymentIds the Maven coordinates of the module deployment JARs
     * @return the path to the custom build file
     * @throws IOException if an error occurs generating the file
     */
    private Path generateCustomGradleBuild(String basePath, Path outputPath, Set<String> deploymentIds) throws IOException {
        Path base = Paths.get(basePath);
        Path path = base.resolve("build.gradle");
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String content = IOUtils.toString(reader);
            content = appendQuarkusResolution(content, outputPath, deploymentIds);
            Path customPath = Files.createTempFile(base, null, ".gradle");
            try (Writer writer = Files.newBufferedWriter(customPath, StandardCharsets.UTF_8)) {
                IOUtils.write(content, writer);
                return customPath;
            }
        }
    }

    /**
     * Append the specific task definition.
     *
     * @param content the content of the module build file
     * @param outputPath the path to the task result file
     * @param deploymentIds the Maven coordinates of the module deployment JARs
     * @return the content of the custom build file
     */
    private String appendQuarkusResolution(String content, Path outputPath, Set<String> deploymentIds) {
        StringBuffer buffer = new StringBuffer(content);
        buffer.append("configurations {quarkusDeployment}").append(System.lineSeparator());
        buffer.append("dependencies {").append(System.lineSeparator());
        deploymentIds.forEach(id -> buffer.append("quarkusDeployment '").append(id).append('\'').append(System.lineSeparator()));
        buffer.append('}').append(System.lineSeparator());
        buffer.append(String.format(QUARKUS_DOWNLOAD_TASK_DEFINITION, outputPath.toString().replace("\\", "\\\\")));
        return buffer.toString();
    }

    private void processLibrary(Library library, ModuleRootManager manager, Set<String> deploymentIds) {
        VirtualFile[] files = library.getFiles(OrderRootType.CLASSES);
        if (files.length == 1) {
            File file = new File(files[0].getCanonicalPath().substring(0, files[0].getCanonicalPath().length() - 2));
            String deploymentIdStr = ToolDelegate.getDeploymentJarId(file);
            if (deploymentIdStr != null && !isDependency(manager, deploymentIdStr)) {
                deploymentIds.add(deploymentIdStr);
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
        String path = toPath(artifactId, File.separatorChar);
        if (path != null) {
            return new File(M2_REPO, path);
        }
        return null;
    }

    private static String toPath(String artifactId, char separator) {
        String[] comps = artifactId.split(":");
        if (comps.length == 3) {
            StringBuffer buffer = new StringBuffer(comps[0].replace('.', separator));
            buffer.append(separator).append(comps[1]).append(separator).append(comps[2]).append(separator).append(comps[1]).append('-').append(comps[2]).append(".jar");
            return buffer.toString();
        }
        return null;
    }

    private boolean isDependency(ModuleRootManager manager, String deploymentIdStr) {
        return Stream.of(manager.getOrderEntries()).filter(entry -> entry instanceof LibraryOrderEntry && (GRADLE_LIBRARY_PREFIX + deploymentIdStr).equals(((LibraryOrderEntry)entry).getLibraryName())).findFirst().isPresent();
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
            ProjectImportProvider gradleProjectImportProvider = getGradleProjectImportProvider();
            ProjectImportBuilder gradleProjectImportBuilder = gradleProjectImportProvider.getBuilder();
            AddModuleWizard wizard = new AddModuleWizard(project, gradleFile.getPath(), new ProjectImportProvider[]{gradleProjectImportProvider});
            if (wizard.getStepCount() == 0 || wizard.showAndGet()) {
                gradleProjectImportBuilder.commit(project, (ModifiableModuleModel)null, (ModulesProvider)null);
            }
        }
    }

    private static final String LEGACY_IMPORT_PROVIDER_CLASS_NAME = "org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportProvider";
    private static final String LEGACY_IMPORT_BUILDER_CLASS_NAME = "org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportBuilder";

    private static final String NEW_IMPORT_PROVIDER_CLASS_NAME = "org.jetbrains.plugins.gradle.service.project.wizard.JavaGradleProjectImportProvider";
    @NotNull
    private ProjectImportProvider getGradleProjectImportProvider() {
        try {
            return (ProjectImportProvider) Class.forName(NEW_IMPORT_PROVIDER_CLASS_NAME).newInstance();
        } catch (ClassNotFoundException e) {
            try {
                Class<ProjectImportBuilder> clazz = (Class<ProjectImportBuilder>) Class.forName(LEGACY_IMPORT_BUILDER_CLASS_NAME);
                ProjectImportBuilder builder = clazz.getConstructor(ProjectDataManager.class).newInstance(ProjectDataManager.getInstance());
                return (ProjectImportProvider) Class.forName(LEGACY_IMPORT_PROVIDER_CLASS_NAME).getConstructor(clazz).newInstance(builder);
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException e1) {
                throw new RuntimeException(e1);
            }
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}
