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
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertProperties;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertPropertiesDuplicate;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.p;

/**
 * Test collection of Quarkus properties from @Scheduled
 */
public class MavenQuarkusScheduledPropertiesTest extends MavenModuleImportingTestCase {

	@Test
	public void testConfigQuickstartFromClasspath() throws Exception {

		Module module = createMavenModule("scheduler-quickstart", new File("projects/quarkus/maven/scheduler-quickstart"));
		MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(myProject), DocumentFormat.PlainText);

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

		Module module = createMavenModule("scheduler-quickstart", new File("projects/quarkus/maven/scheduler-quickstart"));
		MicroProfileProjectInfo infoFromJavaSources = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.ONLY_SOURCES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(myProject), DocumentFormat.PlainText);

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