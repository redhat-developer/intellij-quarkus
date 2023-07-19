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

import com.intellij.json.JsonFileType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.classpath.ClasspathResourceChangedManager;
import org.apache.commons.lang3.tuple.MutablePair;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.eclipse.lsp4mp.utils.JSONSchemaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class QuarkusProjectService implements ClasspathResourceChangedManager.Listener, Disposable {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusProjectService.class);

    private final Map<Module, MutablePair<VirtualFile, Boolean>> schemas = new ConcurrentHashMap<>();

    public static QuarkusProjectService getInstance(Project project) {
        return ServiceManager.getService(project, QuarkusProjectService.class);
    }

    private final Project project;

    private final MessageBusConnection connection;

    public QuarkusProjectService(Project project) {
        this.project = project;
        ClasspathResourceChangedManager.getInstance(project)
                .addClasspathOverrider(new ClasspathResourceChangedManager.ClasspathOverrider() {
                    @Override
                    public void overrideClasspath(Module module) {
                        QuarkusModuleUtil.ensureQuarkusLibrary(module);
                    }
                });
        connection = project.getMessageBus().connect();
        connection.subscribe(ClasspathResourceChangedManager.TOPIC, this);
    }

    public VirtualFile getSchema(Module module) {
        var schemaEntry = schemas.get(module);
        if (schemaEntry == null || !schemaEntry.getRight()) {
            VirtualFile file = computeSchema(module, schemaEntry != null ? schemaEntry.getLeft() : null);
            if (file != null) {
                if (schemaEntry != null) {
                    schemaEntry.setRight(Boolean.TRUE);
                } else {
                    schemaEntry = new MutablePair<>(file, Boolean.TRUE);
                    schemas.put(module, schemaEntry);
                }
            }
        }
        return schemaEntry != null ? schemaEntry.getLeft() : null;
    }

    private static VirtualFile createJSONSchemaFile(String name) throws IOException {
        return new LightVirtualFile(name + "-schema.json", JsonFileType.INSTANCE, "");
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
                                MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.TEST, PsiUtilsLSImpl.getInstance(module.getProject()),
                                DocumentFormat.Markdown, new EmptyProgressIndicator());
                        String schema = JSONSchemaUtils.toJSONSchema(info, false);
                        VfsUtil.saveText(schemaFile1, schema);
                    } catch (IOException e) {
                        LOGGER.warn(e.getLocalizedMessage(), e);
                    }
                });
            });
            return schemaFile;
        } catch (IOException | ProcessCanceledException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }
        return null;
    }

    @Override
    public void librariesChanged() {
        // Do nothing
    }

    @Override
    public void sourceFilesChanged(Set<com.intellij.openapi.util.Pair<VirtualFile, Module>> sources) {
        sources.forEach(pair -> schemas.computeIfPresent(pair.getSecond(), (m, p) -> {
            p.setRight(Boolean.FALSE);
            return p;
        }));
    }

    @Override
    public void moduleUpdated(Module module) {
        var pair = schemas.get(module);
        if (pair != null) {
            pair.setRight(Boolean.FALSE);
        }
    }

    @Override
    public void modulesUpdated() {
        schemas.forEach((module, pair) -> {
            pair.setRight(Boolean.FALSE);
        });
    }

    public void processModules() {
        ClasspathResourceChangedManager.getInstance(project).processModules();
    }

    @Override
    public void dispose() {
        connection.disconnect();
    }

}
