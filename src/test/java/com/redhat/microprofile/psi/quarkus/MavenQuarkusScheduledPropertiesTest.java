/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.microprofile.psi.quarkus;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.junit.Test;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.*;

/**
 * Test collection of Quarkus properties from @Scheduled
 */
public class MavenQuarkusScheduledPropertiesTest extends QuarkusMavenModuleImportingTestCase {

    @Test
    public void testConfigQuickstartFromClasspath() throws Exception {
        Module module = loadMavenProject(QuarkusMavenProjectName.scheduler_quickstart);
        MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(myProject), DocumentFormat.PlainText, new EmptyProgressIndicator());

        assertProperties(infoFromClasspath,
                // CounterBean
                // @Scheduled(cron = "{cron.expr}")
                // void cronJobWithExpressionInConfig()
                p(null, "cron.expr", "java.lang.String", null, false,
                        "org.acme.scheduler.CounterBean", null, "cronJobWithExpressionInConfig()V",
                        0, null));

        assertPropertiesDuplicate(infoFromClasspath);
    }

    @Test
    public void testConfigQuickstartFromJavaSources() throws Exception {
        Module module = loadMavenProject(QuarkusMavenProjectName.scheduler_quickstart);
        MicroProfileProjectInfo infoFromJavaSources = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.ONLY_SOURCES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(myProject), DocumentFormat.PlainText, new EmptyProgressIndicator());

        assertProperties(infoFromJavaSources,
                // CounterBean
                // @Scheduled(cron = "{cron.expr}")
                // void cronJobWithExpressionInConfig()
                p(null, "cron.expr", "java.lang.String", null, false,
                        "org.acme.scheduler.CounterBean", null, "cronJobWithExpressionInConfig()V",
                        0, null));

        assertPropertiesDuplicate(infoFromJavaSources);
    }
}