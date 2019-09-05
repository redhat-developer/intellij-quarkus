package com.redhat.devtools.intellij.quarkus.maven;

import com.intellij.facet.FacetType;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.quarkus.facet.QuarkusFacet;
import com.redhat.devtools.intellij.quarkus.facet.QuarkusFacetConfiguration;
import com.redhat.devtools.intellij.quarkus.facet.QuarkusFacetType;
import org.jetbrains.idea.maven.importing.FacetImporter;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;

import java.util.List;
import java.util.Map;

public class QuarkusFacetMavenImporter extends FacetImporter<QuarkusFacet, QuarkusFacetConfiguration, QuarkusFacetType> {
    protected QuarkusFacetMavenImporter() {
        super("io.quarkus", "quarkus-maven-plugin", FacetType.findInstance(QuarkusFacetType.class));
    }

    @Override
    public boolean isApplicable(MavenProject mavenProject) {
        return !mavenProject.isAggregator() &&
               !mavenProject.findDependencies("io.quarkus", "quarkus-core").isEmpty();
    }

    @Override
    protected void setupFacet(QuarkusFacet quarkusFacet, MavenProject mavenProject) {

    }

    @Override
    protected void reimportFacet(IdeModifiableModelsProvider ideModifiableModelsProvider, Module module, MavenRootModelAdapter mavenRootModelAdapter, QuarkusFacet quarkusFacet, MavenProjectsTree mavenProjectsTree, MavenProject mavenProject, MavenProjectChanges mavenProjectChanges, Map<MavenProject, String> map, List<MavenProjectsProcessorTask> list) {

    }
}
