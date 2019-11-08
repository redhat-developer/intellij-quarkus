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
import static com.redhat.quarkus.commons.ExtendedConfigDescriptionBuildItem.CONFIG_PHASE_RUN_TIME;

public class PSIQuarkusManagerConfigRootTest extends MavenImportingTestCase {
    public void testHibernateOrmResteasy() throws Exception {
        Module module = createMavenModule("hibernate-orm-resteasy", new File("projects/maven/hibernate-orm-resteasy"));
        List<ExtendedConfigDescriptionBuildItem> items = PSIQuarkusManager.INSTANCE.getConfigItems(module, QuarkusPropertiesScope.classpath, false);
        File f = MavenArtifactUtil.getArtifactFile(myProjectsManager.findProject(module).getLocalRepository(), new MavenId("io.quarkus:quarkus-hibernate-orm-deployment:0.19.1"), "jar");
        assertNotNull("Test existing of quarkus-hibernate-orm-deployment*.jar", f);
        assertProperties(items,

                // io.quarkus.hibernate.orm.deployment.HibernateOrmConfig
                p("quarkus-hibernate-orm", "quarkus.hibernate-orm.dialect", "java.util.Optional<java.lang.String>",
                        "The hibernate ORM dialect class name", f.getAbsolutePath(),
                        "io.quarkus.hibernate.orm.deployment.HibernateOrmConfig#dialect", CONFIG_PHASE_BUILD_TIME,
                        null));

    }

    public void testAllQuarkusExtensions() throws Exception {
        Module module = createMavenModule("all-quarkus-extensions", new File("projects/maven/all-quarkus-extensions"));
        List<ExtendedConfigDescriptionBuildItem> items = PSIQuarkusManager.INSTANCE.getConfigItems(module, QuarkusPropertiesScope.classpath, false);
        File keycloakJARFile = MavenArtifactUtil.getArtifactFile(myProjectsManager.findProject(module).getLocalRepository(), new MavenId("io.quarkus:quarkus-keycloak-deployment:0.21.1"), "jar");
        assertNotNull("Test existing of quarkus-keycloak-deployment*.jar", keycloakJARFile);
        File hibernateJARFile = MavenArtifactUtil.getArtifactFile(myProjectsManager.findProject(module).getLocalRepository(), new MavenId("io.quarkus:quarkus-hibernate-orm-deployment:0.21.1"), "jar");
        assertNotNull("Test existing of quarkus-hibernate-orm-deployment*.jar", hibernateJARFile);
        File undertowJARFile = MavenArtifactUtil.getArtifactFile(myProjectsManager.findProject(module).getLocalRepository(), new MavenId("io.quarkus:quarkus-undertow:0.21.1"), "jar");
        assertNotNull("Test existing of quarkus-undertow*.jar", undertowJARFile);
        File mongoJARFile = MavenArtifactUtil.getArtifactFile(myProjectsManager.findProject(module).getLocalRepository(), new MavenId("io.quarkus:quarkus-mongodb-client:0.21.1"), "jar");
        assertNotNull("Test existing of quarkus-mongodb-client*.jar", mongoJARFile);

        assertProperties(items,

                // Test with Map<String, String>
                // https://github.com/quarkusio/quarkus/blob/0.21/extensions/keycloak/deployment/src/main/java/io/quarkus/keycloak/KeycloakConfig.java#L308
                p("quarkus-keycloak", "quarkus.keycloak.credentials.jwt.{*}", "java.lang.String",
                        "The settings for client authentication with signed JWT", keycloakJARFile.getAbsolutePath(),
                        "io.quarkus.keycloak.KeycloakConfig$KeycloakConfigCredentials#jwt", CONFIG_PHASE_BUILD_TIME,
                        null),

                // Test with Map<String, Map<String, Map<String, String>>>
                // https://github.com/quarkusio/quarkus/blob/0.21/extensions/keycloak/deployment/src/main/java/io/quarkus/keycloak/KeycloakConfig.java#L469
                p("quarkus-keycloak", "quarkus.keycloak.policy-enforcer.paths.{*}.claim-information-point.{*}.{*}.{*}",
                        "java.lang.String", "", keycloakJARFile.getAbsolutePath(),
                        "io.quarkus.keycloak.KeycloakConfig$KeycloakConfigPolicyEnforcer$ClaimInformationPointConfig#complexConfig",
                        CONFIG_PHASE_BUILD_TIME, null),

                // io.quarkus.hibernate.orm.deployment.HibernateOrmConfig
                p("quarkus-hibernate-orm", "quarkus.hibernate-orm.dialect", "java.util.Optional<java.lang.String>",
                        "The hibernate ORM dialect class name", hibernateJARFile.getAbsolutePath(),
                        "io.quarkus.hibernate.orm.deployment.HibernateOrmConfig#dialect", CONFIG_PHASE_BUILD_TIME,
                        null),

                // test with extension name
                p("quarkus-undertow", "quarkus.http.ssl.certificate.file", "java.util.Optional<java.nio.file.Path>",
                        "The file path to a server certificate or certificate chain in PEM format.",
                        undertowJARFile.getAbsolutePath(),
                        "io.quarkus.runtime.configuration.ssl.CertificateConfig#file", CONFIG_PHASE_RUN_TIME, null),

                p("quarkus-mongodb-client", "quarkus.mongodb.credentials.auth-mechanism-properties.{*}",
                        "java.lang.String", "Allows passing authentication mechanism properties.",
                        mongoJARFile.getAbsolutePath(),
                        "io.quarkus.mongodb.runtime.CredentialConfig#authMechanismProperties", CONFIG_PHASE_RUN_TIME,
                        null));

    }

}
