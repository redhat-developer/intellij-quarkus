/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.maven;

import com.intellij.facet.FacetType;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.quarkus.facet.QuarkusFacet;
import com.redhat.devtools.intellij.qute.facet.QuteFacet;
import com.redhat.devtools.intellij.qute.facet.QuteFacetConfiguration;
import com.redhat.devtools.intellij.qute.facet.QuteFacetType;
import org.jetbrains.idea.maven.importing.FacetImporter;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;

import java.util.List;
import java.util.Map;

public class QuteFacetMavenImporter extends FacetImporter<QuteFacet, QuteFacetConfiguration, QuteFacetType> {
    protected QuteFacetMavenImporter() {
        super("io.quarkus", "quarkus-maven-plugin", FacetType.findInstance(QuteFacetType.class));
    }

    @Override
    public boolean isApplicable(MavenProject mavenProject) {
        return !mavenProject.isAggregator() &&
               !mavenProject.findDependencies("io.quarkus.qute", "qute-core").isEmpty();
    }

    @Override
    protected void setupFacet(QuteFacet quarkusFacet, MavenProject mavenProject) {

    }

    @Override
    protected void reimportFacet(IdeModifiableModelsProvider ideModifiableModelsProvider, Module module, MavenRootModelAdapter mavenRootModelAdapter, QuteFacet quteFacet, MavenProjectsTree mavenProjectsTree, MavenProject mavenProject, MavenProjectChanges mavenProjectChanges, Map<MavenProject, String> map, List<MavenProjectsProcessorTask> list) {

    }
}
