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
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static com.redhat.devtools.intellij.quarkus.module.QuarkusAssert.assertProperties;
import static com.redhat.devtools.intellij.quarkus.module.QuarkusAssert.p;
import static com.redhat.quarkus.commons.ExtendedConfigDescriptionBuildItem.CONFIG_PHASE_BUILD_AND_RUN_TIME_FIXED;
import static com.redhat.quarkus.commons.ExtendedConfigDescriptionBuildItem.CONFIG_PHASE_BUILD_TIME;
import static com.redhat.quarkus.commons.ExtendedConfigDescriptionBuildItem.CONFIG_PHASE_RUN_TIME;

public class GradlePSIQuarkusManagerConfigRootAllQuarkusExtensionsTest extends GradleTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        FileUtils.copyDirectory(new File("projects/gradle/all-quarkus-extensions"), new File(getProjectPath()));
        importProject();
    }

    @Test
    public void testAllExtensions() throws Exception {
        List<ExtendedConfigDescriptionBuildItem> items = PSIQuarkusManager.INSTANCE.getConfigItems(getModule("all-quarkus-extensions.main"), QuarkusPropertiesScope.classpath, false);
        File keycloakJARFile = getDependency(getProjectPath(), "io.quarkus" , "quarkus-keycloak-authorization", "1.0.1.Final");
        assertNotNull("Test existing of quarkus-keycloak-deployment*.jar", keycloakJARFile);
        File hibernateJARFile = getDependency(getProjectPath(), "io.quarkus", "quarkus-hibernate-orm-deployment", "1.0.1.Final");
        assertNotNull("Test existing of quarkus-hibernate-orm-deployment*.jar", hibernateJARFile);
        File vertxHTTPJARFile = getDependency(getProjectPath(), "io.quarkus" ,"quarkus-vertx-http", "1.0.1.Final");
        assertNotNull("Test existing of quarkus-undertow*.jar", vertxHTTPJARFile);
        File mongoJARFile = getDependency(getProjectPath(), "io.quarkus", "quarkus-mongodb-client", "1.0.1.Final");
        assertNotNull("Test existing of quarkus-mongodb-client*.jar", mongoJARFile);

        assertProperties(items,

                // Test with Map<String, Map<String, Map<String, String>>>
                // https://github.com/quarkusio/quarkus/blob/0.21/extensions/keycloak/deployment/src/main/java/io/quarkus/keycloak/KeycloakConfig.java#L469
                p("quarkus-keycloak-authorization", "quarkus.keycloak.policy-enforcer.paths.{*}.claim-information-point.{*}.{*}.{*}",
                        "java.lang.String", "", keycloakJARFile.getAbsolutePath(),
                        "io.quarkus.keycloak.pep.KeycloakPolicyEnforcerConfig$KeycloakConfigPolicyEnforcer$ClaimInformationPointConfig#complexConfig",
                        CONFIG_PHASE_BUILD_AND_RUN_TIME_FIXED, null),

                // io.quarkus.hibernate.orm.deployment.HibernateOrmConfig
                p("quarkus-hibernate-orm", "quarkus.hibernate-orm.dialect", "java.util.Optional<java.lang.String>",
                        DOC, hibernateJARFile.getAbsolutePath(),
                        "io.quarkus.hibernate.orm.deployment.HibernateOrmConfig#dialect", CONFIG_PHASE_BUILD_TIME,
                        null),

                // test with extension name
                p("quarkus-vertx-http", "quarkus.http.ssl.certificate.file", "java.util.Optional<java.nio.file.Path>",
                        "The file path to a server certificate or certificate chain in PEM format.",
                        vertxHTTPJARFile.getAbsolutePath(),
                        "io.quarkus.vertx.http.runtime.CertificateConfig#file", CONFIG_PHASE_RUN_TIME, null),

                p("quarkus-mongodb-client", "quarkus.mongodb.credentials.auth-mechanism-properties.{*}",
                        "java.lang.String", "Allows passing authentication mechanism properties.",
                        mongoJARFile.getAbsolutePath(),
                        "io.quarkus.mongodb.runtime.CredentialConfig#authMechanismProperties", CONFIG_PHASE_RUN_TIME,
                        null));

    }

}
