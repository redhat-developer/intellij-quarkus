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

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class QuarkusPluginIssueReporter extends ErrorReportSubmitter {

    private static final String GITHUB_ISSUE_BASE_URL =
            "https://github.com/redhat-developer/intellij-quarkus/issues/new?template=error_report.yml";

    private static final String SYSTEM_INFO_TEMPLATE = """
            | Attribute                  | Value |
            |----------------------------|-------|
            | **OS**                     |   %s  |
            | **IDE**                    |   %s  |
            | **JDK**                    |   %s  |
            | **Quarkus Tools Version**  |   %s  |
            """;

    public static final int MAX_URL_LENGTH = 8191;

    final String ideVersion;
    String pluginVersion;
    final String operatingSystem;
    final String jdkVersion;

    public QuarkusPluginIssueReporter(){
        ApplicationInfo applicationInfo = ApplicationInfo.getInstance();
        operatingSystem = SystemInfo.getOsNameAndVersion() + "-" + SystemInfo.OS_ARCH;
        ideVersion = String.join(" ", applicationInfo.getVersionName(),
                applicationInfo.getFullVersion(), applicationInfo.getBuild().asString() );
        jdkVersion = String.join(" ", System.getProperty("java.vm.name"),
                SystemInfo.JAVA_VERSION, SystemInfo.JAVA_RUNTIME_VERSION, SystemInfo.JAVA_VENDOR );
    }

    @Override
    public @NlsActions.ActionText @NotNull String getReportActionText() {
        return "Create GitHub Issue";
    }

    @Override
    public boolean submit(IdeaLoggingEvent @NotNull [] events,
                          @Nullable String userMessage,
                          @NotNull Component parentComponent,
                          @NotNull Consumer<? super SubmittedReportInfo> consumer) {
        try {
            IdeaLoggingEvent event = events[0];
            String url = generateUrl(event.getThrowableText(), userMessage);
            BrowserUtil.browse(url);
            consumer.consume(new SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE));
        } catch (Exception e) {
            consumer.consume(new SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.FAILED));
            return false;
        }
        return true;
    }

    String generateUrl(String throwableText, @Nullable String userMessage) {
        PluginDescriptor pluginDescriptor = getPluginDescriptor();
        pluginVersion = pluginDescriptor!=null? pluginDescriptor.getVersion() : "Unknown";
        String systemInfo = encode(formatSystemInfo());
        String titleLine = encode(throwableText.lines()
                .findFirst().orElse("Unable to get exception message"));
        String url = GITHUB_ISSUE_BASE_URL +"&title=" + titleLine + "&info=" + systemInfo + "&stacktrace=";
        int available = MAX_URL_LENGTH - url.length();
        String stackTrace = truncateStacktrace(throwableText, userMessage, available);
        return url + stackTrace;
    }

    private String truncateStacktrace(String throwableText, @Nullable String userMessage, int available) {
        String stackTrace = encode(formatStacktrace(throwableText,
                Optional.ofNullable(userMessage).orElse("User didn't provide any message")));
        if (stackTrace.length() > available) {
            stackTrace = stackTrace.substring(0, available-1);
        }
        return stackTrace;
    }

    private String formatSystemInfo() {
        return SYSTEM_INFO_TEMPLATE.formatted(operatingSystem, ideVersion, jdkVersion, pluginVersion);
    }

    private String formatStacktrace(String stackTrace, String userMessage) {
        return """
                 **User message**: *%s*
                 **Stacktrace:**
                 ```
                 %s
                 ```
                """.formatted(userMessage, stackTrace);
    }

    private String encode(final String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

}