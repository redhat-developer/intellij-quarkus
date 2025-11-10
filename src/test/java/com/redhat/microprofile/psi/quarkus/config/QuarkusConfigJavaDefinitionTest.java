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
package com.redhat.microprofile.psi.quarkus.config;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.quarkus.psi.internal.providers.QuarkusConfigSourceProvider;
import com.redhat.microprofile.psi.quarkus.QuarkusMavenModuleImportingTestCase;
import com.redhat.microprofile.psi.quarkus.QuarkusMavenProjectName;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.saveFile;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileForJavaAssert.*;

/**
 * MicroProfileConfig name definition to properties files.
 *
 *
 */
public class QuarkusConfigJavaDefinitionTest extends QuarkusMavenModuleImportingTestCase {

	@Test
	public void testConfigPropertyNameDefinitionYml() throws Exception {

		Module javaProject = loadMavenProject(QuarkusMavenProjectName.config_hover);
		String javaFileUri = getFileUri("src/main/java/org/acme/config/GreetingResource.java", javaProject);
		String applicationYmlFileUri = getFileUri("src/main/resources/application.yml", javaProject);

		saveFile(QuarkusConfigSourceProvider.APPLICATION_YML_FILE, //
				"greeting:\n" + //
				"  message: hello\n" + //
				"  name: quarkus\n" + //
				"  number: 100\n",
				javaProject);
		// Position(14, 40) is the character after the | symbol:
		// @ConfigProperty(name = "greeting.mes|sage")
		assertJavaDefinitions(p(14, 40), javaFileUri, PsiUtilsLSImpl.getInstance(getProject()), //
				def(r(14, 28, 44), applicationYmlFileUri, "greeting.message"));

	}
}