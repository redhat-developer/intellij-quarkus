/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.microprofile.psi.quarkus.route;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.microprofile.psi.quarkus.QuarkusMavenModuleImportingTestCase;
import com.redhat.microprofile.psi.quarkus.QuarkusMavenProjectName;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeLensParams;
import org.junit.Test;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.*;

/**
 * Tests for the CodeLens features introduced by
 * {@link com.redhat.microprofile.psi.internal.quarkus.route.java.ReactiveRouteJaxRsInfoProvider}.
 */
public class ReactiveRouteTest extends QuarkusMavenModuleImportingTestCase {

    @Test
    public void testMultipleRoutesCodeLens() throws Exception {
        Module javaProject = loadMavenProject(QuarkusMavenProjectName.quarkus_route);
        assertNotNull(javaProject);

        MicroProfileJavaCodeLensParams params = new MicroProfileJavaCodeLensParams();
        params.setCheckServerAvailable(false);
        String javaFileUri = getFileUri("src/main/java/org/acme/reactive/routes/MultipleRoutes.java", javaProject);
        params.setUri(javaFileUri);
        params.setUrlCodeLensEnabled(true);

        assertCodeLens(params, PsiUtilsLSImpl.getInstance(myProject), //
                cl("http://localhost:8080/first", "", r(9, 4, 4)), //
                cl("http://localhost:8080/second", "", r(9, 4, 4)));
    }

    @Test
    public void testMyDeclarativeRoutesCodeLens() throws Exception {
        Module javaProject = loadMavenProject(QuarkusMavenProjectName.quarkus_route);
        assertNotNull(javaProject);

        MicroProfileJavaCodeLensParams params = new MicroProfileJavaCodeLensParams();
        params.setCheckServerAvailable(false);
        String javaFileUri = getFileUri("src/main/java/org/acme/reactive/routes/MyDeclarativeRoutes.java", javaProject);
        params.setUri(javaFileUri);
        params.setUrlCodeLensEnabled(true);

        assertCodeLens(params, PsiUtilsLSImpl.getInstance(myProject), //
                cl("http://localhost:8080/hello", "", r(15, 4, 4)), //
                cl("http://localhost:8080/world", "", r(20, 4, 4)), //
                cl("http://localhost:8080/greetings", "", r(25, 4, 4)), //
                cl("http://localhost:8080/greetings/:name", "", r(30, 4, 4)));
    }

    @Test
    public void testSimpleRoutesCodeLens() throws Exception {
        Module javaProject = loadMavenProject(QuarkusMavenProjectName.quarkus_route);
        assertNotNull(javaProject);

        MicroProfileJavaCodeLensParams params = new MicroProfileJavaCodeLensParams();
        params.setCheckServerAvailable(false);
        String javaFileUri = getFileUri("src/main/java/org/acme/reactive/routes/SimpleRoutes.java", javaProject);
        params.setUri(javaFileUri);
        params.setUrlCodeLensEnabled(true);

        assertCodeLens(params, PsiUtilsLSImpl.getInstance(myProject), //
                cl("http://localhost:8080/simple/ping", "", r(10, 4, 4)));
    }
}
