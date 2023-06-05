/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.java;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.LSP4MPMavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileMavenProjectName;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManagerForJava;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeLensParams;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.saveFile;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.*;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.r;

/**
 * JAX-RS URL Codelens test for Java file with @ApplicationPath annotation.
 */
public class JaxRsApplicationPathCodeLensTest extends LSP4MPMavenModuleImportingTestCase {

    @Test
    public void testUrlCodeLensApplicationPath() throws Exception {
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.microprofile_applicationpath);
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        MicroProfileJavaCodeLensParams params = new MicroProfileJavaCodeLensParams();
        params.setCheckServerAvailable(false);
        String javaFileUri = getFileUri("src/main/java/org/acme/ApplicationPathResource.java", javaProject);
        params.setUri(javaFileUri);
        params.setUrlCodeLensEnabled(true);

        saveFile("org/acme/MyApplication.java", "package org.acme;\r\n" + //
                "import javax.ws.rs.ApplicationPath;\r\n" + //
                "import javax.ws.rs.core.Application;\r\n" + //
                "@ApplicationPath(\"/api\")\r\n" + //
                "public class MyApplication extends Application {}\r\n", javaProject, true);

        // Default port
        assertCodeLenses(8080, params, utils, "/api/path", r(12, 4, 4));
    }

    @Test
    public void testUrlCodeLensApplicationPathNoSlash() throws Exception {
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.microprofile_applicationpath);
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        MicroProfileJavaCodeLensParams params = new MicroProfileJavaCodeLensParams();
        params.setCheckServerAvailable(false);
        String javaFileUri = getFileUri("src/main/java/org/acme/ApplicationPathResource.java", javaProject);
        params.setUri(javaFileUri);
        params.setUrlCodeLensEnabled(true);

        saveFile("org/acme/MyApplication.java", "package org.acme;\r\n" + //
                "import javax.ws.rs.ApplicationPath;\r\n" + //
                "import javax.ws.rs.core.Application;\r\n" + //
                "@ApplicationPath(\"api\")\r\n" + //
                "public class MyApplication extends Application {}\r\n", javaProject, true);

        // Default port
        assertCodeLenses(8080, params, utils, "/api/path", r(12, 4, 4));
    }

    @Test
    public void testUrlCodeLensApplicationPathChange() throws Exception {
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.microprofile_applicationpath);
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        MicroProfileJavaCodeLensParams params = new MicroProfileJavaCodeLensParams();
        params.setCheckServerAvailable(false);
        String javaFileUri = getFileUri("src/main/java/org/acme/ApplicationPathResource.java", javaProject);
        params.setUri(javaFileUri);
        params.setUrlCodeLensEnabled(true);

        saveFile("org/acme/MyApplication.java", "package org.acme;\r\n" + //
                "import javax.ws.rs.ApplicationPath;\r\n" + //
                "import javax.ws.rs.core.Application;\r\n" + //
                "@ApplicationPath(\"/api\")\r\n" + //
                "public class MyApplication extends Application {}\r\n", javaProject, true);

        // Default port
        assertCodeLenses(8080, params, utils, "/api/path", r(12, 4, 4));

        saveFile("org/acme/MyApplication.java", "package org.acme;\r\n" + //
                "import javax.ws.rs.ApplicationPath;\r\n" + //
                "import javax.ws.rs.core.Application;\r\n" + //
                "@ApplicationPath(\"/ipa\")\r\n" + //
                "public class MyApplication extends Application {}\r\n", javaProject, true);

        assertCodeLenses(8080, params, utils, "/ipa/path", r(12, 4, 4));
    }

    @Test
    public void testOpenLibertyJakarta() throws Exception {
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.open_liberty);
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        MicroProfileJavaCodeLensParams params = new MicroProfileJavaCodeLensParams();
        params.setCheckServerAvailable(false);
        String javaFileUri = getFileUri("src/main/java/com/demo/rest/MyResource.java", javaProject);

        params.setUri(javaFileUri);
        params.setUrlCodeLensEnabled(true);

        assertCodeLenses(8080, params, utils, "/api/api/resource", r(13, 1, 1));
    }

    private static void assertCodeLenses(int port, MicroProfileJavaCodeLensParams params, IPsiUtils utils,
                                         String testPath, Range range) {
        assertCodeLens(params, utils, //
                cl("http://localhost:" + port + testPath, "", range));
    }

}
