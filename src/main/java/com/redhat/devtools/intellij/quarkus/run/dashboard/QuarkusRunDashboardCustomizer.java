/*******************************************************************************
 * Copyright (c) 2023 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.run.dashboard;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.dashboard.RunDashboardCustomizationBuilder;
import com.intellij.execution.dashboard.RunDashboardCustomizer;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.module.Module;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProject;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfiguration;
import com.redhat.devtools.intellij.quarkus.telemetry.TelemetryEventName;
import com.redhat.devtools.intellij.quarkus.telemetry.TelemetryManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Dashboard customizer for Quarkus to provide:
 *
 * <ul>
 *     <li>Open quarkus application in a browser.</li>
 *     <li>Open quarkus DevUI in a browser.</li>
 * </ul>
 */
public class QuarkusRunDashboardCustomizer extends RunDashboardCustomizer {

    @Override
    public boolean isApplicable(@NotNull RunnerAndConfigurationSettings settings, @Nullable RunContentDescriptor descriptor) {
        return settings.getConfiguration() instanceof QuarkusRunConfiguration;
    }

    @Override
    public boolean updatePresentation(@NotNull RunDashboardCustomizationBuilder customizationBuilder,
                                      @NotNull RunnerAndConfigurationSettings settings,
                                      @Nullable RunContentDescriptor descriptor) {
        QuarkusRunConfiguration quarkusRunConfiguration = settings.getConfiguration() instanceof QuarkusRunConfiguration config ? config : null;
        if (descriptor == null || quarkusRunConfiguration == null) {
            return false;
        }
        ProcessHandler processHandler = descriptor.getProcessHandler();
        if (processHandler != null && !processHandler.isProcessTerminated()) {
            // The Quarkus run configuration is running
            Module module = quarkusRunConfiguration.getModule();
            if (QuarkusModuleUtil.isQuarkusWebAppModule(module)) {
                PsiMicroProfileProject mpProject = PsiMicroProfileProjectManager.getInstance(module.getProject()).getMicroProfileProject(module);

                // It is a Web application, add links for:
                // - Opening quarkus application in a browser
                // - Opening DevUI in a browser
                // Add application Url as hyperlink
                String applicationUrl = QuarkusModuleUtil.getApplicationUrl(mpProject);
                customizationBuilder.addLink(applicationUrl, new SimpleColoredComponent.BrowserLauncherTag(applicationUrl) {
                    @Override
                    public void run() {
                        // Open Quarkus application in a Web Browser
                        super.run();
                        // Send "ui-openApplication" telemetry event
                        TelemetryManager.instance().send(TelemetryEventName.UI_OPEN_APPLICATION);
                    }
                });

                // Add DevUI Url as hyperlink
                String devUIUrl = QuarkusModuleUtil.getDevUIUrl(mpProject);
                String devUILabel = "Dev UI";
                customizationBuilder.addLink(devUILabel, new SimpleColoredComponent.BrowserLauncherTag(devUIUrl) {
                    @Override
                    public void run() {
                        // Open DevUI in a Web Browser
                        super.run();
                        // Send "ui-openDevUI" telemetry event
                        TelemetryManager.instance().send(TelemetryEventName.UI_OPEN_DEV_UI);
                    }
                });
            }
        }
        return true;
    }

    @Override
    public boolean updatePresentation(@NotNull PresentationData presentation, @NotNull RunDashboardRunConfigurationNode node) {
        if (!(node.getConfigurationSettings().getConfiguration() instanceof QuarkusRunConfiguration)) {
            return false;
        }
        RunContentDescriptor descriptor = node.getDescriptor();
        if (descriptor != null) {
            ProcessHandler processHandler = descriptor.getProcessHandler();
            if (processHandler != null && !processHandler.isProcessTerminated()) {
                // The Quarkus run configuration is running
                QuarkusRunConfiguration quarkusRunConfiguration = (QuarkusRunConfiguration) node.getConfigurationSettings().getConfiguration();
                Module module = quarkusRunConfiguration.getModule();
                if (QuarkusModuleUtil.isQuarkusWebAppModule(module)) {
                    PsiMicroProfileProject mpProject = PsiMicroProfileProjectManager.getInstance(module.getProject()).getMicroProfileProject(module);

                    // It is a Web application, add links for:
                    // - Opening quarkus application in a browser
                    // - Opening DevUI in a browser
                    // Add application Url as hyperlink
                    String applicationUrl = QuarkusModuleUtil.getApplicationUrl(mpProject);
                    presentation.addText(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    presentation.addText(applicationUrl, SimpleTextAttributes.LINK_ATTRIBUTES);

                    // Add DevUI Url as hyperlink
                    String devUIUrl = QuarkusModuleUtil.getDevUIUrl(mpProject);
                    String devUILabel = "Dev UI";
                    presentation.addText(" - ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    presentation.addText(devUILabel, SimpleTextAttributes.LINK_ATTRIBUTES);

                    Map<Object, Object> links = new HashMap<>();
                    links.put(applicationUrl, new SimpleColoredComponent.BrowserLauncherTag(applicationUrl) {
                        @Override
                        public void run() {
                            // Open Quarkus application in a Web Browser
                            super.run();
                            // Send "ui-openApplication" telemetry event
                            TelemetryManager.instance().send(TelemetryEventName.UI_OPEN_APPLICATION);
                        }
                    });
                    links.put(devUILabel, new SimpleColoredComponent.BrowserLauncherTag(devUIUrl) {
                        @Override
                        public void run() {
                            // Open DevUI in a Web Browser
                            super.run();
                            // Send "ui-openDevUI" telemetry event
                            TelemetryManager.instance().send(TelemetryEventName.UI_OPEN_DEV_UI);
                        }
                    });
                    //node.putUserData(RunDashboardCustomizer.NODE_LINKS, links);
                }
            }
        }
        return true;
    }

}
