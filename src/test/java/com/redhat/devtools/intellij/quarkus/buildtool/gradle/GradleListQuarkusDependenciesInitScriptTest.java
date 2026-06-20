/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.buildtool.gradle;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

/**
 * Verifies the Gradle init script used by {@link AbstractGradleToolDelegate} to collect Quarkus
 * deployment dependencies (the {@code listQuarkusDependencies} task) actually runs on Gradle 9,
 * where the {@code -c}/{@code --settings-file} option and {@code rootProject.buildFileName} that the
 * previous implementation relied on were removed.
 * <p>
 * These tests intentionally do <b>not</b> apply the Quarkus Gradle plugin: the plugin versions pinned
 * by the other Gradle test fixtures (1.x/2.x) are not Gradle 9 compatible and would mask whether the
 * init-script mechanism itself works. Instead they drive minimal {@code java} projects through the
 * Tooling API with the real init script and assert that both the binary and the sources artifacts of
 * a deployment dependency are resolved and written in the format the delegate expects.
 *
 * @see <a href="https://github.com/redhat-developer/intellij-quarkus/issues/1512">redhat-developer/intellij-quarkus#1512</a>
 */
@RunWith(Parameterized.class)
public class GradleListQuarkusDependenciesInitScriptTest {

    // commons-lang3 publishes a -sources.jar, so both the binary and sources resolution paths are covered.
    private static final String DEPLOYMENT_ID = "org.apache.commons:commons-lang3:3.14.0";
    private static final String BINARY_JAR = "commons-lang3-3.14.0.jar";
    private static final String SOURCES_JAR = "commons-lang3-3.14.0-sources.jar";

    @Parameterized.Parameters(name = "Gradle {0}")
    public static Collection<String> gradleVersions() {
        // 8.6 is the version the rest of the suite uses (regression guard); 9.6.0 is the version that
        // dropped the -c option this fix is about.
        return Arrays.asList("8.6", "9.6.0");
    }

    @Parameterized.Parameter
    public String gradleVersion;

    /**
     * Single-module project: the analyzed module is the root project.
     */
    @Test
    public void resolvesDeploymentArtifactsForSingleModuleProject() throws Exception {
        Path projectDir = Files.createTempDirectory("quarkus-init-single");
        try {
            Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'single-module'\n");
            Files.writeString(projectDir.resolve("build.gradle"),
                    "plugins { id 'java' }\n" +
                    "repositories { mavenCentral() }\n");

            runAndAssert(projectDir, projectDir);
        } finally {
            deleteRecursively(projectDir);
        }
    }

    /**
     * Multi-module project: the analyzed module is a subproject, and the repositories needed to resolve
     * the deployment artifacts are declared <b>only</b> in that subproject (not in the root). This fails
     * if the init script attaches the {@code quarkusDeployment} configuration to the root project instead
     * of the analyzed module.
     */
    @Test
    public void resolvesDeploymentArtifactsForSubProjectWithOwnRepositories() throws Exception {
        Path rootDir = Files.createTempDirectory("quarkus-init-multi");
        try {
            Files.writeString(rootDir.resolve("settings.gradle"),
                    "rootProject.name = 'multi-module'\n" +
                    "include 'app'\n");
            // Root has no repositories on purpose.
            Files.writeString(rootDir.resolve("build.gradle"), "");

            Path appDir = Files.createDirectories(rootDir.resolve("app"));
            Files.writeString(appDir.resolve("build.gradle"),
                    "plugins { id 'java' }\n" +
                    "repositories { mavenCentral() }\n");

            runAndAssert(rootDir, appDir);
        } finally {
            deleteRecursively(rootDir);
        }
    }

    /**
     * Generate the real init script for {@code moduleDir}, run {@code listQuarkusDependencies} from
     * {@code projectDir} on the parameterized Gradle version, and assert both jars are resolved.
     */
    private void runAndAssert(Path projectDir, Path moduleDir) throws Exception {
        Path outputPath = Files.createTempFile("quarkus-deps", ".txt");
        Path initScriptFile = Files.createTempFile("quarkus-init", ".gradle");
        try {
            String initScript = new GradleGroovyToolDelegate()
                    .buildInitScript(outputPath, Collections.singleton(DEPLOYMENT_ID), moduleDir.toString());
            Files.writeString(initScriptFile, initScript);

            try (ProjectConnection connection = GradleConnector.newConnector()
                    .forProjectDirectory(projectDir.toFile())
                    .useGradleVersion(gradleVersion)
                    .connect()) {
                connection.newBuild()
                        .forTasks("listQuarkusDependencies")
                        .withArguments("--init-script", initScriptFile.toString(), "-q")
                        .run();
            }

            List<String> lines = Files.readAllLines(outputPath, StandardCharsets.UTF_8);
            assertTrue("listQuarkusDependencies produced no usable output on Gradle " + gradleVersion + ": " + lines,
                    lines.size() >= 4);
            assertTrue("Binary jar not resolved on Gradle " + gradleVersion + ": " + lines,
                    lines.stream().anyMatch(line -> line.endsWith(BINARY_JAR)));
            assertTrue("Sources jar not resolved on Gradle " + gradleVersion + ": " + lines,
                    lines.stream().anyMatch(line -> line.endsWith(SOURCES_JAR)));
        } finally {
            Files.deleteIfExists(initScriptFile);
            Files.deleteIfExists(outputPath);
        }
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (Stream<Path> paths = Files.walk(dir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }
}
