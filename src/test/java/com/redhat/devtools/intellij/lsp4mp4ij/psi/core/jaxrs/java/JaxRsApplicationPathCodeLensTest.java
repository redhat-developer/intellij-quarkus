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
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.LSP4MPMavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileMavenProjectName;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeLensParams;
import org.junit.Test;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.saveFile;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.*;

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

        saveFile("org/acme/MyApplication.java", """
                package org.acme;
                import javax.ws.rs.ApplicationPath;
                import javax.ws.rs.core.Application;
                @ApplicationPath("/api")
                public class MyApplication extends Application {}
                """, javaProject, true);

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

        saveFile("org/acme/MyApplication.java", """
                package org.acme;
                import javax.ws.rs.ApplicationPath;
                import javax.ws.rs.core.Application;
                @ApplicationPath("api")
                public class MyApplication extends Application {}
                """, javaProject, true);

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

        saveFile("org/acme/MyApplication.java", """
                package org.acme;
                import javax.ws.rs.ApplicationPath;
                import javax.ws.rs.core.Application;
                @ApplicationPath("/api")
                public class MyApplication extends Application {}
                """, javaProject, true);

        // Default port
        assertCodeLenses(8080, params, utils, "/api/path", r(12, 4, 4));

        saveFile("org/acme/MyApplication.java", """
                package org.acme;
                import javax.ws.rs.ApplicationPath;
                import javax.ws.rs.core.Application;
                @ApplicationPath("/ipa")
                public class MyApplication extends Application {}
                """, javaProject, true);

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
