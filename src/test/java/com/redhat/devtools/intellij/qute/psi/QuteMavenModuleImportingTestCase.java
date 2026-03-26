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
package com.redhat.devtools.intellij.qute.psi;

import com.intellij.testFramework.IndexingTestUtil;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;

import com.intellij.openapi.module.Module;
import java.io.File;

/**
 * Base class to import qute maven project.
 *
 */
public abstract class QuteMavenModuleImportingTestCase extends MavenModuleImportingTestCase {

    protected Module loadMavenProject(String projectName) throws Exception {
        Module module = createMavenModule(new File("projects/qute/projects/maven/" + projectName));
        IndexingTestUtil.waitUntilIndexesAreReady(getTestFixture().getProject());
        return module;
    }
}
