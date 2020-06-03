/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.gradle;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.quarkus.search.PropertiesManager;
import com.redhat.devtools.intellij.quarkus.search.PsiUtilsImpl;
import org.apache.commons.io.FileUtils;
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
public class GradlePropertiesManagerLocationConfigQuickstartTest extends GradleTestCase {

	@Override
	public void setUp() throws Exception {
		super.setUp();
		FileUtils.copyDirectory(new File("projects/gradle/config-quickstart"), new File(getProjectPath()));
		importProject();
	}

	@Test
	public void testConfigPropertiesMethodTest() throws Exception {

		Module javaProject = getModule("config-quickstart.main");

		// Test with method with parameters
		// greeting.constructor.message
		Location location = PropertiesManager.getInstance().findPropertyLocation(javaProject,
				"org.acme.config.GreetingMethodResource", null, "setMessage(QString;)V",
				PsiUtilsImpl.getInstance());

		Assert.assertNotNull("Definition from GreetingMethodResource#setMessage() method", location);
	}

	@Test
	public void testConfigPropertiesConstructorTest() throws Exception {

		Module javaProject = getModule("config-quickstart.main");

		// Test with constructor with parameters
		// greeting.constructor.message
		Location location = PropertiesManager.getInstance().findPropertyLocation(javaProject,
				"org.acme.config.GreetingConstructorResource", null,
				"GreetingConstructorResource(QString;QString;QOptional<QString;>;)V",
				PsiUtilsImpl.getInstance());

		Assert.assertNotNull("Definition from GreetingConstructorResource constructor", location);
	}

}
