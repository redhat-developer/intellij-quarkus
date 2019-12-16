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
package com.redhat.devtools.intellij.quarkus.gradle;

import com.redhat.devtools.intellij.quarkus.search.PSIQuarkusManager;
import com.redhat.quarkus.commons.ExtendedConfigDescriptionBuildItem;
import com.redhat.quarkus.commons.QuarkusPropertiesScope;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.redhat.devtools.intellij.quarkus.module.QuarkusAssert.assertProperties;
import static com.redhat.devtools.intellij.quarkus.module.QuarkusAssert.p;
import static com.redhat.quarkus.commons.ExtendedConfigDescriptionBuildItem.CONFIG_PHASE_BUILD_TIME;

public class GradlePSIQuarkusManagerTest extends GradleTestCase {
    private static final String CONFIG =
    "plugins {\n" +
    "    id 'java'\n" +
    "}\n" +

    "repositories {\n" +
    "    mavenLocal()\n" +
    "    mavenCentral()\n" +
    "}\n" +

    "dependencies {\n" +
    "    implementation 'io.quarkus:quarkus-core-deployment:1.0.1.Final'\n" +
    "}\n" +

    "group 'org.acme'\n" +
    "version '1.0.0-SNAPSHOT'\n" +

    "java {\n" +
    "    sourceCompatibility = JavaVersion.VERSION_1_8\n" +
    "    targetCompatibility = JavaVersion.VERSION_1_8\n" +
    "}";

    @Test
    public void testQuarkusCoreDeploymentProperties() throws IOException {
        importProject(CONFIG);
        List<ExtendedConfigDescriptionBuildItem> items = PSIQuarkusManager.INSTANCE.getConfigItems(getModule("project.main"), QuarkusPropertiesScope.classpath, false);
        File quarkusCoreJARFile = getDependency(getProjectPath(), "io.quarkus", "quarkus-core-deployment", "1.0.1.Final");
        assertNotNull("Test existing of quarkus-core-deployment.jar", quarkusCoreJARFile);

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
