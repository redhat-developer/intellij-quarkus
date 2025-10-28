/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.config.java;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.LSP4MPMavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileMavenProjectName;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.TestConfigSourceProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.IConfigSourceProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.providers.MicroProfileConfigSourceProvider;
import org.eclipse.lsp4j.Position;
import org.junit.Test;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.saveFile;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.*;

/**
 * JDT Quarkus manager test for hover in Java file.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/JavaHoverTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/JavaHoverTest.java</a>
 */
public class MicroProfileConfigJavaHoverTest extends LSP4MPMavenModuleImportingTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testConfigPropertyNameHover() throws Exception {
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.config_hover);
        String javaFileUri = getFileUri("src/main/java/org/acme/config/GreetingResource.java", javaProject);
        String propertiesFileUri = getFileUri("src/main/resources/META-INF/microprofile-config.properties", javaProject);

        saveFile(MicroProfileConfigSourceProvider.MICROPROFILE_CONFIG_PROPERTIES_FILE, //
                "greeting.message = hello\r\n" + //
                        "greeting.name = quarkus\r\n" + //
                        "greeting.number = 100",
                javaProject);
        // Position(14, 40) is the character after the | symbol:
        // @ConfigProperty(name = "greeting.mes|sage")
        assertJavaHover(new Position(14, 40), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()), h(
                "`greeting.message = hello` *in* [META-INF/microprofile-config.properties](" + propertiesFileUri + ")",
                14, 28, 44));

        // Test left edge
        // Position(14, 28) is the character after the | symbol:
        // @ConfigProperty(name = "|greeting.message")
        assertJavaHover(new Position(14, 28), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()), h(
                "`greeting.message = hello` *in* [META-INF/microprofile-config.properties](" + propertiesFileUri + ")",
                14, 28, 44));

        // Test right edge
        // Position(14, 43) is the character after the | symbol:
        // @ConfigProperty(name = "greeting.messag|e")
        assertJavaHover(new Position(14, 43), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()), h(
                "`greeting.message = hello` *in* [META-INF/microprofile-config.properties](" + propertiesFileUri + ")",
                14, 28, 44));

        // Test no hover
        // Position(14, 27) is the character after the | symbol:
        // @ConfigProperty(name = |"greeting.message")
        assertJavaHover(new Position(14, 27), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()), null);

        // Test no hover 2
        // Position(14, 44) is the character after the | symbol:
        // @ConfigProperty(name = "greeting.message|")
        assertJavaHover(new Position(14, 44), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()), null);

        // Hover default value
        // Position(17, 33) is the character after the | symbol:
        // @ConfigProperty(name = "greet|ing.suffix", defaultValue="!")
        assertJavaHover(new Position(17, 33), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()),
                h("`greeting.suffix = !` *in* [GreetingResource.java](" + javaFileUri + ")", 17, 28, 43));

        // Hover override default value
        // Position(26, 33) is the character after the | symbol:
        // @ConfigProperty(name = "greet|ing.number", defaultValue="0")
        assertJavaHover(new Position(29, 33), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()),
                h("`greeting.number = 100` *in* [META-INF/microprofile-config.properties](" + propertiesFileUri + ")",
                        29, 28, 43));

        // Hover when no value
        // Position(23, 33) is the character after the | symbol:
        // @ConfigProperty(name = "greet|ing.missing")
        assertJavaHover(new Position(23, 33), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()), h("`greeting.missing` is not set", 23, 28, 44));
    }

    @Test
    public void testConfigPropertyNameHoverWithProfiles() throws Exception {
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.config_hover);
        String javaFileUri = getFileUri("src/main/java/org/acme/config/GreetingResource.java", javaProject);
        String propertiesFileUri = getFileUri("src/main/resources/META-INF/microprofile-config.properties", javaProject);

        saveFile(MicroProfileConfigSourceProvider.MICROPROFILE_CONFIG_PROPERTIES_FILE, //
                "greeting.message = hello\r\n" + //
                        "%dev.greeting.message = hello dev\r\n" + //
                        "%prod.greeting.message = hello prod\r\n" + //
                        "my.greeting.message\r\n" + //
                        "%dev.my.greeting.message",
                javaProject);

        // Position(14, 40) is the character after the | symbol:
        // @ConfigProperty(name = "greeting.mes|sage")
        assertJavaHover(new Position(14, 40), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()), //
                h("`%dev.greeting.message = hello dev` *in* [META-INF/microprofile-config.properties]("
                                + propertiesFileUri + ")  \n" + //
                                "`%prod.greeting.message = hello prod` *in* [META-INF/microprofile-config.properties]("
                                + propertiesFileUri + ")  \n" + //
                                "`greeting.message = hello` *in* [META-INF/microprofile-config.properties](" + propertiesFileUri
                                + ")", //
                        14, 28, 44));

        saveFile(MicroProfileConfigSourceProvider.MICROPROFILE_CONFIG_PROPERTIES_FILE, //
                "%dev.greeting.message = hello dev\r\n" + //
                        "%prod.greeting.message = hello prod\r\n" + //
                        "my.greeting.message\r\n" + //
                        "%dev.my.greeting.message",
                javaProject);

        // Position(14, 40) is the character after the | symbol:
        // @ConfigProperty(name = "greeting.mes|sage")
        assertJavaHover(new Position(14, 40), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()), //
                h("`%dev.greeting.message = hello dev` *in* [META-INF/microprofile-config.properties]("
                                + propertiesFileUri + ")  \n" + //
                                "`%prod.greeting.message = hello prod` *in* [META-INF/microprofile-config.properties]("
                                + propertiesFileUri + ")  \n" + //
                                "`greeting.message` is not set", //
                        14, 28, 44));
    }

    @Test
    public void testConfigPropertyNameMethod() throws Exception {
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.config_quickstart);
        String javaFileUri = getFileUri("src/main/java/org/acme/config/GreetingMethodResource.java", javaProject);
        String propertiesFileUri = getFileUri("src/main/resources/META-INF/microprofile-config.properties", javaProject);

        saveFile(MicroProfileConfigSourceProvider.MICROPROFILE_CONFIG_PROPERTIES_FILE,
                "greeting.method.message = hello", javaProject);

        // Position(22, 61) is the character after the | symbol:
        // @ConfigProperty(name = "greeting.m|ethod.message")
        assertJavaHover(new Position(22, 61), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()),
                h("`greeting.method.message = hello` *in* [META-INF/microprofile-config.properties]("
                        + propertiesFileUri + ")", 22, 51, 74));

        // Position(27, 60) is the character after the | symbol:
        // @ConfigProperty(name = "greeting.m|ethod.suffix" , defaultValue="!")
        assertJavaHover(new Position(27, 60), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()),
                h("`greeting.method.suffix = !` *in* [GreetingMethodResource.java](" + javaFileUri + ")", 27, 50, 72));

        // Position(32, 58) is the character after the | symbol:
        // @ConfigProperty(name = "greeting.method.name")
        assertJavaHover(new Position(32, 48), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()),
                h("`greeting.method.name` is not set", 32, 48, 68));
    }

    @Test
    public void testConfigPropertyNameConstructor() throws Exception {
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.config_quickstart);
        String javaFileUri = getFileUri("src/main/java/org/acme/config/GreetingConstructorResource.java", javaProject);
        String propertiesFileUri = getFileUri("src/main/resources/META-INF/microprofile-config.properties", javaProject);

        saveFile(MicroProfileConfigSourceProvider.MICROPROFILE_CONFIG_PROPERTIES_FILE,
                "greeting.constructor.message = hello", javaProject);

        // Position(23, 48) is the character after the | symbol:
        // @ConfigProperty(name = "greeting.con|structor.message")
        assertJavaHover(new Position(23, 48), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()), //
                h("`greeting.constructor.message = hello` *in* [META-INF/microprofile-config.properties]("
                        + propertiesFileUri + ")", 23, 36, 64));

        // Position(24, 48) is the character after the | symbol:
        // @ConfigProperty(name = "greeting.con|structor.suffix" , defaultValue="!")
        assertJavaHover(new Position(24, 48), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()), //
                h("`greeting.constructor.suffix = !` *in* [GreetingConstructorResource.java](" + javaFileUri + ")", 24,
                        36, 63));

        // Position(25, 48) is the character after the | symbol:
        // @ConfigProperty(name = "greeting.con|structor.name")
        assertJavaHover(new Position(25, 48), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()), //
                h("`greeting.constructor.name` is not set", 25, 36, 61));
    }

    @Test
    public void testConfigPropertyNamePrecendence() throws Exception {
        IConfigSourceProvider.EP_NAME.getPoint().registerExtension(new TestConfigSourceProvider(), getProject());
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.config_hover);
        String javaFileUri = getFileUri("src/main/java/org/acme/config/GreetingResource.java", javaProject);
        String propertiesFileUri = getFileUri("src/main/resources/META-INF/microprofile-config.properties", javaProject);
        String configPropertiesFileUri = getFileUri("src/main/resources/META-INF/config.properties", javaProject);

        saveFile(MicroProfileConfigSourceProvider.MICROPROFILE_CONFIG_PROPERTIES_FILE, //
                "greeting.message = hello\r\n", javaProject);
        saveFile(TestConfigSourceProvider.CONFIG_FILE, //
                "greeting.message = hi\r\n", javaProject);

        // Position(14, 40) is the character after the | symbol:
        // @ConfigProperty(name = "greeting.mes|sage")
        assertJavaHover(new Position(14, 40), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()),
                h("`greeting.message = hi` *in* [META-INF/config.properties](" + configPropertiesFileUri + ")", 14, 28,
                        44));

        saveFile(TestConfigSourceProvider.CONFIG_FILE, //
                "\r\n", javaProject);

        assertJavaHover(new Position(14, 40), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()), h(
                "`greeting.message = hello` *in* [META-INF/microprofile-config.properties](" + propertiesFileUri + ")",
                14, 28, 44));
    }

    @Test
    public void testConfigPropertyNameProfile() throws Exception {

        IConfigSourceProvider.EP_NAME.getPoint().registerExtension(new TestConfigSourceProvider(), getProject());
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.config_hover);
        String javaFileUri = getFileUri("src/main/java/org/acme/config/GreetingResource.java", javaProject);
        String propertiesFileUri = getFileUri("src/main/resources/META-INF/microprofile-config.properties", javaProject);
        String testPropertiesFileUri = getFileUri("src/main/resources/META-INF/microprofile-config-test.properties", javaProject);

        saveFile(MicroProfileConfigSourceProvider.MICROPROFILE_CONFIG_PROPERTIES_FILE, //
                "greeting.message = hello\r\n", javaProject);
        saveFile(TestConfigSourceProvider.MICROPROFILE_CONFIG_TEST_FILE, //
                "greeting.message = hi\r\n", javaProject);

        // Position(14, 40) is the character after the | symbol:
        // @ConfigProperty(name = "greeting.mes|sage")
        assertJavaHover(new Position(14, 40), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()),
                h("`%test.greeting.message = hi` *in* [META-INF/microprofile-config-test.properties]("
                        + testPropertiesFileUri + ")  \n" + //
                        "`greeting.message = hi` *in* [META-INF/microprofile-config-test.properties]("
                        + testPropertiesFileUri + ")", 14, 28, 44));

        saveFile(TestConfigSourceProvider.MICROPROFILE_CONFIG_TEST_FILE, //
                "\r\n", javaProject);

        assertJavaHover(new Position(14, 40), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()), h(
                "`greeting.message = hello` *in* [META-INF/microprofile-config.properties](" + propertiesFileUri + ")",
                14, 28, 44));

    }

    @Test
    public void testConfigPropertyNameResolveExpression() throws Exception {
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.config_hover);
        String javaFileUri = getFileUri("src/main/java/org/acme/config/GreetingResource.java", javaProject);
        String propertiesFileUri = getFileUri("src/main/resources/META-INF/microprofile-config.properties", javaProject);
        IPsiUtils JDT_UTILS = PsiUtilsLSImpl.getInstance(getProject());

        saveFile(MicroProfileConfigSourceProvider.MICROPROFILE_CONFIG_PROPERTIES_FILE, //
                "greeting.message = ${asdf}\r\n" + "asdf = hello", //
                javaProject);
        assertJavaHover(new Position(14, 40), javaFileUri, JDT_UTILS, h(
                "`greeting.message = hello` *in* [META-INF/microprofile-config.properties](" + propertiesFileUri + ")",
                14, 28, 44));

        saveFile(MicroProfileConfigSourceProvider.MICROPROFILE_CONFIG_PROPERTIES_FILE, //
                "greeting.message = ${${asdf}}\r\n" + "asdf = hjkl\r\n" + "hjkl = salutations", //
                javaProject);
        assertJavaHover(new Position(14, 40), javaFileUri, JDT_UTILS,
                h("`greeting.message = salutations` *in* [META-INF/microprofile-config.properties](" + propertiesFileUri
                        + ")", 14, 28, 44));

        saveFile(MicroProfileConfigSourceProvider.MICROPROFILE_CONFIG_PROPERTIES_FILE, //
                "greeting.message = ${asdf:hi}\r\n", //
                javaProject);
        assertJavaHover(new Position(14, 40), javaFileUri, JDT_UTILS,
                h("`greeting.message = hi` *in* [META-INF/microprofile-config.properties](" + propertiesFileUri + ")",
                        14, 28, 44));

        saveFile(MicroProfileConfigSourceProvider.MICROPROFILE_CONFIG_PROPERTIES_FILE, //
                "greeting.message = ${asdf}\r\n", //
                javaProject);
        assertJavaHover(new Position(14, 40), javaFileUri, JDT_UTILS,
                h("`greeting.message = ${asdf}` *in* [META-INF/microprofile-config.properties](" + propertiesFileUri
                        + ")", 14, 28, 44));
    }
}
