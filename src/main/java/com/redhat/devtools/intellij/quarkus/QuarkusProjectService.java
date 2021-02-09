/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;
import com.redhat.devtools.intellij.quarkus.search.PropertiesManager;
import com.redhat.devtools.intellij.quarkus.search.PsiUtilsImpl;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.eclipse.lsp4mp.utils.JSONSchemaUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class QuarkusProjectService implements LibraryTable.Listener, BulkFileListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusProjectService.class);

    private final Project project;

    private final Map<Module, MutablePair<VirtualFile, Boolean>> schemas = new ConcurrentHashMap<>();

    public interface Listener {
        void libraryUpdated(Library library);
        void sourceUpdated(List<Pair<Module, VirtualFile>> sources);
    }

    public static QuarkusProjectService getInstance(Project project) {
        return ServiceManager.getService(project, QuarkusProjectService.class);
    }

    public static final Topic<Listener> TOPIC = Topic.create(QuarkusProjectService.class.getName(), Listener.class);

    private final MessageBusConnection connection;

    public QuarkusProjectService(Project project) {
        this.project = project;
        LibraryTablesRegistrar.getInstance().getLibraryTable(project).addListener(this, project);
        connection = ApplicationManager.getApplication().getMessageBus().connect(project);
        connection.subscribe(VirtualFileManager.VFS_CHANGES, this);
    }

    private void handleLibraryUpdate(Library library) {
            project.getMessageBus().syncPublisher(TOPIC).libraryUpdated(library);
            schemas.forEach((module, pair) -> {
                pair.setRight(Boolean.FALSE);
            });
    }

    @Override
    public void afterLibraryAdded(@NotNull Library newLibrary) {
        handleLibraryUpdate(newLibrary);
    }

    @Override
    public void afterLibraryRemoved(@NotNull Library library) {
        handleLibraryUpdate(library);
    }

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        List<Pair<Module, VirtualFile>> pairs = events.stream().map(event -> toPair(event)).filter(Objects::nonNull).collect(Collectors.toList());
        if (!pairs.isEmpty()) {
            pairs.forEach(pair -> schemas.computeIfPresent(pair.getLeft(), (m, p) -> {
                p.setRight(Boolean.FALSE);
                return p;
            }));
            project.getMessageBus().syncPublisher(TOPIC).sourceUpdated(pairs);
        }
    }

    private Pair<Module, VirtualFile> toPair(VFileEvent event) {
        VirtualFile file = event.getFile();
        if (file != null && file.exists() && "java".equalsIgnoreCase(file.getExtension())) {
            Module module = ProjectFileIndex.getInstance(project).getModuleForFile(file);
            if (module != null && (event instanceof VFileCreateEvent || event instanceof VFileContentChangeEvent || event instanceof VFileDeleteEvent)) {
                return Pair.of(module, file);
            }
        }
        return null;
    }

    public VirtualFile getSchema(Module module) {
        MutablePair<VirtualFile, Boolean> schemaEntry = schemas.get(module);
        if (schemaEntry == null || !schemaEntry.getRight()) {
            VirtualFile file = computeSchema(module, schemaEntry!=null?schemaEntry.getLeft():null);
            if (file != null) {
                if (schemaEntry != null) {
                    schemaEntry.setRight(Boolean.TRUE);
                } else {
                    schemaEntry = new MutablePair<>(file, Boolean.TRUE);
                    schemas.put(module, schemaEntry);
                }
            }
        }
        return schemaEntry!=null?schemaEntry.getLeft():null;
    }

    private static VirtualFile createJSONSchemaFile(String name) throws IOException {
        File file = new File(System.getProperty("java.io.tmpdir"), name + "-schema.json");
        file.createNewFile();
        file.deleteOnExit();
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
    }


    private VirtualFile computeSchema(Module module, VirtualFile schemaFile) {
        try {
            if (schemaFile == null) {
                schemaFile = createJSONSchemaFile(module.getName());
            }
            final VirtualFile schemaFile1 = schemaFile;
            DumbService.getInstance(module.getProject()).runWhenSmart(() -> {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        MicroProfileProjectInfo info = PropertiesManager.getInstance().getMicroProfileProjectInfo(module,
                                MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.TEST, PsiUtilsImpl.getInstance(),
                                DocumentFormat.Markdown);
                        String schema = JSONSchemaUtils.toJSONSchema(info, false);
                        VfsUtil.saveText(schemaFile1, schema);
                    } catch (IOException e) {
                        LOGGER.warn(e.getLocalizedMessage(), e);
                    }
                });
            });
            return schemaFile;
        } catch (IOException| ProcessCanceledException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }
        return null;
    }
}
