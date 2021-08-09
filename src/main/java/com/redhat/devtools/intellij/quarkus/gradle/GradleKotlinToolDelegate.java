/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.gradle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GradleKotlinToolDelegate extends AbstractGradleToolDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(GradleKotlinToolDelegate.class);

    private static final String QUARKUS_DOWNLOAD_TASK_DEFINITION =
            "typealias PrintWriter = java.io.PrintWriter" + System.lineSeparator() +
            "typealias FileWriter = java.io.FileWriter" + System.lineSeparator() +
            "tasks.register(\"listQuarkusDependencies\") {" + System.lineSeparator() +
            "    val writer = PrintWriter(FileWriter(\"%1$s\"))" + System.lineSeparator() +
            "    quarkusDeployment.files.forEach { it -> writer.println(it) }" + System.lineSeparator() +
            "    val componentIds = quarkusDeployment.incoming.resolutionResult.allDependencies.map { (it as ResolvedDependencyResult).selected.id }" + System.lineSeparator() +
            "    val result = dependencies.createArtifactResolutionQuery()" + System.lineSeparator() +
            "        .forComponents(componentIds)" + System.lineSeparator() +
            "        .withArtifacts(JvmLibrary::class, SourcesArtifact::class)" + System.lineSeparator() +
		    "        .execute()" + System.lineSeparator() +
            "    result.resolvedComponents.forEach { component ->" + System.lineSeparator() +
            "        val sources = component.getArtifacts(SourcesArtifact::class)" + System.lineSeparator() +
            "        sources.forEach { ar ->" + System.lineSeparator() +
            "            if (ar is ResolvedArtifactResult) {" + System.lineSeparator() +
            "                writer.println(ar.file)" + System.lineSeparator() +
            "            }" + System.lineSeparator() +
            "        }" + System.lineSeparator() +
            "    }" + System.lineSeparator() +
            "    writer.close()" + System.lineSeparator() +
            "}";

    String getScriptName() {
        return "build.gradle.kts";
    }

    String getScriptExtension() {
        return ".gradle.kts";
    }

    String formatQuarkusDependency(String id) {
        return "quarkusDeployment(\"" + id + "\")" + System.lineSeparator();
    }

    String generateTask(String path) {
        return String.format(QUARKUS_DOWNLOAD_TASK_DEFINITION, path);
    }

    String createQuarkusConfiguration() {
        return "val quarkusDeployment by configurations.creating";
    }

    @Override
    public String getDisplay() {
        return "Gradle with Kotlin DSL";
    }

    @Override
    public String asParameter() {
        return "GRADLE_KOTLIN_DSL";
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
