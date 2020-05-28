/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.maven;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.quarkus.search.PropertiesManager;
import com.redhat.devtools.intellij.quarkus.search.PsiUtilsImpl;
import com.redhat.microprofile.commons.ClasspathKind;
import com.redhat.microprofile.commons.DocumentFormat;
import com.redhat.microprofile.commons.MicroProfileProjectInfo;
import com.redhat.microprofile.commons.MicroProfilePropertiesScope;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.assertHints;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.assertProperties;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.assertPropertiesDuplicate;
import static com.redhat.devtools.intellij.quarkus.module.MicroProfileAssert.p;

/**
 * Test the availability of the MicroProfile Metrics properties
 *
 * @author David Kwon
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/metrics/MicroProfileMetricsTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/metrics/MicroProfileMetricsTest.java</a>
 *
 */
public class MavenMicroProfileMetricsTest extends MavenImportingTestCase {

	@Test
	public void testMicroprofileMetrics() throws Exception {

		Module module = createMavenModule("microprofile-metrics", new File("projects/maven/microprofile-metrics"));
		MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsImpl.getInstance(), DocumentFormat.PlainText);

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
