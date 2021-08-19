/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.gradle;

import com.redhat.devtools.intellij.quarkus.search.PropertiesManager;
import com.redhat.devtools.intellij.quarkus.search.PsiUtilsImpl;
import org.apache.commons.io.FileUtils;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.assertProperties;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.assertPropertiesDuplicate;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.p;

/**
 * Test the availability of the MicroProfile GraphQL properties
 *
 * @author Kathryn Kodama
 *
 */
public class GradleMicroProfileGraphQLTest extends GradleTestCase {

	@Override
	public void setUp() throws Exception {
		super.setUp();
		FileUtils.copyDirectory(new File("projects/gradle/microprofile-graphql"), new File(getProjectPath()));
		importProject();
	}

	@Test
	public void testMicroprofileGraphQL() throws Exception {

		MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(getModule("microprofile-graphql.main"), MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsImpl.getInstance(), DocumentFormat.PlainText);

		assertProperties(infoFromClasspath,

				p("microprofile-graphql-api", "mp.graphql.defaultErrorMessage", "java.lang.String",
						"Configured default message displayed when an unchecked exception is thrown from the user application. By default this value is set to \"Server Error\".", true,
						null, null, null, 0, null),

				p("microprofile-graphql-api", "mp.graphql.hideErrorMessage", "java.lang.String",
						"Hide exception error messages when checked exceptions are thrown from the user application. Separate multiple exceptions with a comma. By default all unchecked exceptions are on the `hideErrorMessage` list.", true,
						null, null, null, 0, null),

				p("microprofile-graphql-api", "mp.graphql.showErrorMessage", "java.lang.String",
						"Show exception error messages when unchecked exceptions are thrown from the user application. Separate multiple exceptions with a comma. By default all checked exceptions are on the `showErrorMessage` list.", true,
						null, null, null, 0, null)
		);

		assertPropertiesDuplicate(infoFromClasspath);
	}
}
