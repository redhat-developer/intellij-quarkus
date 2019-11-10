/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.module;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.quarkus.MavenImportingTestCase;
import com.redhat.devtools.intellij.quarkus.search.PSIQuarkusManager;
import com.redhat.quarkus.commons.ExtendedConfigDescriptionBuildItem;
import com.redhat.quarkus.commons.QuarkusPropertiesScope;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.utils.MavenArtifactUtil;

import java.io.File;
import java.util.List;

import static com.redhat.devtools.intellij.quarkus.module.QuarkusAssert.assertProperties;
import static com.redhat.devtools.intellij.quarkus.module.QuarkusAssert.p;
import static com.redhat.quarkus.commons.ExtendedConfigDescriptionBuildItem.CONFIG_PHASE_BUILD_TIME;

public class PSIQuarkusManagerTest extends MavenImportingTestCase {
    private static final String XML = "<project xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\" xmlns=\"http://maven.apache.org/POM/4.0.0\"" +
            "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
            "  <modelVersion>4.0.0</modelVersion>" +
            "<groupId>com.redhat.devtools.intellij.quarkus</groupId>" +
            "<artifactId>sample-core-deployment</artifactId>" +
            "<version>0.0.1-SNAPSHOT</version>" +
            "<dependencies>" +
            "<dependency>" +
            "<groupId>io.quarkus</groupId>" +
            "<artifactId>quarkus-core-deployment</artifactId>" +
            "<version>0.24.0</version>" +
            "</dependency>" +
            "</dependencies>" +
            "</project>";

    private Module module;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        module = super.createMavenModule("core-deployment", XML);
    }

    public void testQuarkusCoreDeploymentProperties() {
        List<ExtendedConfigDescriptionBuildItem> items = PSIQuarkusManager.INSTANCE.getConfigItems(module, QuarkusPropertiesScope.classpath, false);
        File quarkusCoreJARFile = MavenArtifactUtil.getArtifactFile(myProjectsManager.findProject(module).getLocalRepository(), new MavenId("io.quarkus:quarkus-core-deployment:0.24.0"), "jar");
        assertNotNull("Test existing of quarkus-mongodb-client*.jar", quarkusCoreJARFile);

        assertProperties(items,

                // io.quarkus.deployment.ApplicationConfig
                p("quarkus-core", "quarkus.application.name", "java.lang.String",
                        "The name of the application.\nIf not set, defaults to the name of the project.",
                        quarkusCoreJARFile.getAbsolutePath(), "io.quarkus.deployment.ApplicationConfig#name",
                        CONFIG_PHASE_BUILD_TIME, null),

                p("quarkus-core", "quarkus.application.version", "java.lang.String",
                        "The version of the application.\nIf not set, defaults to the version of the project",
                        quarkusCoreJARFile.getAbsolutePath(), "io.quarkus.deployment.ApplicationConfig#version",
                        CONFIG_PHASE_BUILD_TIME, null),

                // io.quarkus.deployment.JniProcessor$JniConfig
                p("quarkus-core", "quarkus.jni.enable", "boolean", "Enable JNI support.", quarkusCoreJARFile.getAbsolutePath(),
                        "io.quarkus.deployment.JniProcessor$JniConfig#enable", CONFIG_PHASE_BUILD_TIME, "false"),

                p("quarkus-core", "quarkus.jni.library-paths", "java.util.List<java.lang.String>",
                        "Paths of library to load.", quarkusCoreJARFile.getAbsolutePath(),
                        "io.quarkus.deployment.JniProcessor$JniConfig#libraryPaths", CONFIG_PHASE_BUILD_TIME, null),

                // io.quarkus.deployment.SslProcessor$SslConfig
                p("quarkus-core", "quarkus.ssl.native", "java.util.Optional<java.lang.Boolean>",
                        "Enable native SSL support.", quarkusCoreJARFile.getAbsolutePath(),
                        "io.quarkus.deployment.SslProcessor$SslConfig#native_", CONFIG_PHASE_BUILD_TIME, null),

                // io.quarkus.deployment.index.ApplicationArchiveBuildStep$IndexDependencyConfiguration
                // -> Map<String, IndexDependencyConfig>
                p("quarkus-core", "quarkus.index-dependency.{*}.classifier", "java.lang.String",
                        "The maven classifier of the artifact to index", quarkusCoreJARFile.getAbsolutePath(),
                        "io.quarkus.deployment.index.IndexDependencyConfig#classifier", CONFIG_PHASE_BUILD_TIME, null),
                p("quarkus-core", "quarkus.index-dependency.{*}.artifact-id", "java.lang.String",
                        "The maven artifactId of the artifact to index", quarkusCoreJARFile.getAbsolutePath(),
                        "io.quarkus.deployment.index.IndexDependencyConfig#artifactId", CONFIG_PHASE_BUILD_TIME, null),
                p("quarkus-core", "quarkus.index-dependency.{*}.group-id", "java.lang.String",
                        "The maven groupId of the artifact to index", quarkusCoreJARFile.getAbsolutePath(),
                        "io.quarkus.deployment.index.IndexDependencyConfig#groupId", CONFIG_PHASE_BUILD_TIME, null));


    }
}
