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
package com.redhat.devtools.intellij;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Couple;
import com.redhat.devtools.intellij.quarkus.buildtool.gradle.AbstractGradleToolDelegate;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.importing.GradleImportingTestCase;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public abstract class GradleTestCase extends GradleImportingTestCase {
    protected static final String DOC = "Class name of the Hibernate ORM dialect. The complete list of bundled dialects is available in the\n" +
            "https://docs.jboss.org/hibernate/stable/orm/javadocs/org/hibernate/dialect/package-summary.html[Hibernate ORM JavaDoc].\n" +
            "\n" +
            "[NOTE]\n" +
            "====\n" +
            "Not all the dialects are supported in GraalVM native executables: we currently provide driver extensions for PostgreSQL,\n" +
            "MariaDB, Microsoft SQL Server and H2.\n" +
            "====\n" +
            "\n" +
            "@asciidoclet";

    public static File getDependency(String path, String groupId, String artifactId, String version) {
        try (ProjectConnection connection = GradleConnector.newConnector().forProjectDirectory(new File(path)).connect()) {
            Optional<IdeaSingleEntryLibraryDependency> dependency = (Optional<IdeaSingleEntryLibraryDependency>) connection.getModel(IdeaProject.class).getModules().stream().flatMap(module -> module.getDependencies().stream()).
                    filter(dep -> dep instanceof IdeaSingleEntryLibraryDependency).
                    map(dep -> (IdeaSingleEntryLibraryDependency) dep).
                    filter(library -> isCoordinateSame((IdeaSingleEntryLibraryDependency) library, groupId, artifactId, version)).findFirst();
            if (dependency.isPresent()) {
                return dependency.get().getFile();
            } else {
                return AbstractGradleToolDelegate.getDeploymentFile(groupId + ":" + artifactId + ":" + version);
            }
        }
    }

    private static boolean isCoordinateSame(IdeaSingleEntryLibraryDependency library, String groupId, String artifactId, String version) {
        return library.getGradleModuleVersion().getGroup().equals(groupId) &&
                library.getGradleModuleVersion().getName().equals(artifactId) &&
                library.getGradleModuleVersion().getVersion().equals(version);
    }

    @NotNull
    @Override
    protected String injectRepo(String config) {
        return config;
    }

    protected File getMavenRepository() {
        return new File(System.getProperty("user.home") + File.separatorChar + ".m2" + File.separator + "repository");
    }

    @Parameterized.Parameters(name = "{index}: with Gradle-{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{{"8.6"}});
    }

    protected String getJavaFileUri(String path, Module module) {
        return new File(ModuleUtilCore.getModuleDirPath(module), path).toURI().toString();
    }

    @Override
    protected void handleDeprecationError(Couple<String> errorInfo) {
        //Don't fail the test -yet- if Gradle is complaining about deprecated features
    }
}
