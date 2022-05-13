/*******************************************************************************
 * Copyright (c) 2019-2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.config.properties;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.utils.MavenArtifactUtil;

import java.io.File;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertProperties;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertPropertiesDuplicate;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.p;
import static org.eclipse.lsp4mp.commons.metadata.ItemMetadata.CONFIG_PHASE_BUILD_AND_RUN_TIME_FIXED;
import static org.eclipse.lsp4mp.commons.metadata.ItemMetadata.CONFIG_PHASE_BUILD_TIME;

/**
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/MicroProfileConfigPropertyTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/MicroProfileConfigPropertyTest.java</a>
 */
public class MicroProfileConfigPropertyTest extends MavenModuleImportingTestCase {
    public void testConfigQuickstartFromClasspath() throws Exception {
        Module module = createMavenModule("config-quickstart", new File("projects/maven/config-quickstart"));
        MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(myProject), DocumentFormat.PlainText);

        File f = MavenArtifactUtil.getArtifactFile(myProjectsManager.findProject(module).getLocalRepository(), new MavenId("io.quarkus:quarkus-core-deployment:1.1.0.Final"), "jar");
        assertNotNull("Test existing of quarkus-core-deployment*.jar", f);

        assertProperties(infoFromClasspath, 257 /* properties from JAR */ + //
                        18 /* properties from Java sources with ConfigProperty */ + //
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
                p(null, "greeting.method.name", "java.util.Optional<java.lang.String>", null, false,
                        "org.acme.config.GreetingMethodResource", null, "setName(Ljava/util/Optional;)V", 0, null),

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
        MicroProfileProjectInfo infoFromJavaSources = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.ONLY_SOURCES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(myProject), DocumentFormat.PlainText);

        assertProperties(infoFromJavaSources, 18 /* properties from Java sources with ConfigProperty */ + //
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
