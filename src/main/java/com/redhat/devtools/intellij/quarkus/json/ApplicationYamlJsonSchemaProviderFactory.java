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
package com.redhat.devtools.intellij.quarkus.json;

import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Json schema factory used to provide support for Quarkus application.yaml
 */
public class ApplicationYamlJsonSchemaProviderFactory implements JsonSchemaProviderFactory {

    @NotNull
    @Override
    public List<JsonSchemaFileProvider> getProviders(@NotNull Project project) {
        List<JsonSchemaFileProvider> providers = new ArrayList<>();
        providers.addAll(ApplicationYamlJsonSchemaManager.getInstance(project).getProviders());
        return providers;
    }
}
