/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.faulttolerance.properties;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.faulttolerance.MicroProfileFaultToleranceConstants;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertHints;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertHintsDuplicate;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertProperties;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertPropertiesDuplicate;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.h;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.p;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.vh;

/**
 * Test the availability of the MicroProfile Metrics properties
 *
 * @author David Kwon
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/faulttolerance/MicroProfileOpenTracingTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/faulttolerance/MicroProfileFaultToleranceTest.java</a>
 *
 */
@Ignore("Skipped because dependencies on the classpath may not have sources")
public class MicroProfileFaultTolerancePropertiesTest extends MavenModuleImportingTestCase {

	@Test
	public void testMicroprofileFaultTolerancePropertiesTest() throws Exception {

		Module module = createMavenModule(new File("projects/lsp4mp/projects/maven/microprofile-fault-tolerance"));
		MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.ONLY_SOURCES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(myProject), DocumentFormat.Markdown);

		assertProperties(infoFromClasspath,

				// <classname>/<annotation>/<parameter>
				p(null, "org.acme.MyClient/Retry/maxRetries", "int", " *  **Returns:**" + System.lineSeparator() + //
								"    " + System.lineSeparator() + //
								"     *  The max number of retries. -1 means retry forever. The value must be greater than or equal to -1.",
						false, "org.acme.MyClient", null, null, 0, "3"),

				// <classname>/<methodname>/<annotation>/<parameter>
				p(null, "org.acme.MyClient/serviceA/Retry/maxRetries", "int",
						" *  **Returns:**" + System.lineSeparator() + //
								"    " + System.lineSeparator() + //
								"     *  The max number of retries. -1 means retry forever. The value must be greater than or equal to -1.",
						false, "org.acme.MyClient", null, "serviceA()V", 0, "90"),

				p(null, "org.acme.MyClient/serviceA/Retry/delay", "long",
						"The delay between retries. Defaults to 0. The value must be greater than or equal to 0."
								+ System.lineSeparator() + //
								"" + System.lineSeparator() + //
								" *  **Returns:**" + System.lineSeparator() + //
								"    " + System.lineSeparator() + //
								"     *  the delay time",
						false, "org.acme.MyClient", null, "serviceA()V", 0, "0"),

				// <annotation>
				// -> <annotation>/enabled
				p(null, "Asynchronous/enabled", "boolean", "Enabling the policy", false,
						"org.eclipse.microprofile.faulttolerance.Asynchronous", null, null, 0, "true"),

				// <annotation>/<parameter>
				p(null, "Bulkhead/value", "int",
						"Specify the maximum number of concurrent calls to an instance. The value must be greater than 0. Otherwise, `org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException` occurs."
								+ System.lineSeparator() + //
								"" + System.lineSeparator() + //
								" *  **Returns:**" + System.lineSeparator() + //
								"    " + System.lineSeparator() + //
								"     *  the limit of the concurrent calls",
						false, "org.eclipse.microprofile.faulttolerance.Bulkhead", null, "value()I", 0, "10"),

				p(null, "MP_Fault_Tolerance_NonFallback_Enabled", "boolean",
						MicroProfileFaultToleranceConstants.MP_FAULT_TOLERANCE_NONFALLBACK_ENABLED_DESCRIPTION, false,
						null, null, null, 0, "false")

		);

		assertPropertiesDuplicate(infoFromClasspath);

		assertHints(infoFromClasspath, h("java.time.temporal.ChronoUnit", null, true, "java.time.temporal.ChronoUnit", //
				vh("NANOS", null, null), //
				vh("MICROS", null, null), //
				vh("MILLIS", null, null), //
				vh("SECONDS", null, null), //
				vh("MINUTES", null, null), //
				vh("HALF_DAYS", null, null), //
				vh("DAYS", null, null), //
				vh("WEEKS", null, null), //
				vh("MONTHS", null, null), //
				vh("YEARS", null, null), //
				vh("DECADES", null, null), //
				vh("CENTURIES", null, null), //
				vh("MILLENNIA", null, null), //
				vh("ERAS", null, null), //
				vh("FOREVER", null, null)) //
		);

		assertHintsDuplicate(infoFromClasspath);
	}
}
