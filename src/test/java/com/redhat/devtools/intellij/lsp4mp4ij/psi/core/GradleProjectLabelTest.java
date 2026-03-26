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
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.GradleTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.apache.commons.io.FileUtils;
import org.eclipse.lsp4mp.commons.MicroProfileJavaProjectLabelsParams;
import org.eclipse.lsp4mp.commons.ProjectLabelInfoEntry;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;

/**
 * Project label tests
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/ProjectLabelTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/ProjectLabelTest.java</a>
 */
public class GradleProjectLabelTest extends GradleTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        FileUtils.copyDirectory(new File("projects/gradle/using-vertx"), new File(getProjectPath()));
        importProject();
    }

    @Test
    public void testGetProjectLabelQuarkusMaven() throws Exception {
        Module module = getModule("using-vertx.main");
        MicroProfileJavaProjectLabelsParams projectLabelsParams = new MicroProfileJavaProjectLabelsParams();
        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/src/main/java/org/acme/vertx/GreetingService.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();
        projectLabelsParams.setUri(uri);
        ProjectLabelInfoEntry projectLabelEntry = ProjectLabelManager.getInstance(module.getProject()).getProjectLabelInfo(projectLabelsParams, PsiUtilsLSImpl.getInstance(module.getProject()));
        assertLabels(projectLabelEntry, "quarkus", "microprofile");
    }

    private void assertLabels(ProjectLabelInfoEntry projectLabelInfoEntry, String... expectedLabels) {
        for (String expectedLabel : expectedLabels) {
            assertContains(projectLabelInfoEntry.getLabels(), expectedLabel);
        }
    }

    private void assertContains(List<String> list, String strToFind) {
        for (String str : list) {
            if (str.equals(strToFind)) {
                return;
            }
        }
        Assert.fail("Expected List to contain <\"" + strToFind + "\">.");
    }
}
