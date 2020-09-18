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
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
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

		MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(getModule("microprofile-opentracing.main"), MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsImpl.getInstance(), DocumentFormat.PlainText);

		assertProperties(infoFromClasspath,

				p("microprofile-opentracing-api", "mp.opentracing.server.skip-pattern", "java.util.regex.Pattern",
						"Specifies a skip pattern to avoid tracing of selected REST endpoints.",
						true, null, null, null, 0, null),

				p("microprofile-opentracing-api", "mp.opentracing.server.operation-name-provider", "\"http-path\" or \"class-method\"",
						"Specifies operation name provider for server spans. Possible values are `http-path` and `class-method`.",
						true, null, null, null, 0, "class-method")

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
