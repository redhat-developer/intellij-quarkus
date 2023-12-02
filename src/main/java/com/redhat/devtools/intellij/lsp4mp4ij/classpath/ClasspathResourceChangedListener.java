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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Classpath resource changed listener used to track update of:
 *
 * <ul>
 *     <li>library has changed.</li>
 *     <li>Java source file has changed.</li>
 *     <li>microprofile-config.properties file has changed.</li>
 *   </ul>
 */
class ClasspathResourceChangedListener extends PsiTreeChangeAdapter implements BulkFileListener, LibraryTable.Listener, ModuleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClasspathResourceChangedListener.class);

    private final ClasspathResourceChangedManager manager;

    ClasspathResourceChangedListener(ClasspathResourceChangedManager manager) {
        this.manager = manager;
    }

    // Track library changes

    @Override
    public void afterLibraryAdded(@NotNull Library newLibrary) {
        handleLibraryUpdate(newLibrary);
    }

    @Override
    public void afterLibraryRemoved(@NotNull Library library) {
        handleLibraryUpdate(library);
    }

    private void handleLibraryUpdate(Library library) {
        LOGGER.info("handleLibraryUpdate called " + library.getName());
        // Notify that a library has changed.
        final var notifier = manager.getResourceChangedNotifier();
        notifier.addLibrary(library);
    }

    // Track Psi file changes

    @Override
    public void childAdded(@NotNull PsiTreeChangeEvent event) {
        handleChangedPsiTree(event);
    }

    @Override
    public void childRemoved(@NotNull PsiTreeChangeEvent event) {
        handleChangedPsiTree(event);
    }

    @Override
    public void childReplaced(@NotNull PsiTreeChangeEvent event) {
        handleChangedPsiTree(event);
    }

    @Override
    public void childMoved(@NotNull PsiTreeChangeEvent event) {
        handleChangedPsiTree(event);
    }

    @Override
    public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
        handleChangedPsiTree(event);
    }

    @Override
    public void propertyChanged(@NotNull PsiTreeChangeEvent event) {
        handleChangedPsiTree(event);
    }

    private void handleChangedPsiTree(PsiTreeChangeEvent event) {
        // A Psi file has been changed in the editor
        PsiFile psiFile = event.getFile();
        if (psiFile == null) {
            return;
        }
        tryToAddSourceFile(psiFile.getVirtualFile(), true);
    }

    // Track file system changes

    @Override
    public void before(@NotNull List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
            boolean expectedEvent = (event instanceof VFileDeleteEvent);
            if (expectedEvent) {
                // A file has been deleted
                // We need to track delete event in 'before' method because we need the project of the file (in after we loose this information).
                tryToAddSourceFile(event.getFile(), false);
            }
        }
    }

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
            boolean expectedEvent = (event instanceof VFileCreateEvent || event instanceof VFileContentChangeEvent);
            if (expectedEvent) {
                // A file has been created, updated
                tryToAddSourceFile(event.getFile(), false);
            }
        }
    }

    private static boolean isJavaFile(VirtualFile file) {
        return PsiMicroProfileProjectManager.isJavaFile(file);
    }

    private static boolean isConfigSource(VirtualFile file) {
        return PsiMicroProfileProjectManager.isConfigSource(file);
    }

    private void tryToAddSourceFile(VirtualFile file, boolean checkExistingFile) {
        if (checkExistingFile && (file == null || !file.exists())) {
            // The file doesn't exist
            return;
        }
        var project = manager.getProject();
        if (!isJavaFile(file) && !isConfigSource(file)) {
            return;
        }
        // The file is a Java file or microprofile-config.properties
        Module module = LSPIJUtils.getModule(file, project);
        if (module == null || module.isDisposed()) {
            return;
        }
        // Notify that the file has changed
        var notifier = manager.getResourceChangedNotifier();
        notifier.addSourceFile(Pair.pair(file, module));
    }

}
