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
package com.redhat.microprofile.psi.quarkus;

import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.GradleTestCase;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertProperties;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.p;
import static org.eclipse.lsp4mp.commons.metadata.ItemMetadata.CONFIG_PHASE_BUILD_AND_RUN_TIME_FIXED;
import static org.eclipse.lsp4mp.commons.metadata.ItemMetadata.CONFIG_PHASE_BUILD_TIME;
import static org.eclipse.lsp4mp.commons.metadata.ItemMetadata.CONFIG_PHASE_RUN_TIME;

public class GradleQuarkusConfigRootAllQuarkusExtensionsTest extends GradleTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        FileUtils.copyDirectory(new File("projects/gradle/all-quarkus-extensions"), new File(getProjectPath()));
        importProject();
    }

    @Test
    public void testAllExtensions() throws Exception {
        MicroProfileProjectInfo info = PropertiesManager.getInstance().getMicroProfileProjectInfo(getModule("all-quarkus-extensions.main"), MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(), DocumentFormat.PlainText);
        File keycloakJARFile = getDependency(getProjectPath(), "io.quarkus" , "quarkus-keycloak-authorization", "1.0.1.Final");
        assertNotNull("Test existing of quarkus-keycloak-deployment*.jar", keycloakJARFile);
        File hibernateJARFile = getDependency(getProjectPath(), "io.quarkus", "quarkus-hibernate-orm-deployment", "1.0.1.Final");
        assertNotNull("Test existing of quarkus-hibernate-orm-deployment*.jar", hibernateJARFile);
        File vertxHTTPJARFile = getDependency(getProjectPath(), "io.quarkus" ,"quarkus-vertx-http", "1.0.1.Final");
        assertNotNull("Test existing of quarkus-undertow*.jar", vertxHTTPJARFile);
        File mongoJARFile = getDependency(getProjectPath(), "io.quarkus", "quarkus-mongodb-client", "1.0.1.Final");
        assertNotNull("Test existing of quarkus-mongodb-client*.jar", mongoJARFile);

        assertProperties(info,

                // Test with Map<String, Map<String, Map<String, String>>>
                // https://github.com/quarkusio/quarkus/blob/0.21/extensions/keycloak/deployment/src/main/java/io/quarkus/keycloak/KeycloakConfig.java#L469
                p("quarkus-keycloak-authorization", "quarkus.keycloak.policy-enforcer.paths.{*}.claim-information-point.{*}.{*}.{*}",
                        "java.lang.String", "", true,
                        "io.quarkus.keycloak.pep.KeycloakPolicyEnforcerConfig$KeycloakConfigPolicyEnforcer$ClaimInformationPointConfig",
                        "complexConfig", null, CONFIG_PHASE_BUILD_AND_RUN_TIME_FIXED, null),

                // io.quarkus.hibernate.orm.deployment.HibernateOrmConfig
                p("quarkus-hibernate-orm", "quarkus.hibernate-orm.dialect", "java.util.Optional<java.lang.String>",
                        DOC, true,
                        "io.quarkus.hibernate.orm.deployment.HibernateOrmConfig", "dialect", null,
                        CONFIG_PHASE_BUILD_TIME, null),

                // test with extension name
                p("quarkus-vertx-http", "quarkus.http.ssl.certificate.file", "java.util.Optional<java.nio.file.Path>",
                        "The file path to a server certificate or certificate chain in PEM format.", true,
                        "io.quarkus.vertx.http.runtime.CertificateConfig", "file", null, CONFIG_PHASE_RUN_TIME,
                        null),

                p("quarkus-mongodb-client", "quarkus.mongodb.credentials.auth-mechanism-properties.{*}",
                        "java.lang.String", "Allows passing authentication mechanism properties.", true,
                        "io.quarkus.mongodb.runtime.CredentialConfig", "authMechanismProperties", null,
                        CONFIG_PHASE_RUN_TIME, null));
    }
}
