/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import org.eclipse.lsp4j.Location;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * Test with find MicroProfile definition.
 *
 * @author Angelo ZERR
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/PropertiesManagerLocationTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/PropertiesManagerLocationTest.java</a>
 *
 */
public class PropertiesManagerLocationTest extends MavenModuleImportingTestCase {

	@Test
	public void testUsingVertxTest() throws Exception {

		Module javaProject = createMavenModule(new File("projects/lsp4mp/projects/maven/using-vertx"));
		// Test with JAR
		// quarkus.datasource.url
		Location location = PropertiesManager.getInstance().findPropertyLocation(javaProject,
				"io.quarkus.reactive.pg.client.runtime.DataSourceConfig", "url", null, PsiUtilsLSImpl.getInstance(myProject));
		Assert.assertNotNull("Definition from JAR", location);

		// Test with deployment JAR
		// quarkus.arc.auto-inject-fields
		location = PropertiesManager.getInstance().findPropertyLocation(javaProject,
				"io.quarkus.arc.deployment.ArcConfig", "autoInjectFields", null, PsiUtilsLSImpl.getInstance(myProject));
		Assert.assertNotNull("Definition deployment from JAR", location);

		// Test with Java sources
		// myapp.schema.create
		location = PropertiesManager.getInstance().findPropertyLocation(javaProject, "org.acme.vertx.FruitResource",
				"schemaCreate", null, PsiUtilsLSImpl.getInstance(myProject));
		Assert.assertNotNull("Definition from Java Sources", location);
	}

	@Test
	public void testConfigPropertiesTest() throws Exception {

		Module javaProject = createMavenModule(new File("projects/lsp4mp/projects/maven/config-properties"));
		// Test with method
		// greetingInterface.name
		Location location = PropertiesManager.getInstance().findPropertyLocation(javaProject,
				"org.acme.config.IGreetingConfiguration", null, "getName()QOptional<QString;>;",
				PsiUtilsLSImpl.getInstance(myProject));

		Assert.assertNotNull("Definition from IGreetingConfiguration#getName() method", location);
	}

	@Test
	public void testConfigPropertiesMethodTest() throws Exception {

		Module javaProject = createMavenModule(new File("projects/lsp4mp/projects/maven/config-quickstart"));
		// Test with method with parameters
		// greeting.constructor.message
		Location location = PropertiesManager.getInstance().findPropertyLocation(javaProject,
				"org.acme.config.GreetingMethodResource", null, "setMessage(QString;)V",
				PsiUtilsLSImpl.getInstance(myProject));

		Assert.assertNotNull("Definition from GreetingMethodResource#setMessage() method", location);
	}

	@Test
	public void testConfigPropertiesConstructorTest() throws Exception {

		Module javaProject = createMavenModule(new File("projects/lsp4mp/projects/maven/config-quickstart"));
		// Test with constructor with parameters
		// greeting.constructor.message
		Location location = PropertiesManager.getInstance().findPropertyLocation(javaProject,
				"org.acme.config.GreetingConstructorResource", null,
				"GreetingConstructorResource(QString;QString;QOptional<QString;>;)V",
				PsiUtilsLSImpl.getInstance(myProject));

		Assert.assertNotNull("Definition from GreetingConstructorResource constructor", location);
	}
}
