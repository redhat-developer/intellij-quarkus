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
package com.redhat.devtools.intellij.quarkus.maven;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.quarkus.search.PropertiesManager;
import com.redhat.devtools.intellij.quarkus.search.PsiUtils;
import com.redhat.microprofile.commons.ClasspathKind;
import com.redhat.microprofile.commons.DocumentFormat;
import com.redhat.microprofile.commons.MicroProfileProjectInfo;
import com.redhat.microprofile.commons.MicroProfilePropertiesScope;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.utils.MavenArtifactUtil;

import java.io.File;

import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.assertProperties;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.assertPropertiesDuplicate;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.p;
import static com.redhat.microprofile.commons.metadata.ItemMetadata.CONFIG_PHASE_BUILD_TIME;

/**
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/MicroProfileConfigPropertyTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/MicroProfileConfigPropertyTest.java</a>
 */
public class MavenMicroProfileConfigPropertyTest extends MavenImportingTestCase {
    public void testConfigQuickstartFromClasspath() throws Exception {
        Module module = createMavenModule("config-quickstart", new File("projects/maven/config-quickstart"));
        MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtils.getInstance(), DocumentFormat.PlainText);

        File f = MavenArtifactUtil.getArtifactFile(myProjectsManager.findProject(module).getLocalRepository(), new MavenId("io.quarkus:quarkus-core-deployment:1.1.0.Final"), "jar");
        assertNotNull("Test existing of quarkus-core-deployment*.jar", f);

        assertProperties(infoFromClasspath, 201 /* properties from JAR */ + //
                        3 /* properties from Java sources with ConfigProperty */ + //
                        2 /* properties from Java sources with ConfigRoot */,

                // io.quarkus.deployment.ApplicationConfig
                p("quarkus-core", "quarkus.application.name", "java.util.Optional<java.lang.String>",
                        "The name of the application.\nIf not set, defaults to the name of the project.", true,
                        "io.quarkus.deployment.ApplicationConfig", "name", null, CONFIG_PHASE_BUILD_TIME, null),

                p("quarkus-core", "quarkus.application.version", "java.util.Optional<java.lang.String>",
                        "The version of the application.\nIf not set, defaults to the version of the project", true,
                        "io.quarkus.deployment.ApplicationConfig", "version", null, CONFIG_PHASE_BUILD_TIME, null),

                // GreetingResource
                // @ConfigProperty(name = "greeting.message")
                // String message;
                p(null, "greeting.message", "java.lang.String", null, false, "org.acme.config.GreetingResource",
                        "message", null, 0, null),

                // @ConfigProperty(name = "greeting.suffix" , defaultValue="!")
                // String suffix;
                p(null, "greeting.suffix", "java.lang.String", null, false, "org.acme.config.GreetingResource",
                        "suffix", null, 0, "!"),

                // @ConfigProperty(name = "greeting.name")
                // Optional<String> name;
                p(null, "greeting.name", "java.util.Optional<java.lang.String>", null, false, "org.acme.config.GreetingResource", "name",
                        null, 0, null),

                // @ConfigRoot / CustomExtensionConfig / property1
                p(null, "quarkus.custom-extension.property1", "java.lang.String", null, false,
                        "org.acme.config.CustomExtensionConfig", "property1", null, CONFIG_PHASE_BUILD_TIME, null),

                // @ConfigRoot / CustomExtensionConfig / property2
                p(null, "quarkus.custom-extension.property2", "java.lang.Integer", null, false,
                        "org.acme.config.CustomExtensionConfig", "property2", null, CONFIG_PHASE_BUILD_TIME, null));

        assertPropertiesDuplicate(infoFromClasspath);
    }

    public void testConfigQuickstartFromJavaSources() throws Exception {
        Module module = createMavenModule("config-quickstart", new File("projects/maven/config-quickstart"));
        MicroProfileProjectInfo infoFromJavaSources = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.ONLY_SOURCES, ClasspathKind.SRC, PsiUtils.getInstance(), DocumentFormat.PlainText);

        assertProperties(infoFromJavaSources, 3 /* properties from Java sources with ConfigProperty */ + //
                        2 /* properties from Java sources with ConfigRoot */,

                // GreetingResource
                // @ConfigProperty(name = "greeting.message")
                // String message;
                p(null, "greeting.message", "java.lang.String", null, false, "org.acme.config.GreetingResource",
                        "message", null, 0, null),

                // @ConfigProperty(name = "greeting.suffix" , defaultValue="!")
                // String suffix;
                p(null, "greeting.suffix", "java.lang.String", null, false, "org.acme.config.GreetingResource",
                        "suffix", null, 0, "!"),

                // @ConfigProperty(name = "greeting.name")
                // Optional<String> name;
                p(null, "greeting.name", "java.util.Optional<java.lang.String>", null, false, "org.acme.config.GreetingResource", "name",
                        null, 0, null),

                // @ConfigRoot / CustomExtensionConfig / property1
                p(null, "quarkus.custom-extension.property1", "java.lang.String", null, false,
                        "org.acme.config.CustomExtensionConfig", "property1", null, CONFIG_PHASE_BUILD_TIME, null),

                // @ConfigRoot / CustomExtensionConfig / property2
                p(null, "quarkus.custom-extension.property2", "java.lang.Integer", null, false,
                        "org.acme.config.CustomExtensionConfig", "property2", null, CONFIG_PHASE_BUILD_TIME, null));

        assertPropertiesDuplicate(infoFromJavaSources);
    }
}
