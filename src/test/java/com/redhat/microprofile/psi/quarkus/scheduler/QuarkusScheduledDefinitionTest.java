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
package com.redhat.microprofile.psi.quarkus.scheduler;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.quarkus.psi.internal.providers.QuarkusConfigSourceProvider;
import com.redhat.microprofile.psi.quarkus.QuarkusMavenProjectName;
import org.eclipse.lsp4j.Position;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.saveFile;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.assertJavaDefinitions;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.def;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.fixURI;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.r;

/**
 * Quarkus Scheduled annotation property test for definition in properties file.
 */
public class QuarkusScheduledDefinitionTest extends MavenModuleImportingTestCase {


	@Test
	public void testConfigCronPropertyDefinition() throws Exception {

		Module javaProject = createMavenModule(QuarkusMavenProjectName.scheduler_quickstart, new File("projects/quarkus/projects/maven/" + QuarkusMavenProjectName.scheduler_quickstart));
		IPsiUtils JDT_UTILS = PsiUtilsLSImpl.getInstance(myProject);
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/scheduler/CounterBean.java").toURI());
		String propertiesFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/resources/application.properties").toURI());


		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, "cron.expr=*/5 * * * * ?\r\n", javaProject);

		// Position(29, 25) is the character after the | symbol:
		// @Scheduled(cron = "{c|ron.expr}", every = "{every.expr}")
		assertJavaDefinitions(new Position(29, 25), javaFileUri, JDT_UTILS,
				def(r(29, 23, 34), propertiesFileUri, "cron.expr"));
	}

	@Test
	public void testConfigEveryPropertyNameDefinition() throws Exception {

		Module javaProject = createMavenModule(QuarkusMavenProjectName.scheduler_quickstart, new File("projects/quarkus/projects/maven/" + QuarkusMavenProjectName.scheduler_quickstart));
		IPsiUtils JDT_UTILS = PsiUtilsLSImpl.getInstance(myProject);
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/scheduler/CounterBean.java").toURI());
		String propertiesFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/resources/application.properties").toURI());

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, "every.expr=*/5 * * * * ?\r\n", javaProject);

		// Position(29, 48) is the character after the | symbol:
		// @Scheduled(cron = "{cron.expr}", every = "{e|very.expr}")
		assertJavaDefinitions(new Position(29, 48), javaFileUri, JDT_UTILS,
				def(r(29, 46, 58), propertiesFileUri, "every.expr"));
	}

	@Test
	public void testConfigPropertyExpressionDefinition() throws Exception {

		Module javaProject = createMavenModule(QuarkusMavenProjectName.scheduler_quickstart, new File("projects/quarkus/projects/maven/" + QuarkusMavenProjectName.scheduler_quickstart));
		IPsiUtils JDT_UTILS = PsiUtilsLSImpl.getInstance(myProject);
		String javaFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/java/org/acme/scheduler/CounterBean.java").toURI());
		String propertiesFileUri = fixURI(new File(ModuleUtilCore.getModuleDirPath(javaProject), "src/main/resources/application.properties").toURI());

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, "cron.expr=*/5 * * * * ?\r\n", javaProject);

		// Position(35, 26) is the character after the | symbol:
		// @Scheduled(cron = "${c|ron.expr}", every = "${every.expr}")
		assertJavaDefinitions(new Position(35, 26), javaFileUri, JDT_UTILS,
				def(r(35, 23, 35), propertiesFileUri, "cron.expr"));

		saveFile(QuarkusConfigSourceProvider.APPLICATION_PROPERTIES_FILE, "every.expr=*/5 * * * * ?\r\n", javaProject);

		// Position(35, 50) is the character after the | symbol:
		// @Scheduled(cron = "{cron.expr}", every = "{e|very.expr}")
		assertJavaDefinitions(new Position(35, 50), javaFileUri, JDT_UTILS,
				def(r(35, 47, 60), propertiesFileUri, "every.expr"));
	}

}
