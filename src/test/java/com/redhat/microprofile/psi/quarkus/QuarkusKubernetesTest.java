/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.microprofile.psi.quarkus;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.junit.Test;

import static com.redhat.devtools.intellij.lsp4mp4ij.psi.core.MicroProfileAssert.*;

/**
 * Quarkus Kubernetes properties test.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/QuarkusKubernetesTest.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.test/src/main/java/com/redhat/microprofile/jdt/core/QuarkusKubernetesTest.java</a>
 */
public class QuarkusKubernetesTest extends QuarkusMavenModuleImportingTestCase {

    @Test
    public void testKubernetes() throws Exception {
        Module module = loadMavenProject(QuarkusMavenProjectName.kubernetes, true);
        MicroProfileProjectInfo info = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(myProject), DocumentFormat.Markdown, new EmptyProgressIndicator());

        assertProperties(info,

                // io.dekorate.kubernetes.annotation.KubernetesApplication
                p(null, "kubernetes.name", "java.lang.String",
                        """
                                The name of the application. This value will be used for naming Kubernetes resources like: - Deployment - Service and so on ... If no value is specified it will attempt to determine the name using the following rules: If its a maven/gradle project use the artifact id. Else if its a bazel project use the name. Else if the system property app.name is present it will be used. Else find the project root folder and use its name (root folder detection is done by moving to the parent folder until .git is found).

                                * **Returns:**
                                  * The specified application name.""",
                        true, "io.dekorate.kubernetes.annotation.KubernetesApplication", null, "name()Ljava/lang/String;", 0, null),

                p(null, "kubernetes.readiness-probe.initial-delay-seconds", "int", """
                                The amount of time to wait in seconds before starting to probe.

                                * **Returns:**
                                  * The initial delay.""",
                        true, "io.dekorate.kubernetes.annotation.Probe", null, "initialDelaySeconds()I", 0, "0"),

                p(null, "kubernetes.annotations[*].key", "java.lang.String", null, true,
                        "io.dekorate.kubernetes.annotation.Annotation", null, "key()Ljava/lang/String;", 0, null),

                p(null, "kubernetes.init-containers[*].ports[*].protocol", "io.dekorate.kubernetes.annotation.Protocol", null,
                        true, "io.dekorate.kubernetes.annotation.Port", null,
                        "protocol()Lio/dekorate/kubernetes/annotation/Protocol;", 0, "TCP"),

                p(null, "kubernetes.deployment.target", "java.lang.String",
                        """
                                To enable the generation of OpenShift resources, you need to include OpenShift in the target platforms: `kubernetes.deployment.target=openshift`.
                                If you need to generate resources for both platforms (vanilla Kubernetes and OpenShift), then you need to include both (comma-separated).
                                `kubernetes.deployment.target=kubernetes, openshift`.""",
                        true, null, null, null, 0, "kubernetes"),
                p(null, "kubernetes.registry", "java.lang.String", "Specify the docker registry.", true, null, null, null, 0,
                        null));

        assertPropertiesDuplicate(info);

        assertHints(info,

                h("io.dekorate.kubernetes.annotation.Protocol", null, true, "io.dekorate.kubernetes.annotation.Protocol",
                        vh("TCP", null, null), //
                        vh("UDP", null, null)));

