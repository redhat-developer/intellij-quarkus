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
import com.redhat.devtools.intellij.quarkus.search.PsiUtils;
import com.redhat.microprofile.commons.ClasspathKind;
import com.redhat.microprofile.commons.DocumentFormat;
import com.redhat.microprofile.commons.MicroProfileProjectInfo;
import com.redhat.microprofile.commons.MicroProfilePropertiesScope;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.assertHints;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.assertHintsDuplicate;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.assertProperties;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.assertPropertiesDuplicate;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.h;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.p;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.vh;

/**
 * Test the availability of the MicroProfile Metrics properties
 * 
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/opentracing/MicroProfileOpenTracingTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/opentracing/MicroProfileOpenTracingTest.java</a>
 *
 */
public class GradleMicroProfileOpenTracingTest extends GradleTestCase {

	@Override
	public void setUp() throws Exception {
		super.setUp();
		FileUtils.copyDirectory(new File("projects/gradle/microprofile-opentracing"), new File(getProjectPath()));
		importProject();
	}

	@Test
	public void testMicroprofileMetrics() throws Exception {

		MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(getModule("microprofile-opentracing.main"), MicroProfilePropertiesScope.ONLY_SOURCES, ClasspathKind.SRC, PsiUtils.getInstance(), DocumentFormat.PlainText);

		assertProperties(infoFromClasspath,

				p(null, "mp.opentracing.server.skip-pattern", "java.util.Optional<java.util.regex.Pattern>",
						"Specifies a skip pattern to avoid tracing of selected REST endpoints.",
						false, null, null, null, 0, null),

				p(null, "mp.opentracing.server.operation-name-provider", "\"http-path\" or \"class-method\"",
						"Specifies operation name provider for server spans. Possible values are `http-path` and `class-method`.",
						false, null, null, null, 0, "class-method")

		);

		assertPropertiesDuplicate(infoFromClasspath);

		assertHints(infoFromClasspath, h("mp.opentracing.server.operation-name-provider", null, false, "mp.opentracing.server.operation-name-provider", //
				vh("class-method", "The provider for the default operation name.", null), //
				vh("http-path", "The operation name has the following form `<HTTP method>:<@Path value of endpoint’s class>/<@Path value of endpoint’s method>`. "
						+ "For example if the class is annotated with `@Path(\"service\")` and method `@Path(\"endpoint/{id: \\\\d+}\")` "
						+ "then the operation name is `GET:/service/endpoint/{id: \\\\d+}`.", null)) //
		);



		assertHintsDuplicate(infoFromClasspath);
	}
}
