/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.config.properties;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.MavenModuleImportingTestCase;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.junit.Test;

import java.io.File;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertProperties;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.assertPropertiesDuplicate;
import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.p;


/**
 * Tests if <code>boolean</code> and <code>int</code> @ConfigItem properties without the
 * <code>defaultValue</code> annotation, has a default value of <code>false</code>
 * and <code>0</code> respectively
 */
public class ConfigItemIntBoolDefaultValueTest extends MavenModuleImportingTestCase {


	@Test
	public void testConfigItemIntBoolDefaultValueTest() throws Exception {

		Module javaProject = createMavenModule("config-quickstart", new File("projects/maven/config-quickstart"));
		MicroProfileProjectInfo infoFromClasspath = PropertiesManager.getInstance().getMicroProfileProjectInfo(javaProject, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC,
				PsiUtilsLSImpl.getInstance(myProject), DocumentFormat.Markdown);

		String booleanDefault = "false";
		String intDefault = "0";

		assertProperties(infoFromClasspath,
				259 + 20 /* properties from Java sources with ConfigProperty */ + //
				7 /* static properties from microprofile-context-propagation-api */,

				// GreetingConstructorResource(
				// 		@ConfigProperty(name = "greeting.constructor.message") String message,
				//		@ConfigProperty(name = "greeting.constructor.suffix" , defaultValue="!") String suffix,
				//		@ConfigProperty(name = "greeting.constructor.name") Optional<String> name)
				p(null, "greeting.constructor.message", "java.lang.String", null, false, "org.acme.config.GreetingConstructorResource",
						null, "GreetingConstructorResource(Ljava/lang/String;Ljava/lang/String;Ljava/util/Optional;)V", 0, null),

				p(null, "greeting.constructor.suffix", "java.lang.String", null, false, "org.acme.config.GreetingConstructorResource",
						null, "GreetingConstructorResource(Ljava/lang/String;Ljava/lang/String;Ljava/util/Optional;)V", 0, "!"),

				p(null, "greeting.constructor.name", "java.util.Optional<java.lang.String>", null, false, "org.acme.config.GreetingConstructorResource",
						null, "GreetingConstructorResource(Ljava/lang/String;Ljava/lang/String;Ljava/util/Optional;)V", 0, null),

				// setMessage(@ConfigProperty(name = "greeting.method.message") String message)
				p(null, "greeting.method.message", "java.lang.String", null, false, "org.acme.config.GreetingMethodResource",
						null, "setMessage(Ljava/lang/String;)V", 0, null),

				// setSuffix(@ConfigProperty(name = "greeting.method.suffix" , defaultValue="!") String suffix)
				p(null, "greeting.method.suffix", "java.lang.String", null, false, "org.acme.config.GreetingMethodResource",
						null, "setSuffix(Ljava/lang/String;)V", 0, "!"),

				// setName(@ConfigProperty(name = "greeting.method.name") Optional<String> name)
				p(null, "greeting.method.name", "java.util.Optional<java.lang.String>", null, false, "org.acme.config.GreetingMethodResource",
						null, "setName(Ljava/util/Optional;)V", 0, null)
				);

		assertPropertiesDuplicate(infoFromClasspath);
	}
}
