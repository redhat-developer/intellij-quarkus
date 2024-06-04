/*******************************************************************************
 * Copyright (c) 2023 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.OrderEntryUtil;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.redhat.devtools.intellij.lsp4mp4ij.classpath.ClasspathResourceChangedManager;
import com.redhat.devtools.intellij.quarkus.buildtool.BuildToolDelegate;
import com.redhat.devtools.intellij.quarkus.search.QuarkusModuleComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import static com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil.isQuarkusModule;

/**
 * Quarkus deployment support provides the capability to collect, download and add to a module classpath the Quarkus deployment dependencies.
 */
public class QuarkusDeploymentSupport implements ClasspathResourceChangedManager.Listener, Disposable {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusDeploymentSupport.class);
    private static final Key<CompletableFuture<Void>> QUARKUS_DEPLOYMENT_SUPPORT_KEY = new Key<>(QuarkusDeploymentSupport.class.getName());

    private final MessageBusConnection connection;
    private final Project project;

    public static QuarkusDeploymentSupport getInstance(@NotNull Project project) {
        return project.getService(QuarkusDeploymentSupport.class);
    }

    public QuarkusDeploymentSupport(Project project) {
        this.project = project;
        connection = project.getMessageBus().connect(QuarkusPluginDisposable.getInstance(project));
        connection.subscribe(ClasspathResourceChangedManager.TOPIC, this);
    }

    /**
     * Update module classpath with Quarkus deployment dependencies if needed in async mode.
     *
     * @param module the module.
     * @return the future which updates the classpath.
     */
    public CompletableFuture<Void> updateClasspathWithQuarkusDeploymentAsync(Module module) {
        CompletableFuture<Void> deploymentSupport = module.getUserData(QUARKUS_DEPLOYMENT_SUPPORT_KEY);
        if (isOutOfDated(deploymentSupport)) {
            return internalUpdateClasspathWithQuarkusDeployment(module);
        }
        return deploymentSupport;
    }

    /**
     * Update module classpath with Quarkus deployment dependencies if needed.
     *
     * @param module            the module.
     * @param progressIndicator the progress indicator.
     */
    public static void updateClasspathWithQuarkusDeployment(Module module, ProgressIndicator progressIndicator) {
        if (module.isDisposed())
            return;
        LOGGER.info("Ensuring library to " + module.getName());
        long start = System.currentTimeMillis();
        BuildToolDelegate toolDelegate = BuildToolDelegate.getDelegate(module);
        if (toolDelegate != null) {
            LOGGER.info("Tool delegate found for " + module.getName());
            if (isQuarkusModule(module)) {
                LOGGER.info("isQuarkus module " + module.getName());
                QuarkusModuleComponent component = module.getComponent(QuarkusModuleComponent.class);
                Integer previousHash = component.getHash();
                Integer actualHash = computeHash(module);
                var qlib = OrderEntryUtil.findLibraryOrderEntry(ModuleRootManager.getInstance(module), QuarkusConstants.QUARKUS_DEPLOYMENT_LIBRARY_NAME);
                if (qlib == null || (actualHash != null && !actualHash.equals(previousHash)) ||
                        !QuarkusConstants.QUARKUS_DEPLOYMENT_LIBRARY_VERSION.equals(component.getVersion())) {
                    ModuleRootModificationUtil.updateModel(module, model -> {
                        LibraryTable table = model.getModuleLibraryTable();
                        Library library = table.getLibraryByName(QuarkusConstants.QUARKUS_DEPLOYMENT_LIBRARY_NAME);
                        while (library != null) {
                            table.removeLibrary(library);
                            try {
                                TelemetryService.instance().action(TelemetryService.MODEL_PREFIX + "removeLibrary");
                            } catch (Exception e) {

                            }
                            library = table.getLibraryByName(QuarkusConstants.QUARKUS_DEPLOYMENT_LIBRARY_NAME);
                        }
                        progressIndicator.checkCanceled();
                        progressIndicator.setText("Adding in ''" + module.getName() + "'' Quarkus deployment dependencies to classpath...");
                        List<VirtualFile>[] files = toolDelegate.getDeploymentFiles(module, progressIndicator);
                        LOGGER.info("Adding library to " + module.getName() + " previousHash=" + previousHash + " newHash=" + actualHash);
                        try {
                            TelemetryService.instance().action(TelemetryService.MODEL_PREFIX + "addLibrary").send();
                        } catch (Exception e) {

                        }
                        addLibrary(model, files);
                    });
                    component.setHash(actualHash);
                    component.setVersion(QuarkusConstants.QUARKUS_DEPLOYMENT_LIBRARY_VERSION);
                }
            }
        }
        LOGGER.info("ensureQuarkusLibrary ran in " + (System.currentTimeMillis() - start));
    }


    private static boolean isOutOfDated(CompletableFuture<Void> loader) {
        return loader == null;
    }

    @NotNull
    private synchronized static CompletableFuture<Void> internalUpdateClasspathWithQuarkusDeployment(Module module) {
        CompletableFuture<Void> deploymentSupport = module.getUserData(QUARKUS_DEPLOYMENT_SUPPORT_KEY);
        if (!isOutOfDated(deploymentSupport)) {
            return deploymentSupport;
        }
        var project = module.getProject();
        final CompletableFuture<Void> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            Runnable task = () -> ProgressManager.getInstance().run(new Task.Backgroundable(project, "Adding Quarkus deployment dependencies to classpath...") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        long start = System.currentTimeMillis();
                        ProgressIndicator wrappedIndicator = new ProgressIndicatorWrapper(indicator) {

                            @Override
                            public boolean isCanceled() {
                                return super.isCanceled() || future.isCancelled();
                            }
                        };
                        updateClasspathWithQuarkusDeployment(module, wrappedIndicator);
                        long elapsed = System.currentTimeMillis() - start;
                        LOGGER.info("Ensured QuarkusLibrary in {} ms", elapsed);
                        future.complete(null);
                    } catch (ProcessCanceledException e) {
                        //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
                        //TODO delete block when minimum required version is 2024.2
                        future.cancel(true);
                    } catch (CancellationException e) {
                        future.cancel(true);
                    } catch (Exception e) {
                        LOGGER.error("Error while adding Quarkus deployment dependencies to classpath in '" + module.getName() + "'", e);
                        future.completeExceptionally(e);
                    }
                }
            });
            if (DumbService.getInstance(project).isDumb()) {
                DumbService.getInstance(project).runWhenSmart(task);
            } else {
                task.run();
            }
        });
        module.putUserData(QUARKUS_DEPLOYMENT_SUPPORT_KEY, future);
        return future;
    }

    private static void addLibrary(ModifiableRootModel model, List<VirtualFile>[] quarkusDeploymentDependencies) {
        // Create Quarkus Deployment library.
        LibraryEx library = (LibraryEx) model.getModuleLibraryTable().createLibrary(QuarkusConstants.QUARKUS_DEPLOYMENT_LIBRARY_NAME);
        LibraryEx.ModifiableModelEx libraryModel = library.getModifiableModel();

        // Add quarkus deployment dependencies binaries
        for (VirtualFile rootFile : quarkusDeploymentDependencies[BuildToolDelegate.BINARY]) {
            libraryModel.addRoot(rootFile, OrderRootType.CLASSES);
        }
        // Add quarkus deployment dependencies sources
        for (VirtualFile rootFile : quarkusDeploymentDependencies[BuildToolDelegate.SOURCES]) {
            libraryModel.addRoot(rootFile, OrderRootType.SOURCES);
        }

        LibraryOrderEntry entry = model.findLibraryOrderEntry(library);
        assert entry != null : library;
        entry.setScope(DependencyScope.RUNTIME);
        entry.setExported(false);

        // Update classpath
        ApplicationManager.getApplication().invokeAndWait(() -> {
            WriteAction.run(libraryModel::commit);
        });
    }

    private static Integer computeHash(Module module) {
        ModuleRootManager manager = ModuleRootManager.getInstance(module);
        Set<String> files = manager.processOrder(new RootPolicy<Set<String>>() {
            @Override
            public Set<String> visitLibraryOrderEntry(@NotNull LibraryOrderEntry libraryOrderEntry, Set<String> value) {
                if (!isQuarkusDeploymentLibrary(libraryOrderEntry) && isQuarkusExtensionWithDeploymentArtifact(libraryOrderEntry.getLibrary())) {
                    for (VirtualFile file : libraryOrderEntry.getFiles(OrderRootType.CLASSES)) {
                        value.add(file.getPath());
                    }
                }
                return value;
            }
        }, new HashSet<>());
        return files.isEmpty() ? null : files.hashCode();
    }


    private static boolean isQuarkusDeploymentLibrary(@NotNull LibraryOrderEntry libraryOrderEntry) {
        return libraryOrderEntry.getLibraryName() != null &&
                libraryOrderEntry.getLibraryName().equalsIgnoreCase(QuarkusConstants.QUARKUS_DEPLOYMENT_LIBRARY_NAME);
    }


    private static boolean isQuarkusExtensionWithDeploymentArtifact(@Nullable Library library) {
        if (library != null) {
            for (VirtualFile vFile : library.getFiles(OrderRootType.CLASSES)) {
                if (vFile.isDirectory() && BuildToolDelegate.hasExtensionProperties(VfsUtilCore.virtualToIoFile(vFile))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void dispose() {
        connection.dispose();
        cancelFutures();
    }

    @Override
    public void librariesChanged() {
        cancelFutures();
    }

    private void cancelFutures() {
        if (!project.isDisposed()) {
            for (var module : ModuleManager.getInstance(project).getModules()) {
                CompletableFuture<Void> loader = module.getUserData(QUARKUS_DEPLOYMENT_SUPPORT_KEY);
                if (loader != null) {
                    loader.cancel(true);
                    module.putUserData(QUARKUS_DEPLOYMENT_SUPPORT_KEY, null);
                }
            }
        }
    }

    @Override
    public void sourceFilesChanged(Set<Pair<VirtualFile, Module>> sources) {
        // Do nothing
    }

}
