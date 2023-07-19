/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.lsp4ij;

import com.intellij.ide.util.importProject.ProgressIndicatorWrapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class IndexAwareLanguageClient extends LanguageClientImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexAwareLanguageClient.class);

    public IndexAwareLanguageClient(Project project) {
        super(project);
    }

    /**
     * Run the given function as a background task, wrapped in a read action
     *
     * @param title    the title of the action being run
     * @param function the function to execute in the background
     * @param <R>      the return type
     * @return the output of the function
     */
    protected <R> CompletableFuture<R> runAsBackground(String title, Function<ProgressIndicator, R> function) {
        return runAsBackground(title, function, true);
    }

    /**
     * Run the given function as a background task, which can be wrapped in a read action
     *
     * @param title        the title of the action being run
     * @param function     the function to execute in the background
     * @param inReadAction whether to wrap the function call in a read action or not
     * @param <R>          the return type
     * @return the output of the function
     */
    protected <R> CompletableFuture<R> runAsBackground(String title, Function<ProgressIndicator, R> function, boolean inReadAction) {
        CompletableFuture<R> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            Runnable task = () -> ProgressManager.getInstance().run(new Task.Backgroundable(getProject(), title) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    // Wrap the given progress indicator to cancel the task when the language client is disposed (when language server is stopped, ex : when all files are closed)
                    runTask(new LSPProgressIndicator(indicator, IndexAwareLanguageClient.this), future, function, inReadAction);
                }

                @Override
                public boolean isHeadless() {
                    return true;
                }
            });
            if (DumbService.getInstance(getProject()).isDumb()) {
                DumbService.getInstance(getProject()).runWhenSmart(task);
            } else {
                task.run();
            }
        });
        return future;
    }

    private <R> void runTask(@NotNull ProgressIndicator indicator, CompletableFuture<R> future, Function<ProgressIndicator, R> function, boolean inReadAction) {
        boolean done = false;
        for (int i = 0; !done && i < 10; ++i) {
            try {
                R result;
                if (inReadAction) {
                    result = ApplicationManager.getApplication().runReadAction((Computable<R>) () -> function.apply(indicator));
                } else {
                    result = function.apply(indicator);
                }
                if (isDisposed()) {
                    // The task is finished correctly, but the language client has been disposed
                    // We need to throw a CancellationException error to stop the process to avoid having a Stream closed error on the LanguageServerWrapper side.
                    throw new CancellationException();
                }
                future.complete(result);
                done = true;
            } catch (IndexNotReadyException ignored) {
            } catch (Throwable t) {
                if(t instanceof ProcessCanceledException) {
                    // Here we throw a standard CancellationException which will be intercept by the language server.
                    // In this case, the language server will know that the process must be cancelled.
                    t = new CancellationException();
                }
                future.completeExceptionally(t);
                done = true;
            }
        }
        if (!done) {
            DumbService.getInstance(getProject()).runWhenSmart(() -> runTask(indicator, future, function, inReadAction));
        }
    }

    /**
     * This method returns the file path to display in the progress bar.
     *
     * @param fileUri the file uri.
     *
     * @return  the file path to display in the progress bar.
     */
    protected String getFilePath(String fileUri) {
        VirtualFile file =  LSPIJUtils.findResourceFor(fileUri);
        if (file != null) {
            Module module = LSPIJUtils.getProject(file);
            if (module != null) {
                ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
                VirtualFile[] contentRoots = rootManager.getContentRoots();
                if (contentRoots.length > 0) {
                    return VfsUtil.findRelativePath(contentRoots[0], file, '/');
                }
            }
        }
        return fileUri;
    }
}
