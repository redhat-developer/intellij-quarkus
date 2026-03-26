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
package com.redhat.devtools.intellij.qute.psi.template.project;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.KeyedLazyInstanceEP;
import com.redhat.devtools.intellij.qute.psi.internal.AbstractQuteExtensionPointRegistry;
import com.redhat.qute.commons.ProjectFeature;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry to handle instances of {@link IProjectFeatureProvider}
 *
 * @author Angelo ZERR
 */
public class ProjectFeatureProviderRegistry extends AbstractQuteExtensionPointRegistry<IProjectFeatureProvider, ProjectFeatureProviderRegistry.ProjectFeatureProviderBean> {

    private static final Logger LOGGER = Logger.getLogger(ProjectFeatureProviderRegistry.class.getName());

    private static final ExtensionPointName<ProjectFeatureProviderRegistry.ProjectFeatureProviderBean> PROJECT_FEATURE_PROVIDERS_EXTENSION_POINT_ID = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.qute.projectFeatureProvider");

    private static final ProjectFeatureProviderRegistry INSTANCE = new ProjectFeatureProviderRegistry();

    private ProjectFeatureProviderRegistry() {
        super();
    }

    public static ProjectFeatureProviderRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public ExtensionPointName getProviderExtensionId() {
        return PROJECT_FEATURE_PROVIDERS_EXTENSION_POINT_ID;
    }

    /**
     * Returns the project feature list for the given java project.
     *
     * @param javaProject the java project.
     * @return the project feature list for the given java project.
     **/
    public @NotNull Set<ProjectFeature> getProjectFeatures(@NotNull Module javaProject) {
        Set<ProjectFeature> projectFeatures = new HashSet<>();
        for (IProjectFeatureProvider provider : super.getProviders()) {
            try {
                provider.collectProjectFeatures(javaProject, projectFeatures);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error while collecting project feature with the provider '"
                        + provider.getClass().getName() + "'.", e);
            }
        }
        return projectFeatures;
    }

    public static class ProjectFeatureProviderBean extends KeyedLazyInstanceEP<IProjectFeatureProvider> {

    }
}
