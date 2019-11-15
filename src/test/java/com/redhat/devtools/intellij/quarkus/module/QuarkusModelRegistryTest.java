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

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestExecutionPolicy;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.QUARKUS_CODE_URL;
import static org.junit.Assert.assertNotNull;

public class QuarkusModelRegistryTest  {
    private final QuarkusModelRegistry registry = QuarkusModelRegistry.INSTANCE;
    private static CodeInsightTestFixture myFixture;

    @BeforeClass
    public static void init() throws Exception {
        IdeaTestFixtureFactory factory = IdeaTestFixtureFactory.getFixtureFactory();
        TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = factory.createLightFixtureBuilder();
        IdeaProjectTestFixture fixture = fixtureBuilder.getFixture();

        myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture);
        myFixture.setUp();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        myFixture.tearDown();
    }

    @Test
    public void checkThatModelCanLoadWithCodeQuarkusIO() throws IOException {
        assertNotNull(registry.load(QUARKUS_CODE_URL, new EmptyProgressIndicator()));
    }

    @Test(expected = IOException.class)
    public void checkThatIOExceptionIsReturnedWithInvalidURL() throws IOException {
        registry.load("https://invalid.org", new EmptyProgressIndicator());
    }
}
