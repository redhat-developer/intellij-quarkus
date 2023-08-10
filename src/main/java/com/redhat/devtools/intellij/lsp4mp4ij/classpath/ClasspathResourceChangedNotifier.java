/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.classpath;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Source file change notifier with a debounce mode.
 */
public class ClasspathResourceChangedNotifier implements Disposable {

    private static final long DEBOUNCE_DELAY = 1000;

    private final Project project;

    private Timer debounceTimer;
    private TimerTask debounceTask;

    private final Set<Pair<VirtualFile, Module>> sourceFiles;
    private boolean librariesChanged;
    private final List<RunnableProgress> processBeforeLibrariesChanged;
    private boolean disposed;

    public ClasspathResourceChangedNotifier(Project project, List<RunnableProgress> preprocessors) {
        this.project = project;
        this.processBeforeLibrariesChanged = preprocessors;
        sourceFiles = new HashSet<>();
    }

    public synchronized void addLibrary(Library library) {
        if (debounceTask != null) {
            debounceTask.cancel();
        }
        librariesChanged = true;
        asyncNotifyChanges();
    }

    public synchronized void addSourceFile(Pair<VirtualFile, Module> pair) {
        if (debounceTask != null) {
            debounceTask.cancel();
        }
        synchronized (sourceFiles) {
            sourceFiles.add(pair);
        }

        asyncNotifyChanges();
    }

    private void asyncNotifyChanges() {
        if (isDisposed()) {
            return;
        }
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            notifyChanges();
        } else {
            debounceTask = new TimerTask() {
                @Override
                public void run() {
                    notifyChanges();
                }
            };

            if (debounceTimer == null) {
                debounceTimer = new Timer();
            }

            debounceTimer.schedule(debounceTask, DEBOUNCE_DELAY);
        }
    }

    private void notifyChanges() {
        if (isDisposed()) {
            return;
        }
        synchronized (sourceFiles) {
            // Java, config sources files has changed
            project.getMessageBus().syncPublisher(ClasspathResourceChangedManager.TOPIC).sourceFilesChanged(sourceFiles);
            sourceFiles.clear();
        }
        if (librariesChanged) {
            // Java Libraries has changed
            if (processBeforeLibrariesChanged.isEmpty() || ApplicationManager.getApplication().isUnitTestMode()) {
                // No preprocessor or Test context, send directly the librariesChanged event.
                for (var runnable : processBeforeLibrariesChanged) {
                    runnable.run(new EmptyProgressIndicator());
                }
                // Send the libraries changed event
                project.getMessageBus().syncPublisher(ClasspathResourceChangedManager.TOPIC).librariesChanged();
                librariesChanged = false;
            } else {
                // There are some preprocessor (ex : Quarkus deployment preprocessor to load Quarkus deployment dependencies in the classpath).
                ApplicationManager.getApplication().invokeLater(() -> {
                    new Task.Backgroundable(project, "Overriding MicroProfile classpath...", true) {
                        @Override
                        public void run(@NotNull ProgressIndicator progressIndicator) {
                            try {
                                // Execute preprocessor
                                progressIndicator.setIndeterminate(false);
                                progressIndicator.checkCanceled();
                                for (var runnable : processBeforeLibrariesChanged) {
                                    runnable.run(progressIndicator);
                                }
                            } finally {
                                // Send the libraries changed event
                                project.getMessageBus().syncPublisher(ClasspathResourceChangedManager.TOPIC).librariesChanged();
                                librariesChanged = false;
                            }
                        }
                    }.queue();
                }, ModalityState.defaultModalityState(), project.getDisposed());
            }
        }
    }

    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public void dispose() {
        if (isDisposed()) {
            return;
        }
        this.disposed = true;
        if (debounceTask != null) {
            debounceTask.cancel();
            debounceTask =null;
        }
        if (debounceTimer != null) {
            debounceTimer.cancel();
            debounceTimer = null;
        }
    }
}