/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.health.properties;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.LSP4MPMavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileMavenProjectName;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.junit.Test;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.*;

/**
 * Test the availability of the MicroProfile Health properties
 *
 * @author Ryan Zegray
 */
public class MicroProfileHealthTest extends LSP4MPMavenModuleImportingTestCase {

    @Test
    public void testMicroprofileHealth() throws Exception {

        Module module = loadMavenProject(MicroProfileMavenProjectName.microprofile_health_quickstart);
        MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(getProject()), DocumentFormat.PlainText, new EmptyProgressIndicator());

        assertProperties(infoFromClasspath,

                p("microprofile-health-api", "mp.health.disable-default-procedures", "boolean",
                        "Disable all default vendor procedures and display only the user-defined health check procedures.", true,
                        null, null, null, 0, null)
        );

        assertPropertiesDuplicate(infoFromClasspath);
    }
}
