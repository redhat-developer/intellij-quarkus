/*******************************************************************************
 * Copyright (c) 2024 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.quarkus.error;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.redhat.devtools.intellij.quarkus.error.QuarkusPluginIssueReporter.MAX_URL_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;

public class QuarkusPluginIssueReporterTest extends BasePlatformTestCase {
    private QuarkusPluginIssueReporter reporter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        reporter = new QuarkusPluginIssueReporter();
    }

    private static final String STACKTRACE =
            """
                java.lang.NullPointerException: Cannot invoke "javax.swing.JCheckBox.setSelected(boolean)" because "platformCheckbox" is null
                at com.redhat.devtools.intellij.quarkus.projectWizard.QuarkusExtensionsStep.getComponent(QuarkusExtensionsStep.java:150)
                at com.intellij.ide.wizard.AbstractWizard.updateStep(AbstractWizard.java:483)
            """;

    public void testEncodedUrlWithExceptionAndMessage() {
        String userMessage = "I ran into this error with quarkus project wizard";
        String url = URLDecoder.decode(reporter.generateUrl(STACKTRACE, userMessage), StandardCharsets.UTF_8);
        assertThat(url).as("Stacktrace not found in decoded url").contains(STACKTRACE);
        assertThat(url).as("userMessage not found in decoded url").contains(userMessage);
        List<String> systemInfo = List.of(reporter.ideVersion, reporter.jdkVersion,
                reporter.operatingSystem, reporter.pluginVersion);
        assertThat(systemInfo).allSatisfy(attribute -> assertThat(url).contains(attribute));
    }

    public void testLongStacktraceIsTruncated() {
        String stackTrace = STACKTRACE.concat("A".repeat(MAX_URL_LENGTH));
        String url = reporter.generateUrl(stackTrace, "Hallo");
        String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);
        assertThat(decodedUrl).as("Initial Stacktrace not found in decoded url").contains(STACKTRACE);
        assertThat(url.length()).isLessThan(MAX_URL_LENGTH);
    }
}
