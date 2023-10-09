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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.eclipse.lsp4mp.commons.metadata.ItemHint;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;
import org.junit.Assert;

import java.io.File;
import java.util.Optional;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertHints;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertHintsDuplicate;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertProperties;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.h;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.p;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.vh;
import static org.eclipse.lsp4mp.commons.metadata.ItemMetadata.CONFIG_PHASE_BUILD_AND_RUN_TIME_FIXED;
import static org.eclipse.lsp4mp.commons.metadata.ItemMetadata.CONFIG_PHASE_BUILD_TIME;
import static org.eclipse.lsp4mp.commons.metadata.ItemMetadata.CONFIG_PHASE_RUN_TIME;

/**
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/QuarkusConfigRootTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/QuarkusConfigRootTest.java</a>
 */
public class MavenQuarkusConfigRootTest extends QuarkusMavenModuleImportingTestCase {

    public void testHibernateOrmResteasy() throws Exception {
        Module module = loadMavenProject(QuarkusMavenProjectName.hibernate_orm_resteasy, true);
        MicroProfileProjectInfo info = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(myProject), DocumentFormat.PlainText, new EmptyProgressIndicator());
        assertProperties(info,

                // io.quarkus.hibernate.orm.deployment.HibernateOrmConfig
                p("quarkus-hibernate-orm", "quarkus.hibernate-orm.dialect", "java.util.Optional<java.lang.String>",
                        "Class name of the Hibernate ORM dialect. The complete list of bundled dialects is available in the\n" //
                                + "https://docs.jboss.org/hibernate/stable/orm/javadocs/org/hibernate/dialect/package-summary.html[Hibernate ORM JavaDoc].\n" //
                                + "\n" + //
                                "[NOTE]\n" + //
                                "====\n" //
                                + "Not all the dialects are supported in GraalVM native executables: we currently provide driver extensions for PostgreSQL,\n" //
                                + "MariaDB, Microsoft SQL Server and H2.\n" + //
                                "====\n" + //
                                "\n" + //
                                "@asciidoclet",
                        true, "io.quarkus.hibernate.orm.deployment.HibernateOrmConfig", "dialect", null,
                        CONFIG_PHASE_BUILD_TIME, null));
    }

    public void testAllQuarkusExtensions() throws Exception {
        Module module = loadMavenProject(QuarkusMavenProjectName.all_quarkus_extensions, true);
        MicroProfileProjectInfo info = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(myProject), DocumentFormat.PlainText, new EmptyProgressIndicator());
        assertProperties(info,

                p("quarkus-keycloak-authorization", "quarkus.keycloak.policy-enforcer.paths.{*}.name",
                        "java.util.Optional<java.lang.String>",
                        "The name of a resource on the server that is to be associated with a given path", true,
                        "io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerConfig$KeycloakConfigPolicyEnforcer$PathConfig",
                        "name", null, CONFIG_PHASE_BUILD_AND_RUN_TIME_FIXED, null),

                p("quarkus-keycloak-authorization", "quarkus.keycloak.policy-enforcer.paths.{*}.methods.{*}.method",
                        "java.lang.String", "The name of the HTTP method", true,
                        "io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerConfig$KeycloakConfigPolicyEnforcer$MethodConfig",
                        "method", null, CONFIG_PHASE_BUILD_AND_RUN_TIME_FIXED, null),

                p("quarkus-hibernate-orm", "quarkus.hibernate-orm.dialect", "java.util.Optional<java.lang.String>",
                        "Class name of the Hibernate ORM dialect. The complete list of bundled dialects is available in the\n" //
                                + "https://docs.jboss.org/hibernate/stable/orm/javadocs/org/hibernate/dialect/package-summary.html[Hibernate ORM JavaDoc].\n" //
                                + "\n" + //
                                "[NOTE]\n" + //
                                "====\n" //
                                + "Not all the dialects are supported in GraalVM native executables: we currently provide driver extensions for PostgreSQL,\n" //
                                + "MariaDB, Microsoft SQL Server and H2.\n" + //
                                "====\n" + //
                                "\n" + //
                                "@asciidoclet",
                        true, "io.quarkus.hibernate.orm.deployment.HibernateOrmConfig", "dialect", null,
                        CONFIG_PHASE_BUILD_TIME, null),

                p("quarkus-vertx-http", "quarkus.http.ssl.certificate.file", "java.util.Optional<java.nio.file.Path>",
                        "The file path to a server certificate or certificate chain in PEM format.", true,
                        "io.quarkus.vertx.http.runtime.CertificateConfig", "file", null, CONFIG_PHASE_RUN_TIME, null),

                p("quarkus-mongodb-client", "quarkus.mongodb.credentials.auth-mechanism-properties.{*}",
                        "java.lang.String", "Allows passing authentication mechanism properties.", true,
                        "io.quarkus.mongodb.runtime.CredentialConfig", "authMechanismProperties", null,
                        CONFIG_PHASE_RUN_TIME, null),

                // test with java.util.Optional enumeration
                p("quarkus-agroal", "quarkus.datasource.jdbc.transaction-isolation-level",
                        "java.util.Optional<io.agroal.api.configuration.AgroalConnectionFactoryConfiguration.TransactionIsolation>",
                        "The transaction isolation level.", true, "io.quarkus.agroal.runtime.DataSourceJdbcRuntimeConfig",
                        "transactionIsolationLevel", null, CONFIG_PHASE_RUN_TIME, null),

                // test with enumeration
                p("quarkus-core", "quarkus.log.console.async.overflow",
                        "org.jboss.logmanager.handlers.AsyncHandler.OverflowAction",
                        "Determine whether to block the publisher (rather than drop the message) when the queue is full",
                        true, "io.quarkus.runtime.logging.AsyncConfig", "overflow", null, CONFIG_PHASE_RUN_TIME,
                        "block"), //

                // test with quarkus.smallrye-openapi.path (download dependencies from
                // deployment artifact quarkus.smallrye-openapi)
                p("quarkus-smallrye-openapi-common", "quarkus.smallrye-openapi.path", "java.lang.String",
                        "The path at which to register the OpenAPI Servlet.", true,
                        "io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig", "path", null,
                        CONFIG_PHASE_BUILD_TIME, "/openapi") //

        );

        // assertPropertiesDuplicate(info);

        assertHints(info,
                h("io.agroal.api.configuration.AgroalConnectionFactoryConfiguration.TransactionIsolation", null, true,
                        "io.agroal.api.configuration.AgroalConnectionFactoryConfiguration.TransactionIsolation", //
                        vh("UNDEFINED", null, null), //
                        vh("NONE", null, null), //
                        vh("READ_UNCOMMITTED", null, null), //
                        vh("READ_COMMITTED", null, null), //
                        vh("REPEATABLE_READ", null, null), //
                        vh("SERIALIZABLE", null, null)), //

                h("org.jboss.logmanager.handlers.AsyncHandler.OverflowAction", null, true,
                        "org.jboss.logmanager.handlers.AsyncHandler.OverflowAction", //
                        vh("BLOCK", null, null), //
                        vh("DISCARD", null, null)) //
        );

        assertHintsDuplicate(info);

        // Check get enum values from project info

        // for Optional Java enum
        Optional<ItemMetadata> metadata = getItemMetadata("quarkus.datasource.transaction-isolation-level", info);
        Assert.assertTrue("Check existing of quarkus.datasource.transaction-isolation-level", metadata.isPresent());
        ItemHint hint = info.getHint(metadata.get());
        Assert.assertNotNull("Check existing of hint for quarkus.datasource.transaction-isolation-level", hint);
        Assert.assertNotNull("Check existing of values hint for quarkus.datasource.transaction-isolation-level",
                hint.getValues());
        Assert.assertFalse("Check has values hint for quarkus.datasource.transaction-isolation-level",
                hint.getValues().isEmpty());

        // for Java enum
        metadata = getItemMetadata("quarkus.log.console.async.overflow", info);
        Assert.assertTrue("Check existing of quarkus.log.console.async.overflow", metadata.isPresent());
        hint = info.getHint(metadata.get());
        Assert.assertNotNull("Check existing of hint for quarkus.log.console.async.overflow", hint);
        Assert.assertNotNull("Check existing of values hint for quarkus.log.console.async.overflow", hint.getValues());
        Assert.assertFalse("Check has values hint for quarkus.log.console.async.overflow", hint.getValues().isEmpty());
    }

    private static Optional<ItemMetadata> getItemMetadata(String propertyName, MicroProfileProjectInfo info) {
        return info.getProperties().stream().filter(completion -> {
            return propertyName.equals(completion.getName());
        }).findFirst();
    }

}
