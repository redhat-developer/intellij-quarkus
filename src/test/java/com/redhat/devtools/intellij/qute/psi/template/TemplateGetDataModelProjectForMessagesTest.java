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
package com.redhat.devtools.intellij.qute.psi.template;

import java.util.List;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.testFramework.IndexingTestUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.qute.psi.QuteMavenModuleImportingTestCase;
import com.redhat.devtools.intellij.qute.psi.QuteMavenProjectName;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForTemplate;
import org.junit.Assert;
import org.junit.Test;

import com.redhat.qute.commons.datamodel.DataModelParameter;
import com.redhat.qute.commons.datamodel.DataModelProject;
import com.redhat.qute.commons.datamodel.DataModelTemplate;
import com.redhat.qute.commons.datamodel.QuteDataModelProjectParams;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverInfo;

import static com.redhat.devtools.intellij.qute.psi.QuteAssert.*;

/**
 * Tests for
 * {@link QuteSupportForTemplate#getDataModelProject(QuteDataModelProjectParams, IPsiUtils, ProgressIndicator)}
 * to tests Type-safe Message Bundles support
 *
 * @author Angelo ZERR
 *
 */
public class TemplateGetDataModelProjectForMessagesTest extends QuteMavenModuleImportingTestCase {

	@Test
	public void testQuteMessages() throws Exception {

		loadMavenProject(QuteMavenProjectName.qute_messages);

		QuteDataModelProjectParams params = new QuteDataModelProjectParams(QuteMavenProjectName.qute_messages);
		DataModelProject<DataModelTemplate<DataModelParameter>> project = QuteSupportForTemplate.getInstance()
				.getDataModelProject(params, PsiUtilsLSImpl.getInstance(getProject()), new EmptyProgressIndicator());
		Assert.assertNotNull(project);

        IndexingTestUtil.waitUntilIndexesAreReady(getProject());

		// Test templates
		testTemplates(project);

		// Test value resolvers
		List<ValueResolverInfo> resolvers = project.getValueResolvers();
		Assert.assertNotNull(resolvers);
		Assert.assertFalse(resolvers.isEmpty());
		testValueResolversFromMessages(resolvers);
	}

	private static void testTemplates(DataModelProject<DataModelTemplate<DataModelParameter>> project) {
		List<DataModelTemplate<DataModelParameter>> templates = project.getTemplates();
		Assert.assertNotNull(templates);
		Assert.assertFalse(templates.isEmpty());

		templateField(project);
	}

	private static void templateField(DataModelProject<DataModelTemplate<DataModelParameter>> project) {
		// private final Template page;

		DataModelTemplate<DataModelParameter> pageTemplate = project
				.findDataModelTemplate("src/main/resources/templates/page");
		Assert.assertNotNull(pageTemplate);
		Assert.assertEquals("src/main/resources/templates/page", pageTemplate.getTemplateUri());
		Assert.assertEquals("org.acme.SomePage", pageTemplate.getSourceType());
		Assert.assertEquals("page", pageTemplate.getSourceField());

		List<DataModelParameter> parameters = pageTemplate.getParameters();
		Assert.assertNotNull(parameters);

		// return page.data("name", name);

		Assert.assertEquals(1, parameters.size());
		assertParameter("name", "java.lang.String", true, parameters, 0);

	}

	private static void testValueResolversFromMessages(List<ValueResolverInfo> resolvers) {

		// @MessageBundle
		// public interface App2Messages {

		// @Message("HELLO!")
		// String hello();
		ValueResolverInfo message = assertValueResolver("msg2", "hello() : java.lang.String", "org.acme.App2Messages",
				null, false, resolvers);
		assertMessageResolverData(null, "HELLO!", message);

		// @Message
		// String hello2();
		message = assertValueResolver("msg2", "hello2() : java.lang.String", "org.acme.App2Messages", null, false,
				resolvers);
		assertMessageResolverData(null, null, message);

		// @MessageBundle
		// public interface AppMessages {

		// @Message("Hello {name ?: 'Qute'}")
		// String hello_name(String name);
		message = assertValueResolver("msg", "hello_name(name : java.lang.String) : java.lang.String",
				"org.acme.AppMessages", null, false, resolvers);
		assertMessageResolverData(null, "Hello {name ?: 'Qute'}", message);

		// @Message("Goodbye {name}!")
		// String goodbye(String name);
		message = assertValueResolver("msg", "goodbye(name : java.lang.String) : java.lang.String",
				"org.acme.AppMessages", null, false, resolvers);
		assertMessageResolverData(null, "Goodbye {name}!", message);

		// @Message("Hello!")
		// String hello();
		message = assertValueResolver("msg", "hello() : java.lang.String", "org.acme.AppMessages", null, false,
				resolvers);
		assertMessageResolverData(null, "Hello!", message);

		// @Message
		// String hello2();
		message = assertValueResolver("msg", "hello2() : java.lang.String", "org.acme.AppMessages", null, false,
				resolvers);
		assertMessageResolverData(null, null, message);
	}

}
