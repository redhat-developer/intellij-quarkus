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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core;


import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.IndexingTestUtil;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProject;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.redhat.devtools.intellij.quarkus.QuarkusDeploymentSupport;

import java.io.File;

/**
 * Base class to import lsp4mp maven project.
 */
public abstract class LSP4MPMavenModuleImportingTestCase extends MavenModuleImportingTestCase {

    protected Module loadMavenProject(String projectName) throws Exception {
        return loadMavenProject(projectName, false);
    }

    protected Module loadMavenProject(String projectName, boolean collectAndAddQuarkusDeploymentDependencies) throws Exception {
        Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/" + projectName));
        if(collectAndAddQuarkusDeploymentDependencies) {
            QuarkusDeploymentSupport.getInstance(getTestFixture().getProject()).updateClasspathWithQuarkusDeployment(module, new EmptyProgressIndicator());
        }
        IndexingTestUtil.waitUntilIndexesAreReady(getTestFixture().getProject());
        return module;
    }

    /**
     * Waits until the MicroProfile project runtime is initialized for the given module.
     * <p>
     * The runtime is initialized asynchronously (see
     * {@link PsiMicroProfileProjectManager#initProjectRuntime}) and some features, like
     * diagnostic validation of default values, are skipped while it is not available.
     *
     * @param module the module.
     * @throws InterruptedException if the wait is interrupted.
     */
    protected static void waitForProjectRuntime(Module module) throws InterruptedException {
        PsiMicroProfileProject mpProject = PsiMicroProfileProjectManager.getInstance(module.getProject())
                .getMicroProfileProject(module);
        long deadline = System.currentTimeMillis() + 60_000;
        while (mpProject.getProjectRuntime() == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }
        assertNotNull("MicroProfile project runtime should be initialized", mpProject.getProjectRuntime());
    }
}

