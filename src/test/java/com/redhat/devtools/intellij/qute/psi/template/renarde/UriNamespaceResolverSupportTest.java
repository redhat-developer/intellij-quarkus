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
package com.redhat.devtools.intellij.qute.psi.template.renarde;


import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.testFramework.IndexingTestUtil;
import com.redhat.devtools.intellij.qute.psi.QuteMavenModuleImportingTestCase;
import com.redhat.devtools.intellij.qute.psi.QuteMavenProjectName;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForTemplate;
import com.redhat.qute.commons.datamodel.DataModelParameter;
import com.redhat.qute.commons.datamodel.DataModelProject;
import com.redhat.qute.commons.datamodel.DataModelTemplate;
import com.redhat.qute.commons.datamodel.QuteDataModelProjectParams;
import com.redhat.qute.commons.datamodel.resolvers.ValueResolverInfo;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static com.redhat.devtools.intellij.qute.psi.QuteAssert.assertValueResolver;

/**
 * Renarde uri/uriabs namespace resolver support tests.
 * 
 * @author Angelo ZERR
 *
 */
public class UriNamespaceResolverSupportTest extends QuteMavenModuleImportingTestCase {

	@Test
	public void testQuteRenarde() throws Exception {

		loadMavenProject(QuteMavenProjectName.quarkus_renarde_todo);
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());

		QuteDataModelProjectParams params = new QuteDataModelProjectParams(QuteMavenProjectName.quarkus_renarde_todo);
		DataModelProject<DataModelTemplate<DataModelParameter>> project = QuteSupportForTemplate.getInstance()
				.getDataModelProject(params, getJDTUtils(), new EmptyProgressIndicator());
		Assert.assertNotNull(project);

		List<ValueResolverInfo> resolvers = project.getValueResolvers();
		Assert.assertNotNull(resolvers);
		Assert.assertFalse(resolvers.isEmpty());

		testValueResolversFomTemplateExtension(resolvers);
		testValueResolversFomUriNamespace(resolvers);
	}

	private static void testValueResolversFomTemplateExtension(List<ValueResolverInfo> resolvers) {
		// from util.JavaExtensions
		assertValueResolver(null, "isRecent(date : java.util.Date) : boolean", "util.JavaExtensions", resolvers);
	}

	private static void testValueResolversFomUriNamespace(List<ValueResolverInfo> resolvers) {
		Assert.assertNotNull(resolvers);
		Assert.assertFalse(resolvers.isEmpty());

		// public class Application extends Controller
		assertValueResolver("uri", "rest.Application", "rest.Application", //
				"Application", resolvers);
		assertValueResolver("uriabs", "rest.Application", "rest.Application", //
				"Application", resolvers);

		// public class Login extends ControllerWithUser<User>
		assertValueResolver("uri", "rest.Login", "rest.Login", //
				"Login", resolvers);
		assertValueResolver("uriabs", "rest.Login", "rest.Login", //
				"Login", resolvers);

		// public class Todos extends ControllerWithUser<User>
		assertValueResolver("uri", "rest.Todos", "rest.Todos", //
				"Todos", resolvers);
		assertValueResolver("uriabs", "rest.Todos", "rest.Todos", //
				"Todos", resolvers);

		// public class RenardeSecurityController extends Controller {
		assertValueResolver("uri", "io.quarkiverse.renarde.oidc.impl.RenardeSecurityController",
				"io.quarkiverse.renarde.oidc.impl.RenardeSecurityController", //
				"RenardeSecurityController", resolvers);
		assertValueResolver("uriabs", "io.quarkiverse.renarde.oidc.impl.RenardeSecurityController",
				"io.quarkiverse.renarde.oidc.impl.RenardeSecurityController", //
				"RenardeSecurityController", resolvers);

	}

}