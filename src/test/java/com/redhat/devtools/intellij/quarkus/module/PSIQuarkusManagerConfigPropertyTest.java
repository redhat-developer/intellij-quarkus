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

import java.io.File;
import java.util.List;

import static com.redhat.devtools.intellij.quarkus.module.QuarkusAssert.assertProperties;
import static com.redhat.devtools.intellij.quarkus.module.QuarkusAssert.p;

public class PSIQuarkusManagerConfigPropertyTest extends MavenImportingTestCase {
    public void testApplicationConfigurationFromClasspath() throws Exception {
        Module module = createMavenModule("application-configuration", new File("projects/maven/application-configuration"));
        List<ExtendedConfigDescriptionBuildItem> items = PSIQuarkusManager.INSTANCE.getConfigItems(module, QuarkusPropertiesScope.classpath, false);

        assertProperties(items, 69 /* properties from JAR */ + 3 /* properties from Java sources */,
                // GreetingResource
                // @ConfigProperty(name = "greeting.message")
                // String message;
                p(null, "greeting.message", "java.lang.String", null, "/application-configuration/src/main/java",
                        "org.acme.config.GreetingResource#message", 0, null),

                // @ConfigProperty(name = "greeting.suffix" , defaultValue="!")
                // String suffix;
                p(null, "greeting.suffix", "java.lang.String", null, "/application-configuration/src/main/java",
                        "org.acme.config.GreetingResource#suffix", 0, "!"),

                // @ConfigProperty(name = "greeting.name")
                // Optional<String> name;
                p(null, "greeting.name", "java.util.Optional<java.lang.String>", null, "/application-configuration/src/main/java",
                        "org.acme.config.GreetingResource#name", 0, null));
    }

    public void testApplicationConfigurationFromJavaSources() throws Exception {
        Module module = createMavenModule("application-configuration", new File("projects/maven/application-configuration"));
        List<ExtendedConfigDescriptionBuildItem> items = PSIQuarkusManager.INSTANCE.getConfigItems(module, QuarkusPropertiesScope.sources, false);

        assertProperties(items, 3 /* properties from Java sources */,
                // GreetingResource
                // @ConfigProperty(name = "greeting.message")
                // String message;
                p(null, "greeting.message", "java.lang.String", null, "/application-configuration/src/main/java",
                        "org.acme.config.GreetingResource#message", 0, null),

                // @ConfigProperty(name = "greeting.suffix" , defaultValue="!")
                // String suffix;
                p(null, "greeting.suffix", "java.lang.String", null, "/application-configuration/src/main/java",
                        "org.acme.config.GreetingResource#suffix", 0, "!"),

                // @ConfigProperty(name = "greeting.name")
                // Optional<String> name;
                p(null, "greeting.name", "java.util.Optional<java.lang.String>", null, "/application-configuration/src/main/java",
                        "org.acme.config.GreetingResource#name", 0, null));
    }
}
