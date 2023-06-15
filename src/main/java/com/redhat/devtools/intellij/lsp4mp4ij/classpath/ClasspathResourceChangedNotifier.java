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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.*;

/**
 * Source file change notifier with a debounce mode.
 */
public class ClasspathResourceChangedNotifier implements Disposable  {

    private static final long DEBOUNCE_DELAY = 1000;

    private final Project project;

    private Timer debounceTimer;
    private TimerTask debounceTask;

    private final Set<Pair<VirtualFile, Module>> sourceFiles;
    private boolean librariesChanged;

    public ClasspathResourceChangedNotifier(Project project) {
        this.project = project;
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
        synchronized (sourceFiles) {
            project.getMessageBus().syncPublisher(ClasspathResourceChangedManager.TOPIC).sourceFilesChanged(sourceFiles);
            sourceFiles.clear();
        }
        if (librariesChanged) {
            project.getMessageBus().syncPublisher(ClasspathResourceChangedManager.TOPIC).librariesChanged();
            librariesChanged = false;
        }
    }

    @Override
    public void dispose() {
        if (debounceTask != null) {
            debounceTask.cancel();
        }
        if(debounceTimer != null) {
            debounceTimer.cancel();
        }
    }
}