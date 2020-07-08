/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.json;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.SchemaType;
import com.redhat.devtools.intellij.quarkus.QuarkusProjectService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuarkusJsonSchemaProvider implements JsonSchemaFileProvider {
    private final Module module;

    public QuarkusJsonSchemaProvider(Module module) {
        this.module = module;
    }

    @Override
    public boolean isAvailable(@NotNull VirtualFile file) {
        return isApplicationYAMLFile(file) && module.equals(ModuleUtilCore.findModuleForFile(file, module.getProject()));
    }

    private boolean isApplicationYAMLFile(VirtualFile file) {
        return file.getName().equals("application.yaml") || file.getName().equals("application.yml");
    }

    @NotNull
    @Override
    public String getName() {
        return "Quarkus";
    }


    @Nullable
    @Override
    public VirtualFile getSchemaFile() {
        return QuarkusProjectService.getInstance(module.getProject()).getSchema(module);
    }

    @NotNull
    @Override
    public SchemaType getSchemaType() {
        return SchemaType.schema;
    }
}
