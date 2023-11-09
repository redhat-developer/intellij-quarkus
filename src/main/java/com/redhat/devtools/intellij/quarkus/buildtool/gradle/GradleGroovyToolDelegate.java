/*******************************************************************************
 * Copyright (c) 2019-2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.buildtool.gradle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GradleGroovyToolDelegate extends AbstractGradleToolDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(GradleGroovyToolDelegate.class);

    private static final String QUARKUS_DOWNLOAD_TASK_DEFINITION =
            "task listQuarkusDependencies() {" + System.lineSeparator() +
                    "    File f = new File('%1$s')" + System.lineSeparator() +
                    "    f.withPrintWriter('UTF8') { writer ->" + System.lineSeparator() +
                    "        configurations.quarkusDeployment.incoming.artifacts.each {" + System.lineSeparator() +
                    "            writer.println it.id.componentIdentifier" + System.lineSeparator() +
                    "            writer.println it.file" + System.lineSeparator() +
                    "        }" + System.lineSeparator() +
                    "        def componentIds = configurations.quarkusDeployment.incoming.resolutionResult.allDependencies.collect { it.selected.id }" + System.lineSeparator() +
                    "        ArtifactResolutionResult result = dependencies.createArtifactResolutionQuery()" + System.lineSeparator() +
                    "            .forComponents(componentIds)" + System.lineSeparator() +
                    "            .withArtifacts(JvmLibrary, SourcesArtifact)" + System.lineSeparator() +
                    "            .execute()" + System.lineSeparator() +
                    "        result.resolvedComponents.each { ComponentArtifactsResult component ->" + System.lineSeparator() +
                    "            Set<ArtifactResult> sources = component.getArtifacts(SourcesArtifact)" + System.lineSeparator() +
                    "            sources.each { ArtifactResult ar ->" + System.lineSeparator() +
                    "                if (ar instanceof ResolvedArtifactResult) {" + System.lineSeparator() +
                    "                    writer.println ar.id.componentIdentifier" + System.lineSeparator() +
                    "                    writer.println ar.file" + System.lineSeparator() +
                    "                }" + System.lineSeparator() +
                    "            }" + System.lineSeparator() +
                    "        }" + System.lineSeparator() +
                    "    }" + System.lineSeparator() +
                    "}";

    @Override
    String getScriptName() {
        return "build.gradle";
    }

    @Override
    String getSettingsScriptName() {
        return "settings.gradle";
    }

    @Override
    String getScriptExtension() {
        return ".gradle";
    }

    String formatQuarkusDependency(String id) {
        return "quarkusDeployment '" + id + '\'' + System.lineSeparator();
    }

    String generateTask(String path) {
        return String.format(QUARKUS_DOWNLOAD_TASK_DEFINITION, path);
    }

    String createQuarkusConfiguration() {
        return "configurations {quarkusDeployment}";
    }

    @Override
    public String getDisplay() {
        return "Gradle";
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
