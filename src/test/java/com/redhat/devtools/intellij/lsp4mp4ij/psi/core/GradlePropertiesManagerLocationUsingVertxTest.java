/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.GradleTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.apache.commons.io.FileUtils;
import org.eclipse.lsp4j.Location;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * Test with find MicroProfile definition.
 *
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/PropertiesManagerLocationTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/PropertiesManagerLocationTest.java</a>
 */
public class GradlePropertiesManagerLocationUsingVertxTest extends GradleTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        FileUtils.copyDirectory(new File("projects/gradle/using-vertx"), new File(getProjectPath()));
        importProject();
    }

    @Test
    public void testUsingVertxTest() throws Exception {

        Module javaProject = getModule("using-vertx.main");

        // Test with JAR
        // quarkus.datasource.url
        Location location = PropertiesManager.getInstance().findPropertyLocation(javaProject,
                "io.quarkus.reactive.pg.client.runtime.DataSourceReactivePostgreSQLConfig", "cachePreparedStatements", null, PsiUtilsLSImpl.getInstance(myProject));
        Assert.assertNotNull("Definition from JAR", location);

        // Test with deployment JAR
        // quarkus.arc.auto-inject-fields
        location = PropertiesManager.getInstance().findPropertyLocation(javaProject,
                "io.quarkus.arc.deployment.ArcConfig", "autoInjectFields", null, PsiUtilsLSImpl.getInstance(myProject));
        Assert.assertNotNull("Definition deployment from JAR", location);

        // Test with Java sources
        // myapp.schema.create
        location = PropertiesManager.getInstance().findPropertyLocation(javaProject, "org.acme.vertx.FruitResource",
                "schemaCreate", null, PsiUtilsLSImpl.getInstance(myProject));
        Assert.assertNotNull("Definition from Java Sources", location);
    }
}
