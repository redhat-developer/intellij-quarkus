/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.gradle;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
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
import com.intellij.openapi.module.ModuleGrouper;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
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
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfiguration;
import com.redhat.devtools.intellij.quarkus.tool.ToolDelegate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType;
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration;
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

public abstract class AbstractGradleToolDelegate implements ToolDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGradleToolDelegate.class);
    private static final String M2_REPO = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";

    private static final String GRADLE_LIBRARY_PREFIX = "Gradle: ";

    private boolean scriptExists(Module module) {
        String path = getModuleDirPath(module);
        if (path != null) {
            File script = new File(path, getScriptName());
            return script.exists();
        }
        return false;
    }

    @Override
    public boolean isValid(Module module) {
        return ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module) && scriptExists(module);
    }

    @Override
    public List<VirtualFile>[] getDeploymentFiles(Module module, ProgressIndicator progressIndicator) {
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
        Path customBuildFile = generateCustomGradleBuild(getModuleDirPath(module), outputPath, deploymentIds);
        Path customSettingsFile = generateCustomGradleSettings(getModuleDirPath(module), customBuildFile);
        try {
            collectDependencies(module, customBuildFile, customSettingsFile, outputPath, result);
        } catch (IOException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        } finally {
            Files.delete(outputPath);
            Files.delete(customBuildFile);
            Files.delete(customSettingsFile);
        }
   }


    private String getModuleDirPath(Module module) {
        VirtualFile dir = QuarkusModuleUtil.getModuleDirPath(module);
        VirtualFile script = dir!=null?dir.findChild(getScriptName()):null;
        if (script != null && script.exists()) {
            return dir.getPath();
        }
        ModuleGrouper grouper = ModuleGrouper.instanceFor(module.getProject());
        List<String> names = grouper.getGroupPath(module);
        if (!names.isEmpty()) {
            ModuleManager manager = ModuleManager.getInstance(module.getProject());
            Module parentModule = manager.findModuleByName(names.get(0));
            if (parentModule != null) {
                return getModuleDirPath(parentModule);
            }
        }
        return null;
    }

    /**
     * Collect all deployment JARs and dependencies through a Gradle specific task.
     *
     * @param module the module to analyze
     * @param customBuildFile the custom Gradle build file with the specific task
     * @param outputPath the file where the result of the specific task is stored
     * @param result the list where to place results to
     * @throws IOException if an error occurs running Gradle
     */
    private void collectDependencies(Module module, Path customBuildFile, Path customSettingsFile, Path outputPath, List<VirtualFile>[] result) throws IOException {
        try {
            final ExternalSystemFacadeManager manager = ServiceManager.getService(ExternalSystemFacadeManager.class);

            ExternalSystemExecutionSettings settings = ExternalSystemApiUtil.getExecutionSettings(module.getProject(),
                    getModuleDirPath(module),
                    GradleConstants.SYSTEM_ID);

            RemoteExternalSystemFacade facade = manager.getFacade(module.getProject(), getModuleDirPath(module), GradleConstants.SYSTEM_ID);

            RemoteExternalSystemTaskManager taskManager = facade.getTaskManager();
            final List<String> arguments = Arrays.asList("-c", customSettingsFile.toString(), "-q", "--console", "plain");
            settings
                    .withArguments(arguments);
            ExternalSystemTaskId taskId = ExternalSystemTaskId.create(GradleConstants.SYSTEM_ID, ExternalSystemTaskType.EXECUTE_TASK, module.getProject());
            taskManager.executeTasks(taskId, Arrays.asList("listQuarkusDependencies"), getModuleDirPath(module), settings, null);
            try (BufferedReader reader = Files.newBufferedReader(outputPath)) {
                String id;

                while ((id = reader.readLine()) != null) {
                    String file = reader.readLine();
                    String[] ids = id.split(":");
                    if (!isDependency(ModuleRootManager.getInstance(module), ids[0], ids[1])) {
                        if (file != null) {
                            VirtualFile jarFile = getJarFile(file);
                            if (jarFile != null) {
                                result[file.endsWith("sources.jar")?SOURCES:BINARY].add(jarFile);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    abstract String getScriptName();

    abstract String getSettingsScriptName();

    abstract String getScriptExtension();

    abstract String formatQuarkusDependency(String id);

    abstract String generateTask(String path);

    abstract String createQuarkusConfiguration();

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
        Path path = base.resolve(getScriptName());
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String content = IOUtils.toString(reader);
            content = appendQuarkusResolution(content, outputPath, deploymentIds);
            Path customPath = Files.createTempFile(base, null, getScriptExtension());
            try (Writer writer = Files.newBufferedWriter(customPath, StandardCharsets.UTF_8)) {
                IOUtils.write(content, writer);
                return customPath;
            }
        }
    }

    private Path generateCustomGradleSettings(String basePath, Path customBuildFile) throws IOException {
        Path base = Paths.get(basePath);
        Path path = base.resolve(getSettingsScriptName());
        String content = "";
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            content = IOUtils.toString(reader);
        } catch (IOException e) {}
        content += System.lineSeparator() + "rootProject.buildFileName =\"" + customBuildFile.getFileName().toFile().getName() + "\"";
        Path customPath = Files.createTempFile(base, null, getScriptExtension());
        try (Writer writer = Files.newBufferedWriter(customPath, StandardCharsets.UTF_8)) {
            IOUtils.write(content, writer);
            return customPath;
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
        buffer.append(System.lineSeparator());
        buffer.append(createQuarkusConfiguration()).append(System.lineSeparator());
        buffer.append("dependencies {").append(System.lineSeparator());
        deploymentIds.forEach(id -> buffer.append(formatQuarkusDependency(id)));
        buffer.append('}').append(System.lineSeparator());
        buffer.append(generateTask(outputPath.toString().replace("\\", "\\\\")));
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

    private boolean isDependency(ModuleRootManager manager, String groupId, String artifactId) {
        return Stream.of(manager.getOrderEntries()).filter(entry -> entry instanceof LibraryOrderEntry && ((LibraryOrderEntry) entry).getLibraryName().startsWith(GRADLE_LIBRARY_PREFIX + groupId + ':' + artifactId)).findFirst().isPresent();
    }

    @Override
    public void processImport(Module module) {
        Project project = module.getProject();
        File gradleFile = null;

        for(VirtualFile virtualFile : ModuleRootManager.getInstance(module).getContentRoots()) {
            File baseDir = VfsUtilCore.virtualToIoFile(virtualFile);
            File file = new File(baseDir, getScriptName());
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

    @Override
    public RunnerAndConfigurationSettings getConfigurationDelegate(Module module, QuarkusRunConfiguration configuration) {
        RunnerAndConfigurationSettings settings = RunManager.getInstance(module.getProject()).createConfiguration(module.getName() + " Quarkus (Gradle)", GradleExternalTaskConfigurationType.class);
        GradleRunConfiguration gradleConfiguration = (GradleRunConfiguration) settings.getConfiguration();
        gradleConfiguration.getSettings().getTaskNames().add("quarkusDev");
        gradleConfiguration.getSettings().setEnv(configuration.getEnv());
        String parameters = "-Ddebug=" + configuration.getPort();
        if (StringUtils.isNotBlank(configuration.getProfile())) {
            parameters += " -Dquarkus.profile=" + configuration.getProfile();
        }
        gradleConfiguration.getSettings().setScriptParameters(parameters);
        gradleConfiguration.setBeforeRunTasks(configuration.getBeforeRunTasks());
        return settings;
    }
}
