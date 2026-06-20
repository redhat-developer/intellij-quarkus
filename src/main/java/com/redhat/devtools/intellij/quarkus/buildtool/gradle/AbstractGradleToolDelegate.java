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
package com.redhat.devtools.intellij.quarkus.buildtool.gradle;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener;
import com.intellij.openapi.externalSystem.task.TaskCallback;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleGrouper;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.projectImport.ProjectImportBuilder;
import com.intellij.projectImport.ProjectImportProvider;
import com.intellij.util.concurrency.NonUrgentExecutor;
import com.intellij.util.messages.MessageBusConnection;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.quarkus.buildtool.BuildToolDelegate;
import com.redhat.devtools.intellij.quarkus.buildtool.ProjectImportListener;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.execution.GradleRunnerUtil;
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType;
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public abstract class AbstractGradleToolDelegate implements BuildToolDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGradleToolDelegate.class);
    private static final String M2_REPO = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";

    private static final String GRADLE_LIBRARY_PREFIX = "Gradle: ";

    /**
     * The Groovy init script that contributes the {@code quarkusDeployment} configuration, the deployment
     * dependencies and the {@code listQuarkusDependencies} task to the (root) project.
     * <p>
     * It is applied through {@code --init-script} and works for both Groovy and Kotlin DSL projects since it
     * only relies on the DSL agnostic Gradle project API. This replaces the legacy approach based on a custom
     * settings file (selected with the {@code -c} option) and {@code rootProject.buildFileName}, both removed
     * in Gradle 9.
     * <p>
     * The configuration/dependencies/task are contributed to the project whose directory matches the analyzed
     * module directory (not necessarily the root project), so the deployment artifacts are resolved with that
     * module's own repositories. This keeps multi-module projects working, where repositories may be declared
     * per subproject.
     * <ul>
     *     <li>{@code %1$s} is the list of deployment dependencies to resolve</li>
     *     <li>{@code %2$s} is the path of the file where the resolved artifacts are written</li>
     *     <li>{@code %3$s} is the directory of the analyzed module (used to select the target Gradle project)</li>
     * </ul>
     */
    private static final String INIT_SCRIPT_TEMPLATE = """
            import org.gradle.jvm.JvmLibrary
            import org.gradle.language.base.artifact.SourcesArtifact
            import org.gradle.api.artifacts.result.ResolvedArtifactResult

            allprojects { proj ->
                if (proj.projectDir.canonicalPath == new File('%3$s').canonicalPath) {
                    proj.afterEvaluate { project ->
                        project.configurations.create('quarkusDeployment')
            %1$s\
                        project.tasks.register('listQuarkusDependencies') {
                            doLast {
                                def quarkusDeployment = project.configurations.quarkusDeployment
                                new File('%2$s').withPrintWriter('UTF8') { writer ->
                                    quarkusDeployment.incoming.artifacts.each {
                                        writer.println it.id.componentIdentifier
                                        writer.println it.file
                                    }
                                    def componentIds = quarkusDeployment.incoming.resolutionResult.allDependencies.collect { it.selected.id }
                                    def result = project.dependencies.createArtifactResolutionQuery()
                                        .forComponents(componentIds)
                                        .withArtifacts(JvmLibrary, SourcesArtifact)
                                        .execute()
                                    result.resolvedComponents.each { component ->
                                        component.getArtifacts(SourcesArtifact).each { ar ->
                                            if (ar instanceof ResolvedArtifactResult) {
                                                writer.println ar.id.componentIdentifier
                                                writer.println ar.file
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            """;

    private boolean scriptExists(@NotNull Module module) {
        String path = getModuleDirPath(module);
        if (path != null) {
            File script = new File(path, getScriptName());
            return script.exists();
        }
        return false;
    }

    @Override
    public boolean isValid(@NotNull Module module) {
        return ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module) && scriptExists(module);
    }

    @Override
    public List<VirtualFile>[] getDeploymentFiles(@NotNull Module module,
                                                  @NotNull ProgressIndicator progressIndicator) {
        List<VirtualFile>[] result = BuildToolDelegate.initDeploymentFiles();
        ModuleRootManager manager = ModuleRootManager.getInstance(module);
        Set<String> deploymentIds = new HashSet<>();
        try {
            manager.orderEntries().forEachLibrary(library -> {
                progressIndicator.checkCanceled();
                processLibrary(library, manager, deploymentIds);
                return true;
            });
            if (!deploymentIds.isEmpty()) {
                progressIndicator.checkCanceled();
                processDownload(module, deploymentIds, result, progressIndicator);
            }
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return result;
    }

    /**
     * Collect all deployment JARs and dependencies including the sources JARs. Will run a specific Gradle task.
     *
     * @param module            the module
     * @param deploymentIds     the Maven coordinates of the module deployment JARs
     * @param result            the list where to place results to
     * @param progressIndicator the progress indicator used to react to cancellation while the task runs
     * @throws IOException if an error occurs running Gradle
     */
    private void processDownload(@NotNull Module module,
                                 @NotNull Set<String> deploymentIds,
                                 @NotNull List<VirtualFile>[] result,
                                 @NotNull ProgressIndicator progressIndicator) throws IOException {
        Path outputPath = Files.createTempFile(null, ".txt");
        Path initScript = generateInitScript(outputPath, deploymentIds, getModuleDirPath(module));
        TaskCallback callback = new TaskCallback() {
            @Override
            public void onSuccess() {
                cleanCustomFiles();
            }

            @Override
            public void onFailure() {
                LOGGER.error("Failed to run custom gradle build");
                cleanCustomFiles();
            }

            private void cleanCustomFiles() {
                try {
                    Files.delete(initScript);
                } catch (IOException e) {
                    LOGGER.warn(e.getLocalizedMessage(), e);
                }
            }

        };
        try {
            collectDependencies(module, initScript, outputPath, result, callback, progressIndicator);
        } catch (IOException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        } finally {
            Files.delete(outputPath);
        }
    }

    private @Nullable String getModuleDirPath(Module module) {
        return BuildToolDelegate.getModuleDirPath(module, getScriptName());
    }

    /**
     * Collect all deployment JARs and dependencies through a Gradle specific task.
     *
     * @param module            the module to analyze
     * @param initScript        the Gradle init script that contributes the {@code listQuarkusDependencies} task
     * @param outputPath        the file where the result of the specific task is stored
     * @param result            the list where to place results to
     * @param callback          the callback to call after running the task
     * @param progressIndicator the progress indicator used to react to cancellation while the task runs
     * @throws IOException if an error occurs running Gradle
     */
    private void collectDependencies(@NotNull Module module,
                                     @NotNull Path initScript,
                                     @NotNull Path outputPath,
                                     @NotNull List<VirtualFile>[] result,
                                     @NotNull TaskCallback callback,
                                     @NotNull ProgressIndicator progressIndicator) throws IOException {
        try {
            ExternalSystemTaskExecutionSettings executionSettings = new ExternalSystemTaskExecutionSettings();
            executionSettings.setExternalSystemIdString(GradleConstants.SYSTEM_ID.toString());
            executionSettings.setTaskNames(Collections.singletonList("listQuarkusDependencies"));
            executionSettings.setScriptParameters(String.format("--init-script \"%s\" -q --console plain", initScript.toString()));
            executionSettings.setExternalProjectPath(getModuleDirPath(module));

            // The task is run asynchronously, so wait for it to complete before reading the result file:
            // reading it eagerly would race with Gradle still writing it and would (silently) collect no
            // deployment dependencies. This runs on a background thread (the model is committed later), so
            // blocking here is safe.
            CountDownLatch latch = new CountDownLatch(1);
            TaskCallback waitingCallback = new TaskCallback() {
                @Override
                public void onSuccess() {
                    try {
                        callback.onSuccess();
                    } finally {
                        latch.countDown();
                    }
                }

                @Override
                public void onFailure() {
                    try {
                        callback.onFailure();
                    } finally {
                        latch.countDown();
                    }
                }
            };
            ExternalSystemUtil.runTask(executionSettings,DefaultRunExecutor.EXECUTOR_ID,
                module.getProject(),
                GradleConstants.SYSTEM_ID, waitingCallback, ProgressExecutionMode.IN_BACKGROUND_ASYNC, false);
            while (!latch.await(100, TimeUnit.MILLISECONDS)) {
                progressIndicator.checkCanceled();
            }

            try (BufferedReader reader = Files.newBufferedReader(outputPath)) {
                String id;

                while ((id = reader.readLine()) != null) {
                    String file = reader.readLine();
                    String[] ids = id.split(":");
                    if (!isDependency(ModuleRootManager.getInstance(module), ids[0], ids[1]) && file != null) {
                        VirtualFile jarFile = getJarFile(file);
                        if (jarFile != null) {
                            result[file.endsWith("sources.jar") ? SOURCES : BINARY].add(jarFile);
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while collecting Quarkus deployment dependencies", e);
        } catch (ProcessCanceledException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    abstract String getScriptName();

    /**
     * Generate the Gradle init script that contributes the {@code listQuarkusDependencies} task.
     *
     * @param outputPath    the path to the task result file
     * @param deploymentIds the Maven coordinates of the module deployment JARs
     * @return the path to the generated init script
     * @throws IOException if an error occurs generating the file
     */
    private Path generateInitScript(Path outputPath, Set<String> deploymentIds, String moduleDirPath) throws IOException {
        String content = buildInitScript(outputPath, deploymentIds, moduleDirPath);
        Path initScript = Files.createTempFile("quarkus-list-dependencies", ".gradle");
        try (Writer writer = Files.newBufferedWriter(initScript, StandardCharsets.UTF_8)) {
            IOUtils.write(content, writer);
            return initScript;
        }
    }

    /**
     * Build the content of the init script.
     *
     * @param outputPath    the path to the task result file
     * @param deploymentIds the Maven coordinates of the module deployment JARs
     * @param moduleDirPath the directory of the analyzed module, used to select the target Gradle project
     * @return the content of the init script
     */
    // Package-private for testing (see GradleListQuarkusDependenciesInitScriptTest).
    String buildInitScript(Path outputPath, Set<String> deploymentIds, String moduleDirPath) {
        StringBuilder dependencies = new StringBuilder();
        deploymentIds.forEach(id ->
                dependencies.append("                project.dependencies.add('quarkusDeployment', '")
                        .append(id)
                        .append("')")
                        .append(System.lineSeparator()));
        return String.format(INIT_SCRIPT_TEMPLATE,
                dependencies.toString(),
                outputPath.toString().replace("\\", "\\\\"),
                moduleDirPath.replace("\\", "\\\\"));
    }

    private void processLibrary(Library library, ModuleRootManager manager, Set<String> deploymentIds) {
        VirtualFile[] files = library.getFiles(OrderRootType.CLASSES);
        if (files.length == 1) {
            File file = new File(files[0].getCanonicalPath().substring(0, files[0].getCanonicalPath().length() - 2));
            String deploymentIdStr = BuildToolDelegate.getDeploymentJarId(file);
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
        String path = toPath(artifactId);
        if (path != null) {
            return new File(M2_REPO, path);
        }
        return null;
    }

    private static String toPath(String artifactId) {
        String[] comps = artifactId.split(":");
        if (comps.length == 3) {
            return comps[0].replace('.', File.separatorChar) + File.separatorChar + comps[1] + File.separatorChar + comps[2] + File.separatorChar + comps[1] + '-' + comps[2] + ".jar";
        }
        return null;
    }

    private boolean isDependency(ModuleRootManager manager, String deploymentIdStr) {
        return Stream.of(manager.getOrderEntries()).anyMatch(entry -> entry instanceof LibraryOrderEntry && (GRADLE_LIBRARY_PREFIX + deploymentIdStr).equals(((LibraryOrderEntry) entry).getLibraryName()));
    }

    private boolean isDependency(ModuleRootManager manager, String groupId, String artifactId) {
        return Stream.of(manager.getOrderEntries()).anyMatch(entry -> entry instanceof LibraryOrderEntry && ((LibraryOrderEntry) entry).getLibraryName().startsWith(GRADLE_LIBRARY_PREFIX + groupId + ':' + artifactId));
    }

    @Override
    public void processImport(Module module) {
        Project project = module.getProject();
        File gradleFile = null;

        for (VirtualFile virtualFile : ModuleRootManager.getInstance(module).getContentRoots()) {
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
                gradleProjectImportBuilder.commit(project, (ModifiableModuleModel) null, (ModulesProvider) null);
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
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException |
                     InvocationTargetException e1) {
                throw new RuntimeException(e1);
            }
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RunnerAndConfigurationSettings getConfigurationDelegate(@NotNull Module module,
                                                                   @NotNull QuarkusRunConfiguration configuration,
                                                                   @Nullable Integer debugPort,
                                                                   @Nullable Integer quteDebugPort) {
        RunnerAndConfigurationSettings settings = RunManager.getInstance(module.getProject()).createConfiguration(module.getName() + " Quarkus (Gradle)", GradleExternalTaskConfigurationType.class);
        GradleRunConfiguration gradleConfiguration = (GradleRunConfiguration) settings.getConfiguration();
        gradleConfiguration.getSettings().getTaskNames().add("quarkusDev");
        String externalProjectPath = getModuleDirPath(module);
        gradleConfiguration.getSettings().setExternalProjectPath(externalProjectPath);
        gradleConfiguration.getSettings().setEnv(configuration.getEnv());
        String parameters = createParameters(module, configuration, debugPort, quteDebugPort);
        gradleConfiguration.getSettings().setScriptParameters(parameters);
        gradleConfiguration.setBeforeRunTasks(configuration.getBeforeRunTasks());
        return settings;
    }
    private static @NotNull String createParameters(@NotNull Module module,
                                                    @NotNull QuarkusRunConfiguration configuration,
                                                    @Nullable Integer debugPort,
                                                    @Nullable Integer quteDebugPort) {
        StringBuilder parameters = new StringBuilder();

        if (debugPort != null) {
            addParameter(parameters, "debug", debugPort);
            addParameter(parameters, "suspend", "y");
        }
        if (StringUtils.isNotBlank(configuration.getProfile())) {
            addParameter(parameters, "quarkus.profile", configuration.getProfile());
        }
        if (quteDebugPort != null) {
            addParameter(parameters, "quteDebugPort", quteDebugPort);
        }
        if (StringUtils.isNotBlank(configuration.getProgramParameters())) {
            String resolvedArgs = BuildToolDelegate.resolveCommandLine(configuration.getProgramParameters(), module);
            addParameter(parameters, "quarkus.args", "\"" + resolvedArgs + "\"");
        }
        return parameters.toString();
    }

    private static void addParameter(@NotNull StringBuilder parameters, @NotNull String name, @NotNull Object value) {
        if (!parameters.isEmpty()) {
            parameters.append(" ");
        }
        parameters.append("-D").append(name).append("=").append(value);
    }

    @Override
    public void addProjectImportListener(@NotNull Project project, @NotNull MessageBusConnection connection, @NotNull ProjectImportListener listener) {
        connection.subscribe(ProjectDataImportListener.TOPIC, new ProjectDataImportListener() {
            @Override
            public void onImportFinished(@Nullable String projectPath) {
                if (!ProjectUtil.guessProjectDir(project).getPath().contains(projectPath)) {
                    // The imported project doesn't belong to the input project, ignore it.
                    return;
                }

                ReadAction.nonBlocking(() -> {
                        List<Module> modules = new ArrayList<>();
                        Module[] existingModules = ModuleManager.getInstance(project).getModules();
                        for (Module module : existingModules) {
                            // Check if the module is a Gradle project
                            if (GradleRunnerUtil.isGradleModule(module) && isValidGradleModule(module)) {
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

    private static boolean isValidGradleModule(Module module) {
        // Remove Quarkus Gradle modules:
        // - $projectName.integrationTest
        // - $projectName.native-test
        // - $projectName.test
        String name = module.getName();
        return !(name.endsWith(".integrationTest") || name.endsWith(".native-test") || name.endsWith(".test"));
    }

    @Override
    public @Nullable Executor getOverridedExecutor() {
        // The run and debug gradle must be started with the DefaultRunExecutor
        // and not with the DefaultDebugExecutor in debug case otherwise
        // stop action doesn't kill the Quarkus application process.
        return ExecutorRegistry.getInstance().getExecutorById(ToolWindowId.RUN);
    }
}
