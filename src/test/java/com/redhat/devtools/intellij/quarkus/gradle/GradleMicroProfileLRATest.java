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

import com.redhat.devtools.intellij.quarkus.search.PropertiesManager;
import com.redhat.devtools.intellij.quarkus.search.PsiUtilsImpl;
import com.redhat.microprofile.commons.ClasspathKind;
import com.redhat.microprofile.commons.DocumentFormat;
import com.redhat.microprofile.commons.MicroProfileProjectInfo;
import com.redhat.microprofile.commons.MicroProfilePropertiesScope;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.assertProperties;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.assertPropertiesDuplicate;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.p;

/**
 * Test the availability of the MicroProfile LRA properties
 *
 * @author David Kwon
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/lra/MicroProfileLRATest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/lra/MicroProfileLRATest.java</a>
 *
 */
public class GradleMicroProfileLRATest extends GradleTestCase {

	@Override
	public void setUp() throws Exception {
		super.setUp();
		FileUtils.copyDirectory(new File("projects/gradle/microprofile-lra"), new File(getProjectPath()));
		importProject();
	}

	@Test
	public void testMicroprofileLRA() throws Exception {

		MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(getModule("microprofile-lra.main"), MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsImpl.getInstance(), DocumentFormat.PlainText);

		assertProperties(infoFromClasspath,

				p("microprofile-lra-api", "mp.lra.propagation.active", "java.lang.String",
						"When a JAX-RS endpoint, or the containing class, is not "
								+ "annotated with `@LRA`, but it is called on a MicroProfile "
								+ "LRA compliant runtime, the system will propagate the LRA "
								+ "related HTTP headers when this parameter resolves to true.\r\n\r\n"
								+ "The behaviour is similar to the `LRA.Type` `SUPPORTS` "
								+ "(when true) and `NOT_SUPPORTED` (when false) values but "
								+ "only defines the propagation aspect.\r\n\r\n"
								+ "In other words the class does not have to be a participant in "
								+ "order for the LRA context to propagate, i.e. such propagation "
								+ "of the header does not imply that the LRA is in any particular "
								+ "state, and in fact the LRA may not even correspond to a valid LRA.",
						true, null, null, null, 0, null)

		);

		assertPropertiesDuplicate(infoFromClasspath);
	}
}
