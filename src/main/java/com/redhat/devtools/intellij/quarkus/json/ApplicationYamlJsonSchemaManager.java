/*******************************************************************************
 * Copyright (c) 2024 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.quarkus.json;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.redhat.devtools.intellij.lsp4mp4ij.classpath.ClasspathResourceChangedManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.redhat.devtools.intellij.quarkus.QuarkusPluginDisposable;
import com.redhat.devtools.lsp4ij.settings.jsonSchema.LSPJsonSchemaProviderFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pooling of JsonSchemaProvider used by Server / Configuration editors.
 * We need this pooling because there are no way to register {@link com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider}
 * dynamically with {@link LSPJsonSchemaProviderFactory}.
 */
public class ApplicationYamlJsonSchemaManager implements ClasspathResourceChangedManager.Listener, Disposable  {

    private static final int JSON_SCHEMA_FILE_PROVIDER_POOL_SIZE = 20;

    public static ApplicationYamlJsonSchemaManager getInstance(@NotNull Project project) {
        return project.getService(ApplicationYamlJsonSchemaManager.class);
    }

    private final List<ApplicationYamlJsonSchemaFileProvider> providers;
    private final MessageBusConnection connection;

    public ApplicationYamlJsonSchemaManager(@NotNull Project project) {
        providers = new ArrayList<>();
        // Create 100 dummy ApplicationYamlJsonSchemaFileProvider
        for (int i = 0; i < JSON_SCHEMA_FILE_PROVIDER_POOL_SIZE; i++) {
            providers.add(new ApplicationYamlJsonSchemaFileProvider(i, project));
        }
        connection = project.getMessageBus().connect(QuarkusPluginDisposable.getInstance(project));
        connection.subscribe(ClasspathResourceChangedManager.TOPIC, this);
    }

    public List<ApplicationYamlJsonSchemaFileProvider> getProviders() {
        return providers;
    }

    /**
     * Free the {@link ApplicationYamlJsonSchemaFileProvider} stored at the given index from the pool
     * (when a Server / Configuration editor is disposed).
     *
     * @param index the index of {@link ApplicationYamlJsonSchemaFileProvider}.
     */
    public void reset(@NotNull Integer index) {
        ApplicationYamlJsonSchemaFileProvider provider = getProviders().get(index);
        provider.reset();
    }

    @Override
    public void librariesChanged() {
        for(var provider : getProviders()) {
            provider.reset();
        }
    }

    @Override
    public void sourceFilesChanged(Set<Pair<VirtualFile, Module>> sources) {
        Set<Module> modules = sources
                .stream()
                .filter(source -> isJavaFile(source.getFirst()))
                .map(source -> source.getSecond())
                .collect(Collectors.toSet());
        for (var module : modules ) {
            var provider = ApplicationYamlJsonSchemaFileProvider.getProviderFor(module);
            if (provider != null) {
                provider.reset();
            }
        }
    }

    private boolean isJavaFile(@NotNull VirtualFile file) {
        return PsiMicroProfileProjectManager.isJavaFile(file);
    }

    @Override
    public void dispose() {
        connection.disconnect();
    }

}
