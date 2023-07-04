/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
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
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeLensParams;
import org.junit.Test;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.*;

/**
 * JAX-RS URL Codelens test for Java file.
 *
 * @author Angelo ZERR
 */
public class JaxRsCodeLensTest extends LSP4MPMavenModuleImportingTestCase {

    @Test
    public void testUrlCodeLensProperties() throws Exception {
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.hibernate_orm_resteasy);
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        MicroProfileJavaCodeLensParams params = new MicroProfileJavaCodeLensParams();
        params.setCheckServerAvailable(false);
        String javaFileUri = getFileUri("/src/main/java/org/acme/hibernate/orm/FruitResource.java", javaProject);
        params.setUri(javaFileUri);
        params.setUrlCodeLensEnabled(true);

        // Default port
        assertCodeLenses(8080, "", params, utils);
    }

    @Test
    public void testUrlCodeLensYaml() throws Exception {
        Module javaProject = loadMavenProject(MicroProfileMavenProjectName.hibernate_orm_resteasy_yaml);
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        MicroProfileJavaCodeLensParams params = new MicroProfileJavaCodeLensParams();
        params.setCheckServerAvailable(false);
        String javaFileUri = getFileUri("/src/main/java/org/acme/hibernate/orm/FruitResource.java", javaProject);
        params.setUri(javaFileUri);
        params.setUrlCodeLensEnabled(true);

        // Default port
        assertCodeLenses(8080, "", params, utils);
    }

    private static void assertCodeLenses(int port, String rootPath, MicroProfileJavaCodeLensParams params, IPsiUtils utils) {
        assertCodeLens(params, utils, //
                cl("http://localhost:" + port + rootPath + "/fruits", "", r(31, 4, 4)), //
                cl("http://localhost:" + port + rootPath + "/fruits/{id}", "", r(38, 4, 4)), //
                cl("http://localhost:" + port + rootPath + "/fruits", "", r(48, 4, 4)), //
                cl("http://localhost:" + port + rootPath + "/fruits/{id}", "", r(60, 4, 4)), //
                cl("http://localhost:" + port + rootPath + "/fruits/{id}", "", r(81, 4, 4)));
    }

}