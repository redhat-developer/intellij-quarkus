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
package com.redhat.devtools.intellij.quarkus.gradle;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.redhat.devtools.intellij.quarkus.facet.QuarkusFacet;
import com.redhat.devtools.intellij.quarkus.facet.QuarkusFacetConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class QuarkusProjectDataService extends AbstractProjectDataService<LibraryDependencyData, Module> {
    @NotNull
    @Override
    public Key<LibraryDependencyData> getTargetDataKey() {
        return ProjectKeys.LIBRARY_DEPENDENCY;
    }

    @Override
    public void importData(@NotNull Collection<? extends DataNode<LibraryDependencyData>> toImport, @Nullable ProjectData projectData, @NotNull Project project, @NotNull IdeModifiableModelsProvider modelsProvider) {
        for(DataNode<LibraryDependencyData> libraryNode : toImport) {
            String name = libraryNode.getData().getExternalName();
            if (name.startsWith("io.quarkus:")) {
                Module module = modelsProvider.findIdeModule(libraryNode.getData().getOwnerModule());
                if (module != null) {
                    ensureQuarkusFacet(module, modelsProvider, libraryNode.getData().getOwner());
                }
            }

        }
    }

    private static void ensureQuarkusFacet(Module module, IdeModifiableModelsProvider modelsProvider, ProjectSystemId externalSystemId) {
        FacetType<QuarkusFacet, QuarkusFacetConfiguration> facetType = QuarkusFacet.getQuarkusFacetType();
        ModifiableFacetModel facetModel = modelsProvider.getModifiableFacetModel(module);
        QuarkusFacet facet = facetModel.getFacetByType(facetType.getId());
        if (facet == null) {
            facet = facetType.createFacet(module, facetType.getDefaultFacetName(), facetType.createDefaultConfiguration(), (Facet)null);
            facetModel.addFacet(facet, ExternalSystemApiUtil.toExternalSource(externalSystemId));
        }
    }
}