        assertHintsDuplicate(info);

    }

    @Test
    public void testOpenshift() throws Exception {
        Module module = loadMavenProject(QuarkusMavenProjectName.kubernetes, true);
        MicroProfileProjectInfo info = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(myProject), DocumentFormat.Markdown, new EmptyProgressIndicator());

        assertProperties(info,
                // io.dekorate.openshift.annotation.OpenshiftApplication
                p(null, "openshift.name", "java.lang.String",
                        """
                                The name of the application. This value will be used for naming Kubernetes resources like: - Deployment - Service and so on ... If no value is specified it will attempt to determine the name using the following rules: If its a maven/gradle project use the artifact id. Else if its a bazel project use the name. Else if the system property app.name is present it will be used. Else find the project root folder and use its name (root folder detection is done by moving to the parent folder until .git is found).

                                * **Returns:**
                                  * The specified application name.""",
                        true, "io.dekorate.openshift.annotation.OpenshiftApplication", null, "name()Ljava/lang/String;", 0, null),

                p(null, "openshift.readiness-probe.initial-delay-seconds", "int", """
                                The amount of time to wait in seconds before starting to probe.

                                * **Returns:**
                                  * The initial delay.""", true,
                        "io.dekorate.kubernetes.annotation.Probe", null, "initialDelaySeconds()I", 0, "0"),

                p(null, "openshift.annotations[*].key", "java.lang.String", null, true,
                        "io.dekorate.kubernetes.annotation.Annotation", null, "key()Ljava/lang/String;", 0, null),

                p(null, "openshift.init-containers[*].ports[*].protocol", "io.dekorate.kubernetes.annotation.Protocol", null,
                        true, "io.dekorate.kubernetes.annotation.Port", null,
                        "protocol()Lio/dekorate/kubernetes/annotation/Protocol;", 0, "TCP"),

                p(null, "openshift.registry", "java.lang.String", "Specify the docker registry.", true, null, null, null, 0,
                        null));

        assertPropertiesDuplicate(info);

        assertHints(info,
                h("io.dekorate.kubernetes.annotation.Protocol", null, true, "io.dekorate.kubernetes.annotation.Protocol",
                        vh("TCP", null, null), //
                        vh("UDP", null, null)));

        assertHintsDuplicate(info);

    }

    @Test
    public void testS2i() throws Exception {
        Module module = loadMavenProject(QuarkusMavenProjectName.kubernetes, true);
        MicroProfileProjectInfo info = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(myProject), DocumentFormat.Markdown, new EmptyProgressIndicator());

        assertProperties(info,
                // io.dekorate.s2i.annotation.S2iBuild
                p(null, "s2i.docker-file", "java.lang.String", """
                                The relative path of the Dockerfile, from the module root.

                                * **Returns:**
                                  * The relative path.""", true, "io.dekorate.s2i.annotation.S2iBuild", null,
                        "dockerFile()Ljava/lang/String;", 0, "Dockerfile"),

                p(null, "s2i.group", "java.lang.String",
                        """
                                The group of the application. This value will be use as image user.

                                * **Returns:**
                                  * The specified group name.""",
                        true, "io.dekorate.s2i.annotation.S2iBuild", null, "group()Ljava/lang/String;", 0,
                        null),

                p("quarkus-container-image-s2i", "quarkus.s2i.jar-directory", "java.lang.String",
                        """
                                The directory where the jar is added during the assemble phase.
                                This is dependent on the S2I image and should be supplied if a non default image is used.""",
                        true, "io.quarkus.container.image.s2i.deployment.S2iConfig", "jarDirectory", null, 1,
                        "/deployments/"));

        assertPropertiesDuplicate(info);

        assertHintsDuplicate(info);

    }

    @Test
    public void testDocker() throws Exception {
        Module module = loadMavenProject(QuarkusMavenProjectName.kubernetes, true);
        MicroProfileProjectInfo info = PropertiesManager.getInstance().getMicroProfileProjectInfo(module, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, PsiUtilsLSImpl.getInstance(myProject), DocumentFormat.Markdown, new EmptyProgressIndicator());
        String description = """
                            The relative path of the Dockerfile, from the module root.

                            * **Returns:**
                              * The relative path.""";
        assertProperties(info,

                // io.dekorate.docker.annotation.DockerBuild
                p(null, "docker.docker-file", "java.lang.String", description, true, "io.dekorate.docker.annotation.DockerBuild", null,
                        "dockerFile()Ljava/lang/String;", 0, "Dockerfile"));

        assertPropertiesDuplicate(info);

        assertHintsDuplicate(info);

    }
}
