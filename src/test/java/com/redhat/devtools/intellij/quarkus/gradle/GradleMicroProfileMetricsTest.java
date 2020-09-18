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
 * Test collect MicroProfile properties from @RegisterRestClient
 * 
 * @see <a href="@see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/metrics/MicroProfileMetricsTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/metrics/MicroProfileMetricsTest.java</a>">@see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/metrics/MicroProfileMetricsTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/metrics/MicroProfileMetricsTest.java</a>
 *
 */
public class GradleMicroProfileMetricsTest extends GradleTestCase {

	@Override
	public void setUp() throws Exception {
		super.setUp();
		FileUtils.copyDirectory(new File("projects/gradle/microprofile-metrics"), new File(getProjectPath()));
		importProject();
	}

	@Test
	public void testMicroprofileMetrics() throws Exception {

		MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(getModule("microprofile-metrics.main"), MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsImpl.getInstance(), DocumentFormat.PlainText);

		assertProperties(infoFromClasspath,

				p("microprofile-metrics-api", "mp.metrics.tags", "java.lang.String",
						"List of tag values.\r\n"
								+ "Tag values set through `mp.metrics.tags` MUST escape equal symbols `=` and commas `,` with a backslash `\\`.",
						true, null, null, null, 0, null),

				p("microprofile-metrics-api", "mp.metrics.appName", "java.lang.String",
						"The app name.",
						true, null, null, null, 0, null)

		);

		assertPropertiesDuplicate(infoFromClasspath);
	}
}
