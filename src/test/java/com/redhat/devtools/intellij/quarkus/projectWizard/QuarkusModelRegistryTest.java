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
package com.redhat.devtools.intellij.quarkus.projectWizard;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.redhat.devtools.intellij.lsp4mp4ij.classpath.ClasspathResourceChangedManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.redhat.devtools.intellij.quarkus.QuarkusProjectService;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.QUARKUS_CODE_URL;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.QUARKUS_CODE_URL_PROPERTY_NAME;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.QUARKUS_CODE_URL_TEST;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class QuarkusModelRegistryTest  {
    private final QuarkusModelRegistry registry = QuarkusModelRegistry.INSTANCE;
    private static CodeInsightTestFixture myFixture;

    private static final String JAXRS_EXTENSION = "RESTEasy Classic";
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void init() throws Exception {
        System.setProperty(QUARKUS_CODE_URL_PROPERTY_NAME, QUARKUS_CODE_URL_TEST);
        IdeaTestFixtureFactory factory = IdeaTestFixtureFactory.getFixtureFactory();
        TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = factory.createLightFixtureBuilder(QuarkusModelRegistryTest.class.getName());
        IdeaProjectTestFixture fixture = fixtureBuilder.getFixture();

        myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture);
        myFixture.setUp();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        QuarkusProjectService.getInstance(myFixture.getProject()).dispose();
        PsiMicroProfileProjectManager.getInstance(myFixture.getProject()).dispose();
        ClasspathResourceChangedManager.getInstance(myFixture.getProject()).dispose();
        myFixture.tearDown();
    }

    @Before
    @After
    public void resetRegistry() {
        registry.reset();
    }

    @Test
    public void checkThatModelCanLoadWithCodeQuarkusIO() throws IOException {
        assertNotNull(registry.load(QUARKUS_CODE_URL, new EmptyProgressIndicator()));
    }

    @Test
    public void checkThatModelCanLoadWithCodeQuarkusIOAndSpaceInHost() throws IOException {
        assertNotNull(registry.load(QUARKUS_CODE_URL + ' ', new EmptyProgressIndicator()));
    }

    @Test
    public void checkThatModelCanLoadWithCodeQuarkusIOAndSpaceInPath() throws IOException {
        assertNotNull(registry.load(QUARKUS_CODE_URL + "/ ", new EmptyProgressIndicator()));
    }

    @Test(expected = IOException.class)
    public void checkThatIOExceptionIsReturnedWithInvalidURL() throws IOException {
        registry.load("https://invalid.org", new EmptyProgressIndicator());
    }

    private void enableExtension(QuarkusExtensionsModel model, String name) {
        Stream<QuarkusExtension> extStream = model.getCategories().stream().flatMap(cat -> cat.getExtensions().stream()).filter(extension -> extension.getName().equals(name));
        List<QuarkusExtension> extensions = extStream.collect(Collectors.toList());
        if (extensions.isEmpty()) {
           fail("Could not find any "+name+" extension in "+model.getKey());
        }
        extensions.forEach(extension -> extension.setSelected(true));
    }

    private QuarkusExtensionsModel getRecommendedModel(QuarkusModel model) throws IOException {
        String key = model.getStreams().stream().filter(s -> s.isRecommended()).findFirst().orElse(model.getStreams().get(0)).getKey();
        return model.getExtensionsModel(key, new EmptyProgressIndicator());
    }

    private File checkBaseMavenProject(boolean examples) throws IOException {
        File folder = temporaryFolder.newFolder();
        QuarkusModel model = registry.load(QUARKUS_CODE_URL, new EmptyProgressIndicator());
        QuarkusExtensionsModel extensionsModel = getRecommendedModel(model);
        enableExtension(extensionsModel, JAXRS_EXTENSION);
        QuarkusModelRegistry.zip(QUARKUS_CODE_URL, "MAVEN", "org.acme", "code-with-quarkus",
                "0.0.1-SNAPSHOT", "org.acme.ExampleResource", "/example",
                extensionsModel, folder, examples);
        assertTrue(new File(folder, "pom.xml").exists());
        return folder;
    }

    @Test
    public void checkBaseMavenProjectWithExamples() throws IOException {
        File folder = checkBaseMavenProject(true);
        assertTrue(new File(folder, "src/main/java/org/acme/ExampleResource.java").exists());
    }

    @Test
    public void checkBaseMavenProjectWithoutExamples() throws IOException {
        File folder = checkBaseMavenProject(false);
        assertFalse(new File(folder, "src/main/java/org/acme/ExampleResource.java").exists());
    }

    private void enableAllExtensions(QuarkusExtensionsModel model) {
        model.getCategories().stream().filter(category -> !category.getName().equals("Alternative languages")).
                forEach(category -> category.getExtensions().forEach(extension -> extension.setSelected(true)));
    }

    @Test
    public void checkAllExtensionsMavenProject() throws IOException {
        File folder = temporaryFolder.newFolder();
        QuarkusModel model = registry.load(QUARKUS_CODE_URL, new EmptyProgressIndicator());
        QuarkusExtensionsModel extensionsModel = getRecommendedModel(model);
        enableAllExtensions(extensionsModel);
        QuarkusModelRegistry.zip(QUARKUS_CODE_URL, "MAVEN", "org.acme", "code-with-quarkus",
                "0.0.1-SNAPSHOT", "org.acme.ExampleResource", "/example", extensionsModel,
                folder, false);
        assertTrue(new File(folder, "pom.xml").exists());
    }

    private File checkBaseGradleProject(boolean examples) throws IOException {
        File folder = temporaryFolder.newFolder();
        QuarkusModel model = registry.load(QUARKUS_CODE_URL, new EmptyProgressIndicator());
        QuarkusExtensionsModel extensionsModel = getRecommendedModel(model);
        enableExtension(extensionsModel, JAXRS_EXTENSION);
        QuarkusModelRegistry.zip(QUARKUS_CODE_URL, "GRADLE", "org.acme", "code-with-quarkus",
                "0.0.1-SNAPSHOT", "org.acme.ExampleResource", "/example",
                extensionsModel, folder, examples);
        assertTrue(new File(folder, "build.gradle").exists());
        return folder;
    }

    @Test
    public void checkBaseGradleProjectWithExamples() throws IOException {
        File folder  = checkBaseGradleProject(true);
        assertTrue(new File(folder, "src/main/java/org/acme/ExampleResource.java").exists());
    }

    @Test
    public void checkBaseGradleProjectWithoutExamples() throws IOException {
        File folder  = checkBaseGradleProject(false);
        assertFalse(new File(folder, "src/main/java/org/acme/ExampleResource.java").exists());
    }

    @Test
    public void checkAllExtensionsGradleProject() throws IOException {
        File folder = temporaryFolder.newFolder();
        QuarkusModel model = registry.load(QUARKUS_CODE_URL, new EmptyProgressIndicator());
        QuarkusExtensionsModel extensionsModel = getRecommendedModel(model);
        enableAllExtensions(extensionsModel);
        QuarkusModelRegistry.zip(QUARKUS_CODE_URL, "GRADLE", "org.acme", "code-with-quarkus",
                "0.0.1-SNAPSHOT", "org.acme.ExampleResource", "/example", extensionsModel,
                folder, false);
        assertTrue(new File(folder, "build.gradle").exists());
    }
}
