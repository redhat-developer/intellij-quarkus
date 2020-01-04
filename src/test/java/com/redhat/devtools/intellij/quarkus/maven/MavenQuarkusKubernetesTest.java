/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
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
import com.redhat.devtools.intellij.quarkus.search.PsiUtils;
import com.redhat.microprofile.commons.ClasspathKind;
import com.redhat.microprofile.commons.DocumentFormat;
import com.redhat.microprofile.commons.MicroProfileProjectInfo;
import com.redhat.microprofile.commons.MicroProfilePropertiesScope;
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
 * Quarkus Kubernetes properties test.
 * 
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/QuarkusKubernetesTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/QuarkusKubernetesTest.java</a>
 *
 */
public class MavenQuarkusKubernetesTest extends MavenImportingTestCase {

	@Test
	public void testKubernetes() throws Exception {
		Module module = createMavenModule("kubernetes", new File("projects/maven/kubernetes"));
		MicroProfileProjectInfo info = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtils.getInstance(), DocumentFormat.Markdown);

		assertProperties(info,

				// io.dekorate.kubernetes.annotation.KubernetesApplication
				p(null, "kubernetes.name", "java.lang.String",
						"The name of the application. This value will be used for naming Kubernetes resources like: - Deployment - Service and so on ... If no value is specified it will attempt to determine the name using the following rules: If its a maven/gradle project use the artifact id. Else if its a bazel project use the name. Else if the system property app.name is present it will be used. Else find the project root folder and use its name (root folder detection is done by moving to the parent folder until .git is found)."
								+ System.lineSeparator() + "" + System.lineSeparator() + " *  **Returns:**"
								+ System.lineSeparator() + "    " + System.lineSeparator()
								+ "     *  The specified application name.",
						true, "io.dekorate.kubernetes.annotation.KubernetesApplication", null,
						"name()Ljava/lang/String;", 0, null),

				p(null, "kubernetes.readiness-probe.initial-delay-seconds", "int",
						"The amount of time to wait in seconds before starting to probe." + System.lineSeparator() +
								System.lineSeparator() +
								" *  **Returns:**" + System.lineSeparator() +
								"    " + System.lineSeparator() +
								"     *  The initial delay.", true,
						"io.dekorate.kubernetes.annotation.Probe", null, "initialDelaySeconds()I", 0, "0"),

				p(null, "kubernetes.annotations[*].key", "java.lang.String", null, true,
						"io.dekorate.kubernetes.annotation.Annotation", null, "key()Ljava/lang/String;", 0, null),

				p(null, "kubernetes.init-containers[*].ports[*].protocol", "io.dekorate.kubernetes.annotation.Protocol",
						null, true, "io.dekorate.kubernetes.annotation.Port", null,
						"protocol()Lio/dekorate/kubernetes/annotation/Protocol;", 0, "TCP"),

				p(null, "kubernetes.group", "java.lang.String",
						"The group of the application. This value will be use as: - docker image repo - labeling resources"
								+ System.lineSeparator() + "" + System.lineSeparator() + " *  **Returns:**"
								+ System.lineSeparator() + "    " + System.lineSeparator()
								+ "     *  The specified group name.",
						true, "io.dekorate.kubernetes.annotation.KubernetesApplication", null,
						"group()Ljava/lang/String;", 0, null),

				p(null, "kubernetes.deployment.target", "java.lang.String",
						"To enable the generation of OpenShift resources, you need to include OpenShift in the target platforms: `kubernetes.deployment.target=openshift`."
								+ System.lineSeparator() + ""
								+ "If you need to generate resources for both platforms (vanilla Kubernetes and OpenShift), then you need to include both (coma separated)."
								+ System.lineSeparator() + "" + "`kubernetes.deployment.target=kubernetes, openshift`.",
						true, null, null, null, 0, "kubernetes"),
				p(null, "kubernetes.registry", "java.lang.String", "Specify the docker registry.", true, null, null,
						null, 0, null));

		assertPropertiesDuplicate(info);

		assertHints(info,

				h("io.dekorate.kubernetes.annotation.Protocol", null, true,
						"io.dekorate.kubernetes.annotation.Protocol", vh("TCP", null, null), //
						vh("UDP", null, null)));

		assertHintsDuplicate(info);

	}

	@Test
	public void testOpenshift() throws Exception {
		Module module = createMavenModule("kubernetes", new File("projects/maven/kubernetes"));
		MicroProfileProjectInfo info = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtils.getInstance(), DocumentFormat.Markdown);

		assertProperties(info,

				// io.dekorate.openshift.annotation.OpenshiftApplication
				p(null, "openshift.name", "java.lang.String",
						"The name of the application. This value will be used for naming Kubernetes resources like: - Deployment - Service and so on ... If no value is specified it will attempt to determine the name using the following rules: If its a maven/gradle project use the artifact id. Else if its a bazel project use the name. Else if the system property app.name is present it will be used. Else find the project root folder and use its name (root folder detection is done by moving to the parent folder until .git is found)."
								+ System.lineSeparator() + "" + System.lineSeparator() + " *  **Returns:**"
								+ System.lineSeparator() + "    " + System.lineSeparator()
								+ "     *  The specified application name.",
						true, "io.dekorate.openshift.annotation.OpenshiftApplication", null, "name()Ljava/lang/String;",
						0, null),

				p(null, "openshift.readiness-probe.initial-delay-seconds", "int",
						"The amount of time to wait in seconds before starting to probe." + System.lineSeparator() +
								System.lineSeparator() +
								" *  **Returns:**" + System.lineSeparator() +
								"    " + System.lineSeparator() +
								"     *  The initial delay.", true,
						"io.dekorate.kubernetes.annotation.Probe", null, "initialDelaySeconds()I", 0, "0"),

				p(null, "openshift.annotations[*].key", "java.lang.String", null, true,
						"io.dekorate.kubernetes.annotation.Annotation", null, "key()Ljava/lang/String;", 0, null),

				p(null, "openshift.init-containers[*].ports[*].protocol", "io.dekorate.kubernetes.annotation.Protocol",
						null, true, "io.dekorate.kubernetes.annotation.Port", null,
						"protocol()Lio/dekorate/kubernetes/annotation/Protocol;", 0, "TCP"),

				p(null, "openshift.group", "java.lang.String",
						"The group of the application. This value will be use as: - docker image repo - labeling resources"
								+ System.lineSeparator() + "" + System.lineSeparator() + " *  **Returns:**"
								+ System.lineSeparator() + "    " + System.lineSeparator()
								+ "     *  The specified group name.",
						true, "io.dekorate.openshift.annotation.OpenshiftApplication", null,
						"group()Ljava/lang/String;", 0, null),

				p(null, "openshift.registry", "java.lang.String", "Specify the docker registry.", true, null, null,
						null, 0, null));

		assertPropertiesDuplicate(info);

		assertHints(info,

				h("io.dekorate.kubernetes.annotation.Protocol", null, true,
						"io.dekorate.kubernetes.annotation.Protocol", vh("TCP", null, null), //
						vh("UDP", null, null)));

		assertHintsDuplicate(info);

	}
}
