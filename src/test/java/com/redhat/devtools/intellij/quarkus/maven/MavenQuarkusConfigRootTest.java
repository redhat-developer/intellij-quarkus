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
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.p;
import static com.redhat.microprofile.commons.metadata.ItemMetadata.CONFIG_PHASE_BUILD_TIME;
import static com.redhat.microprofile.commons.metadata.ItemMetadata.CONFIG_PHASE_RUN_TIME;

/**
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/QuarkusConfigRootTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/QuarkusConfigRootTest.java</a>
 */
public class MavenQuarkusConfigRootTest extends MavenImportingTestCase {
    public void testHibernateOrmResteasy() throws Exception {
        Module module = createMavenModule("hibernate-orm-resteasy", new File("projects/maven/hibernate-orm-resteasy"));
        MicroProfileProjectInfo info = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtils.getInstance(), DocumentFormat.PlainText);
        File f = MavenArtifactUtil.getArtifactFile(myProjectsManager.findProject(module).getLocalRepository(), new MavenId("io.quarkus:quarkus-hibernate-orm-deployment:0.19.1"), "jar");
        assertNotNull("Test existing of quarkus-hibernate-orm-deployment*.jar", f);
        assertProperties(info,

                // io.quarkus.hibernate.orm.deployment.HibernateOrmConfig
                p("quarkus-hibernate-orm", "quarkus.hibernate-orm.dialect", "java.util.Optional<java.lang.String>",
                        "The hibernate ORM dialect class name", true,
                        "io.quarkus.hibernate.orm.deployment.HibernateOrmConfig", "dialect", null,
                        CONFIG_PHASE_BUILD_TIME, null));
    }

    public void testAllQuarkusExtensions() throws Exception {
        Module module = createMavenModule("all-quarkus-extensions", new File("projects/maven/all-quarkus-extensions"));
        MicroProfileProjectInfo info = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtils.getInstance(), DocumentFormat.PlainText);
        File keycloakJARFile = MavenArtifactUtil.getArtifactFile(myProjectsManager.findProject(module).getLocalRepository(), new MavenId("io.quarkus:quarkus-keycloak-deployment:0.21.1"), "jar");
        assertNotNull("Test existing of quarkus-keycloak-deployment*.jar", keycloakJARFile);
        File hibernateJARFile = MavenArtifactUtil.getArtifactFile(myProjectsManager.findProject(module).getLocalRepository(), new MavenId("io.quarkus:quarkus-hibernate-orm-deployment:0.21.1"), "jar");
        assertNotNull("Test existing of quarkus-hibernate-orm-deployment*.jar", hibernateJARFile);
        File undertowJARFile = MavenArtifactUtil.getArtifactFile(myProjectsManager.findProject(module).getLocalRepository(), new MavenId("io.quarkus:quarkus-undertow:0.21.1"), "jar");
        assertNotNull("Test existing of quarkus-undertow*.jar", undertowJARFile);
        File mongoJARFile = MavenArtifactUtil.getArtifactFile(myProjectsManager.findProject(module).getLocalRepository(), new MavenId("io.quarkus:quarkus-mongodb-client:0.21.1"), "jar");
        assertNotNull("Test existing of quarkus-mongodb-client*.jar", mongoJARFile);

        assertProperties(info,

                // Test with Map<String, String>
                // https://github.com/quarkusio/quarkus/blob/0.21/extensions/keycloak/deployment/src/main/java/io/quarkus/keycloak/KeycloakConfig.java#L308
                p("quarkus-keycloak", "quarkus.keycloak.credentials.jwt.{*}", "java.lang.String",
                        "The settings for client authentication with signed JWT", true,
                        "io.quarkus.keycloak.KeycloakConfig$KeycloakConfigCredentials", "jwt", null,
                        CONFIG_PHASE_BUILD_TIME, null),

                // Test with Map<String, Map<String, Map<String, String>>>
                // https://github.com/quarkusio/quarkus/blob/0.21/extensions/keycloak/deployment/src/main/java/io/quarkus/keycloak/KeycloakConfig.java#L469
                p("quarkus-keycloak", "quarkus.keycloak.policy-enforcer.paths.{*}.claim-information-point.{*}.{*}.{*}",
                        "java.lang.String", "", true,
                        "io.quarkus.keycloak.KeycloakConfig$KeycloakConfigPolicyEnforcer$ClaimInformationPointConfig",
                        "complexConfig", null, CONFIG_PHASE_BUILD_TIME, null),

                // io.quarkus.hibernate.orm.deployment.HibernateOrmConfig
                p("quarkus-hibernate-orm", "quarkus.hibernate-orm.dialect", "java.util.Optional<java.lang.String>",
                        "The hibernate ORM dialect class name", true,
                        "io.quarkus.hibernate.orm.deployment.HibernateOrmConfig", "dialect", null,
                        CONFIG_PHASE_BUILD_TIME, null),

                // test with extension name
                p("quarkus-undertow", "quarkus.http.ssl.certificate.file", "java.util.Optional<java.nio.file.Path>",
                        "The file path to a server certificate or certificate chain in PEM format.", true,
                        "io.quarkus.runtime.configuration.ssl.CertificateConfig", "file", null, CONFIG_PHASE_RUN_TIME,
                        null),

                p("quarkus-mongodb-client", "quarkus.mongodb.credentials.auth-mechanism-properties.{*}",
                        "java.lang.String", "Allows passing authentication mechanism properties.", true,
                        "io.quarkus.mongodb.runtime.CredentialConfig", "authMechanismProperties", null,
                        CONFIG_PHASE_RUN_TIME, null));
    }
}
