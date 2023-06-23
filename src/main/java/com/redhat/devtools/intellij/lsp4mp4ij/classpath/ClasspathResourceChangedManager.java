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

import com.intellij.ProjectTopics;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiManager;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Classpath resource change manager provides the capability to track update of libraries changed and Java, microprofile-config properties files
 * by any component by registering a listener {@link Listener}.
 *
 * <code>
 * ClasspathResourceChangeManager.Listener myListener = ...
 * project.getMessageBus().connect(project).subscribe(ClasspathResourceChangeManager.TOPIC, myListener);
 * </code>
 *
 *
 * <ul>
 *     <li>Track update of libraries is done with {@link com.intellij.openapi.roots.libraries.LibraryTable.Listener}.
 *     In other words {@link Listener#librariesChanged()}  are fired when all libraries are inserted, deleted, updated.</li>
 *     <li>Track update of Java, microprofile-config properties files are done when Java Psi file is updated, when Java file is created, deleted, saved.</li>
 * </ul>
 */
public class ClasspathResourceChangedManager implements Disposable {

    public static final Topic<ClasspathResourceChangedManager.Listener> TOPIC = Topic.create(ClasspathResourceChangedManager.class.getName(), ClasspathResourceChangedManager.Listener.class);

    private final ExecutorService executor;
    private final ClasspathResourceChangedNotifier resourceChangedNotifier;
    private final MessageBusConnection projectConnection;
    private final MessageBusConnection appConnection;
    private final ClasspathResourceChangedListener listener;
    private final List<ClasspathOverrider> overriders;

    public static ClasspathResourceChangedManager getInstance(Project project) {
        return ServiceManager.getService(project, ClasspathResourceChangedManager.class);
    }

    public interface Listener {

        void librariesChanged();

        void sourceFilesChanged(Set<Pair<VirtualFile, Module>> sources);

        void moduleUpdated(Module module);

        void modulesUpdated();
    }

    public interface ClasspathOverrider {

        void overrideClasspath(Module module);

    }

    private final Project project;

    public ClasspathResourceChangedManager(Project project) {
        this.project = project;
        this.overriders = new ArrayList<>();
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            this.executor = ConcurrencyUtil.newSameThreadExecutorService();
        } else {
            this.executor = new ThreadPoolExecutor(0, 1,
                    1L, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(),
                    r -> new Thread(r, "Quarkus lib pool " + project.getName()));
        }
        // Send source files changed in debounce mode
        this.resourceChangedNotifier = new ClasspathResourceChangedNotifier(project);
        listener = new ClasspathResourceChangedListener(this);
        projectConnection = project.getMessageBus().connect();
        // Track end of Java libraries update
        LibraryTablesRegistrar.getInstance().getLibraryTable(project).addListener(listener);
        // Track update of Psi Java, properties files
        PsiManager.getInstance(project).addPsiTreeChangeListener(listener, project);
        // Track modules changes
        projectConnection.subscribe(ProjectTopics.MODULES, listener);
        // Track delete, create, update of file
        appConnection = ApplicationManager.getApplication().getMessageBus().connect(project);
        appConnection.subscribe(VirtualFileManager.VFS_CHANGES, listener);
        processModules();
    }

    public void processModules() {
        listener.processModules();
    }

    public void addClasspathOverrider(ClasspathOverrider overrider) {
        overriders.add(overrider);
    }

    @Override
    public void dispose() {
        this.resourceChangedNotifier.dispose();
        this.projectConnection.disconnect();
        this.appConnection.disconnect();
        LibraryTablesRegistrar.getInstance().getLibraryTable(project).removeListener(listener);
        PsiManager.getInstance(project).removePsiTreeChangeListener(listener);
        executor.shutdown();
    }

    Project getProject() {
        return project;
    }

    ClasspathResourceChangedNotifier getResourceChangedNotifier() {
        return resourceChangedNotifier;
    }

    ExecutorService getExecutor() {
        return executor;
    }

    List<ClasspathOverrider> getOverriders() {
        return overriders;
    }
}
