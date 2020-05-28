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

import com.redhat.devtools.intellij.quarkus.search.PropertiesManager;
import com.redhat.devtools.intellij.quarkus.search.PsiUtilsImpl;
import com.redhat.microprofile.commons.ClasspathKind;
import com.redhat.microprofile.commons.DocumentFormat;
import com.redhat.microprofile.commons.MicroProfileProjectInfo;
import com.redhat.microprofile.commons.MicroProfilePropertiesScope;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.assertProperties;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.assertPropertiesDuplicate;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.p;
import static com.redhat.microprofile.commons.metadata.ItemMetadata.CONFIG_PHASE_BUILD_AND_RUN_TIME_FIXED;
import static com.redhat.microprofile.commons.metadata.ItemMetadata.CONFIG_PHASE_BUILD_TIME;

/**
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/MicroProfileConfigPropertyTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/MicroProfileConfigPropertyTest.java</a>
 */
@Ignore("Seems annotated parameters are not processed for Gradle")
public class GradleMicroProfileConfigPropertyTest extends GradleTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        FileUtils.copyDirectory(new File("projects/gradle/config-quickstart"), new File(getProjectPath()));
        importProject();
    }

    @Test
    public void testConfigQuickstartFromClasspath() throws Exception {
        MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(getModule("config-quickstart.main"), MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsImpl.getInstance(), DocumentFormat.PlainText);

        File f = getDependency(getProjectPath(),"io.quarkus", "quarkus-core-deployment", "1.1.0.Final");
        assertNotNull("Test existing of quarkus-core-deployment*.jar", f);

        assertProperties(infoFromClasspath, 254 /*257*/ /* properties from JAR */ + //
                        9 /* properties from Java sources with ConfigProperty */ + //
                        2 /* properties from Java sources with ConfigRoot */ + //
                        7 /* static properties from microprofile-context-propagation-api */,

                // io.quarkus.deployment.ApplicationConfig
                p("quarkus-core", "quarkus.application.name", "java.util.Optional<java.lang.String>",
                        "The name of the application.\nIf not set, defaults to the name of the project (except for tests where it is not set at all).",
                        true, "io.quarkus.runtime.ApplicationConfig", "name", null,
                        CONFIG_PHASE_BUILD_AND_RUN_TIME_FIXED, null),

                p("quarkus-core", "quarkus.application.version", "java.util.Optional<java.lang.String>",
                        "The version of the application.\nIf not set, defaults to the version of the project (except for tests where it is not set at all).",
                        true, "io.quarkus.runtime.ApplicationConfig", "version", null,
                        CONFIG_PHASE_BUILD_AND_RUN_TIME_FIXED, null),

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

                // GreetingConstructorResource(
                // @ConfigProperty(name = "greeting.constructor.message") String message,
                // @ConfigProperty(name = "greeting.constructor.suffix" , defaultValue="!")
                // String suffix,
                // @ConfigProperty(name = "greeting.constructor.name") Optional<String> name)
                p(null, "greeting.constructor.message", "java.lang.String", null, false,
                        "org.acme.config.GreetingConstructorResource", null,
                        "GreetingConstructorResource(Ljava/lang/String;Ljava/lang/String;Ljava/util/Optional;)V", 0, null),

                p(null, "greeting.constructor.suffix", "java.lang.String", null, false,
                        "org.acme.config.GreetingConstructorResource", null,
                        "GreetingConstructorResource(Ljava/lang/String;Ljava/lang/String;Ljava/util/Optional;)V", 0, "!"),

                p(null, "greeting.constructor.name", "java.util.Optional<java.lang.String>", null, false,
                        "org.acme.config.GreetingConstructorResource", null,
                        "GreetingConstructorResource(Ljava/lang/String;Ljava/lang/String;Ljava/util/Optional;)V", 0, null),

                // setMessage(@ConfigProperty(name = "greeting.method.message") String message)
                p(null, "greeting.method.message", "java.lang.String", null, false,
                        "org.acme.config.GreetingMethodResource", null, "setMessage(Ljava/lang/String;)V", 0, null),

                // setSuffix(@ConfigProperty(name = "greeting.method.suffix" , defaultValue="!")
                // String suffix)
                p(null, "greeting.method.suffix", "java.lang.String", null, false,
                        "org.acme.config.GreetingMethodResource", null, "setSuffix(Ljava/lang/String;)V", 0, "!"),

                // setName(@ConfigProperty(name = "greeting.method.name") Optional<String> name)
                p(null, "greeting.method.name", "java.util.Optional<java.lang.String>GradleMicroProfileConfigPropertyTest", null, false,
                        "org.acme.config.GreetingMethodResource", null, "setName(Ljava/util/Optional;)V", 0, null),

                // @ConfigRoot / CustomExtensionConfig / property1
                p(null, "quarkus.custom-extension.property1", "java.lang.String", null, false,
                        "org.acme.config.CustomExtensionConfig", "property1", null, CONFIG_PHASE_BUILD_TIME, null),

                // @ConfigRoot / CustomExtensionConfig / property2
                p(null, "quarkus.custom-extension.property2", "java.lang.Integer", null, false,
                        "org.acme.config.CustomExtensionConfig", "property2", null, CONFIG_PHASE_BUILD_TIME, null));

        assertPropertiesDuplicate(infoFromClasspath);
    }

    @Test
    public void testApplicationConfigurationFromJavaSources() throws Exception {
        MicroProfileProjectInfo infoFromJavaSources = PropertiesManager.getInstance().getMicroProfileProjectInfo(getModule("config-quickstart.main"), MicroProfilePropertiesScope.ONLY_SOURCES, ClasspathKind.SRC, PsiUtilsImpl.getInstance(), DocumentFormat.PlainText);

        assertProperties(infoFromJavaSources, 9 /* properties from Java sources with ConfigProperty */ + //
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

                // GreetingConstructorResource(
                // @ConfigProperty(name = "greeting.constructor.message") String message,
                // @ConfigProperty(name = "greeting.constructor.suffix" , defaultValue="!")
                // String suffix,
                // @ConfigProperty(name = "greeting.constructor.name") Optional<String> name)
                p(null, "greeting.constructor.message", "java.lang.String", null, false,
                        "org.acme.config.GreetingConstructorResource", null,
                        "GreetingConstructorResource(Ljava/lang/String;Ljava/lang/String;Ljava/util/Optional;)V", 0, null),

                p(null, "greeting.constructor.suffix", "java.lang.String", null, false,
                        "org.acme.config.GreetingConstructorResource", null,
                        "GreetingConstructorResource(Ljava/lang/String;Ljava/lang/String;Ljava/util/Optional;)V", 0, "!"),

                p(null, "greeting.constructor.name", "java.util.Optional<java.lang.String>", null, false,
                        "org.acme.config.GreetingConstructorResource", null,
                        "GreetingConstructorResource(Ljava/lang/String;Ljava/lang/String;Ljava/util/Optional;)V", 0, null),

                // setMessage(@ConfigProperty(name = "greeting.method.message") String message)
                p(null, "greeting.method.message", "java.lang.String", null, false,
                        "org.acme.config.GreetingMethodResource", null, "setMessage(Ljava/lang/String;)V", 0, null),

                // setSuffix(@ConfigProperty(name = "greeting.method.suffix" , defaultValue="!")
                // String suffix)
                p(null, "greeting.method.suffix", "java.lang.String", null, false,
                        "org.acme.config.GreetingMethodResource", null, "setSuffix(Ljava/lang/String;)V", 0, "!"),

                // setName(@ConfigProperty(name = "greeting.method.name") Optional<String> name)
                p(null, "greeting.method.name", "java.util.Optional<java.lang.String>", null, false,
                        "org.acme.config.GreetingMethodResource", null, "setName(Ljava/util/Optional;)V", 0, null),

                // @ConfigRoot / CustomExtensionConfig / property1
                p(null, "quarkus.custom-extension.property1", "java.lang.String", null, false,
                        "org.acme.config.CustomExtensionConfig", "property1", null, CONFIG_PHASE_BUILD_TIME, null),

                // @ConfigRoot / CustomExtensionConfig / property2
                p(null, "quarkus.custom-extension.property2", "java.lang.Integer", null, false,
                        "org.acme.config.CustomExtensionConfig", "property2", null, CONFIG_PHASE_BUILD_TIME, null));

        assertPropertiesDuplicate(infoFromJavaSources);
    }
}
