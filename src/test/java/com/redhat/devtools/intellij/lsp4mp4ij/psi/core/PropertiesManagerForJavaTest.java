/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4mp.commons.JavaFileInfo;
import org.eclipse.lsp4mp.commons.MicroProfileJavaFileInfoParams;
import org.junit.Test;

import java.io.File;

/**
 * Test for Java file information.
 *
 * @see <a href="https://github.com/eclipse/lsp4mp/blob/master/microprofile.jdt/org.eclipse.lsp4mp.jdt.test/src/main/java/org/eclipse/lsp4mp/jdt/core/PropertiesManagerForJavaTest.java">https://github.com/eclipse/lsp4mp/blob/master/microprofile.jdt/org.eclipse.lsp4mp.jdt.test/src/main/java/org/eclipse/lsp4mp/jdt/core/PropertiesManagerForJavaTest.java</a>
 */
public class PropertiesManagerForJavaTest extends MavenModuleImportingTestCase {
    private Module javaProject;

    private String getJavaFileUri(String path) {
        return new File(ModuleUtilCore.getModuleDirPath(javaProject), path).toURI().toString();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        javaProject = createMavenModule(new File("projects/lsp4mp/projects/maven/config-hover"));
    }

    @Test
    public void testFileInfoWithPackage() throws Exception {
        MicroProfileJavaFileInfoParams params = new MicroProfileJavaFileInfoParams();
        String javaFileUri = getJavaFileUri("src/main/java/org/acme/config/GreetingResource.java");
        params.setUri(javaFileUri);
        JavaFileInfo javaFileInfo = PropertiesManagerForJava.getInstance().fileInfo(params, PsiUtilsLSImpl.getInstance(getProject()));
        assertNotNull(javaFileInfo);
        assertEquals("org.acme.config", javaFileInfo.getPackageName());
    }

    @Test
    public void testFileInfoWithoutPackage() throws Exception {
        String javaFileUri = getJavaFileUri("src/main/java/NoPackage.java");

        MicroProfileJavaFileInfoParams params = new MicroProfileJavaFileInfoParams();
        params.setUri(javaFileUri);
        JavaFileInfo javaFileInfo = PropertiesManagerForJava.getInstance().fileInfo(params, PsiUtilsLSImpl.getInstance(getProject()));
        assertNotNull(javaFileInfo);
        assertEquals("", javaFileInfo.getPackageName());
    }

    @Test
    public void testFileInfoNull() throws Exception {
        String javaFileUri = getJavaFileUri("src/main/java/BAD_JAVA_FILE.java");

        MicroProfileJavaFileInfoParams params = new MicroProfileJavaFileInfoParams();
        params.setUri(javaFileUri);
        JavaFileInfo javaFileInfo = PropertiesManagerForJava.getInstance().fileInfo(params, PsiUtilsLSImpl.getInstance(getProject()));
        assertNull(javaFileInfo);
    }
}
