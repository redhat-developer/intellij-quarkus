/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.*;

/**
 * Test collection of Quarkus properties from classpath kind
 *
 * <ul>
 * <li>not in classpath -> 0 quarkus properties</li>
 * <li>in /java/main/src classpath -> N quarkus properties</li>
 * <li>in /java/main/test classpath-> N + M quarkus properties</li>
 * </ul>
 *
 * @author Angelo ZERR
 */
public class PropertiesManagerClassPathKindTest extends LSP4MPMavenModuleImportingTestCase {

    @Test
    public void testconfigQuickstartTest() throws Exception {

        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.config_quickstart_test, true);
        IPsiUtils JDT_UTILS = PsiUtilsLSImpl.getInstance(javaProject.getProject());

        // not in classpath -> 0 quarkus properties
        VirtualFile fileFromNone = LocalFileSystem.getInstance().findFileByIoFile(new File(ModuleUtilCore.getModuleDirPath(javaProject), "application.properties"));
        MicroProfileProjectInfo infoFromNone = PropertiesManager.getInstance().getMicroProfileProjectInfo(fileFromNone,
                MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, JDT_UTILS,
                DocumentFormat.Markdown, new EmptyProgressIndicator());
        Assert.assertEquals(ClasspathKind.NONE, infoFromNone.getClasspathKind());
        Assert.assertEquals(0, infoFromNone.getProperties().size());

		/*File resteasyJARFile = DependencyUtil.getArtifact("io.quarkus", "quarkus-resteasy-common-deployment",
				"1.0.0.CR1", null, new NullProgressMonitor());
		Assert.assertNotNull("quarkus-resteasy-common-deployment*.jar is missing", resteasyJARFile);*/

        // in /java/main/src classpath -> N quarkus properties
        VirtualFile fileFromSrc = LocalFileSystem.getInstance().findFileByIoFile(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/resources/application.properties"));
        MicroProfileProjectInfo infoFromSrc = PropertiesManager.getInstance().getMicroProfileProjectInfo(fileFromSrc,
                MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, JDT_UTILS,
                DocumentFormat.Markdown, new EmptyProgressIndicator());
        Assert.assertEquals(ClasspathKind.SRC, infoFromSrc.getClasspathKind());
        assertProperties(infoFromSrc, 3 /* properties from Java sources */ + //
                        7 /* static properties from microprofile-context-propagation-api */ +
                        1 /* static property from microprofile config_ordinal */ +
                        180,

                // GreetingResource
                // @ConfigProperty(name = "greeting.message")
                // String message;
                p(null, "greeting.message", "java.lang.String", null, false, "org.acme.config.GreetingResource",
                        "message", null, 0, null),

                // @ConfigProperty(name = "greeting.suffix" , defaultValue="!")
                // String suffix;
                p(null, "greeting.suffix", "java.lang.String", null, false, "org.acme.config.GreetingResource",
                        "suffix", null, 0, "!"),

                // @ConfigProperty(name = "greeting.name")
                // Optional<String> name;
                p(null, "greeting.name", "java.util.Optional<java.lang.String>", null, false, "org.acme.config.GreetingResource", "name",
                        null, 0, null));

        assertPropertiesDuplicate(infoFromSrc);

        // in /java/main/test classpath-> N + M quarkus properties
		/*File undertowJARFile = DependencyUtil.getArtifact("io.quarkus", "quarkus-undertow-deployment", "1.0.0.CR1",
				null, new NullProgressMonitor());
		Assert.assertNotNull("quarkus-undertow-deployment*.jar is missing", undertowJARFile);*/

        VirtualFile filefromTest = LocalFileSystem.getInstance().findFileByIoFile(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/test/resources/application.properties"));
        MicroProfileProjectInfo infoFromTest = PropertiesManager.getInstance().getMicroProfileProjectInfo(filefromTest,
                MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, JDT_UTILS,
                DocumentFormat.Markdown, new EmptyProgressIndicator());
        Assert.assertEquals(ClasspathKind.TEST, infoFromTest.getClasspathKind());
        assertProperties(infoFromTest, 3 /* properties from (src) Java sources */ + //
                        3 /* properties from (test) Java sources */ + //
                        7 /* static properties from microprofile-context-propagation-api */ +
                        1 /* static property from microprofile config_ordinal */ +
                        177,

                // GreetingResource
                // @ConfigProperty(name = "greeting.message")
                // String message;
                p(null, "greeting.message", "java.lang.String", null, false, "org.acme.config.GreetingResource",
                        "message", null, 0, null),

                // @ConfigProperty(name = "greeting.suffix" , defaultValue="!")
                // String suffix;
                p(null, "greeting.suffix", "java.lang.String", null, false, "org.acme.config.GreetingResource",
                        "suffix", null, 0, "!"),

                // @ConfigProperty(name = "greeting.name")
                // Optional<String> name;
                p(null, "greeting.name", "java.util.Optional<java.lang.String>", null, false, "org.acme.config.GreetingResource", "name",
                        null, 0, null),

                // TestResource
                // @ConfigProperty(name = "greeting.message.test")
                // String message;
                p(null, "greeting.message.test", "java.lang.String", null, false, "org.acme.config.TestResource",
                        "message", null, 0, null),

                // @ConfigProperty(name = "greeting.suffix.test" , defaultValue="!")
                // String suffix;
                p(null, "greeting.suffix.test", "java.lang.String", null, false, "org.acme.config.TestResource",
                        "suffix", null, 0, "!"),

                // @ConfigProperty(name = "greeting.name.test")
                // Optional<String> name;
                p(null, "greeting.name.test", "java.util.Optional<java.lang.String>", null, false, "org.acme.config.TestResource", "name",
                        null, 0, null)

        );

        assertPropertiesDuplicate(infoFromTest);
    }

}
