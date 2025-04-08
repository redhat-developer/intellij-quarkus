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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.SchemaType;
import com.jetbrains.jsonSchema.impl.JsonSchemaVersion;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.quarkus.ProgressIndicatorWrapper;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.eclipse.lsp4mp.utils.JSONSchemaUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Json Schema provider used to provide completion, validation, hover in application.yaml.
 */
public class ApplicationYamlJsonSchemaFileProvider implements JsonSchemaFileProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationYamlJsonSchemaFileProvider.class);

    private static final Key<ApplicationYamlJsonSchemaFileProvider> JSON_SCHEMA_PROVIDER = Key.create("quarkus.application.yaml.schema.for.module");

    private final String jsonFilename;
    private final @NotNull Project project;
    private final @NotNull JsonSchemaLightVirtualFile jsonSchemaFile;
    private @Nullable Module module;
    private boolean updating;
    private long updatedTime;
    private long toUpdateTime;

    /**
     * LSP Server / Configuration {@link com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider} constructor.
     *
     * @param index   the index where the provider instance is stored in the pool of providers managed by {@link ApplicationYamlJsonSchemaManager}.
     * @param project the project.
     */
    public ApplicationYamlJsonSchemaFileProvider(int index, @NotNull Project project) {
        this.jsonFilename = generateJsonFileName(index);
        this.project = project;
        this.jsonSchemaFile = new JsonSchemaLightVirtualFile(generateJsonSchemaFileName(index), "");
        this.module = null;
    }

    @Override
    public boolean isAvailable(@NotNull VirtualFile file) {
        // Is file name matches application.yaml ?
        if (!QuarkusModuleUtil.isQuarkusYamlFile(file)) {
            return false;
        }
        // Is module is a Quarkus project?
        Module module = LSPIJUtils.getModule(file, project);
        if (!QuarkusModuleUtil.isQuarkusModule(module)) {
            return false;
        }
        // Is module is already associated to a Json Schema provider?
        var provider = getProviderFor(module);
        if (provider != null && provider != this) {
            return false;
        }
        if (this.module == null) {
            // Associate a Json Schema provider to the module
            module.putUserData(JSON_SCHEMA_PROVIDER, this);
            this.module = module;
            reset();
            return true;
        }
        return module.equals(this.module);
    }

    /**
     * Returns the associated Json provider to the given module and null otherwise.
     * @param module the module.
     * @return the associated Json provider to the given module and null otherwise.
     */
    @Nullable
    public static ApplicationYamlJsonSchemaFileProvider getProviderFor(@NotNull Module module) {
        return module.getUserData(JSON_SCHEMA_PROVIDER);
    }

    @Nullable
    @Override
    public final VirtualFile getSchemaFile() {
        if (module != null) {
            updateJsonSchemaIfNeededFor(module);
        }
        return jsonSchemaFile;
    }

    @NotNull
    @Override
    public final String getName() {
        return jsonFilename;
    }

    @NotNull
    @Override
    public final SchemaType getSchemaType() {
        return SchemaType.schema;
    }

    @Override
    public final JsonSchemaVersion getSchemaVersion() {
        return JsonSchemaVersion.SCHEMA_7;
    }

    @NotNull
    @Override
    public final String getPresentableName() {
        return getName();
    }

    @Override
    public boolean isUserVisible() {
        return false;
    }

    protected static void reloadPsi(@Nullable VirtualFile file) {
        if (file != null) {
            file.refresh(true, false, () -> file.refresh(false, false));
        }
    }

    private static String generateJsonFileName(int index) {
        return "quarkus.application.yaml." + index + ".json";
    }

    private static String generateJsonSchemaFileName(int index) {
        return "quarkus.application.yaml." + index + ".schema.json";
    }

    /**
     * Free the Json Schema provider (when Java sources , Libraries changed) to evict the cached Json Schema.
     */
    public void reset() {
        toUpdateTime = System.currentTimeMillis();
    }

    /**
     * Update file content.
     *
     * @param content the new content.
     * @param file    the file to update.
     */
    private static void updateFileContent(@NotNull String content,
                                          @NotNull JsonSchemaLightVirtualFile file) {
        if (Objects.equals(content, file.getContent())) {
            // No changes, don't update the file.
            return;
        }
        // Update the virtual file content and the modification stamp (used by Json Schema cache)
        file.setContent(content);
        // Synchronize the Psi file from the new content of the virtual file and the modification stamp (used by Json Schema cache)
        reloadPsi(file);
    }

    /**
     * Update Json Schema for teh given module if needed.
     * @param module the module.
     */
    private void updateJsonSchemaIfNeededFor(@NotNull Module module) {
        if (updating || toUpdateTime == updatedTime) {
            // - the Json schema is updating
            // - or the Json Schema doesn't need to be updated
            return;
        }
        updating = true;
        updatedTime = toUpdateTime;
        final long currentUpdatedTime = updatedTime;
        var project = module.getProject();
        // Generate and update the Json Schema when indexed files are finished.
        DumbService.getInstance(project)
                .runWhenSmart(() -> {
                    // Check if previous start of Json Schema update is relevant
                    if (isCanceled(currentUpdatedTime)) {
                        return;
                    }
                    ApplicationManager.getApplication()
                            .runWriteAction(() -> {
                                // Check if previous start of Json Schema update is relevant
                                if (isCanceled(currentUpdatedTime)) {
                                    return;
                                }
                                try {
                                    // Collect all MicroProfile/Quarksu properties from the given module.
                                    MicroProfileProjectInfo info = PropertiesManager.getInstance().getMicroProfileProjectInfo(module,
                                            MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.TEST, PsiUtilsLSImpl.getInstance(project),
                                            DocumentFormat.Markdown, new ProgressIndicatorWrapper(new EmptyProgressIndicator()) {
                                                @Override
                                                public boolean isCanceled() {
                                                    return super.isCanceled() || ApplicationYamlJsonSchemaFileProvider.this.isCanceled(currentUpdatedTime);
                                                }
                                            });
                                    // Generate Json Schema
                                    String schemaContent = JSONSchemaUtils.toJSONSchema(info, false);
                                    if (isCanceled(currentUpdatedTime)) {
                                        return;
                                    }
                                    // Update file with the generated Json Schema
                                    updateFileContent(schemaContent, jsonSchemaFile);
                                } catch (Exception e) {
                                    LOGGER.error("Error while generating Quarkus Json Schema for the module '{}.", module.getName(), e);
                                } finally {
                                    updating = false;
                                }
                            });
                });
    }

    private boolean isCanceled(long currentUpdatedTime) {
        return updatedTime != currentUpdatedTime;
    }

}
