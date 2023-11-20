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
import com.intellij.execution.dashboard.RunDashboardCustomizer;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfiguration;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunContext;
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
    public boolean updatePresentation(@NotNull PresentationData presentation, @NotNull RunDashboardRunConfigurationNode node) {
        if (!(node.getConfigurationSettings().getConfiguration() instanceof QuarkusRunConfiguration)) {
            return false;
        }
        node.putUserData(RunDashboardCustomizer.NODE_LINKS, null);
        RunContentDescriptor descriptor = node.getDescriptor();
        if (descriptor != null) {
            ProcessHandler processHandler =  descriptor.getProcessHandler();
            if (processHandler != null && !processHandler.isProcessTerminated()) {
                // The Quarkus run configuration is running
                QuarkusRunConfiguration quarkusRunConfiguration = (QuarkusRunConfiguration) node.getConfigurationSettings().getConfiguration();
                if (QuarkusModuleUtil.isQuarkusWebAppModule(quarkusRunConfiguration.getModule())) {
                    // It is a Web application, add links for:
                    // - Opening quarkus application in a browser
                    // - Opening DevUI in a browser
                    QuarkusRunContext runContext = new QuarkusRunContext(quarkusRunConfiguration.getModule());
                    // Add application Url as hyperlink
                    String applicationUrl = runContext.getApplicationURL();
                    String applicationLabel = applicationUrl;
                    presentation.addText(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    presentation.addText(applicationLabel, SimpleTextAttributes.LINK_ATTRIBUTES);

                    // Add DevUI Url as hyperlink
                    String devUIUrl = runContext.getDevUIURL();
                    String devUILabel = "Dev UI";
                    presentation.addText(" - ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    presentation.addText(devUILabel, SimpleTextAttributes.LINK_ATTRIBUTES);

                    Map<Object, Object> links = new HashMap<>();
                    links.put(applicationLabel, new SimpleColoredComponent.BrowserLauncherTag(applicationUrl));
                    links.put(devUILabel, new SimpleColoredComponent.BrowserLauncherTag(devUIUrl));
                    node.putUserData(RunDashboardCustomizer.NODE_LINKS, links);
                }
            }
        }
        return true;
    }

}
