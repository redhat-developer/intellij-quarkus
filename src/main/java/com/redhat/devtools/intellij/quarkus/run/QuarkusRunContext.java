/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.run;

import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProject;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;

import javax.swing.JComponent;

public class QuarkusRunContext {
    private final PsiMicroProfileProject project;

    public QuarkusRunContext(Module module) {
        this.project = PsiMicroProfileProjectManager.getInstance(module.getProject()).getJDTMicroProfileProject(module);
    }

    protected static QuarkusRunContext getContext(AnActionEvent e) {
        Project project = PlatformDataKeys.PROJECT.getData(e.getDataContext());
        RunContentManager contentManager = RunContentManager.getInstance(project);
        RunContentDescriptor selectedContent = contentManager.getSelectedContent();
        JComponent component = selectedContent.getComponent();
        return (component == null) ? null : (QuarkusRunContext) component.getClientProperty(QuarkusConstants.QUARKUS_RUN_CONTEXT_KEY);
    }

    private String getProperty(String name, String defaultValue) {
        String val = project.getProperty(name, defaultValue);
        return project.getProperty("%dev." + name, val);
    }

    private String normalize(String path) {
        StringBuilder builder = new StringBuilder(path);
        if (builder.length() == 0 || builder.charAt(0) != '/') {
            builder.insert(0, '/');
        }
        if (builder.charAt(builder.length() - 1) != '/') {
            builder.append('/');
        }
        return builder.toString();
    }

    private int getPort() {
        int port = project.getPropertyAsInteger("quarkus.http.port", 8080);
        port = project.getPropertyAsInteger("%dev.quarkus.http.port", port);
        return port;
    }

    public String getDevUIURL() {
        int port = getPort();
        String path = getProperty("quarkus.http.non-application-root-path", "q");
        if (!path.startsWith("/")) {
            String rootPath = getProperty("quarkus.http.root-path", "/");
            path = normalize(rootPath) + path;
        }
        path = normalize(path);
        return "http://localhost:" + port + path + "dev";
    }

    public String getApplicationURL() {
        int port = getPort();
        String path = getProperty("quarkus.http.root-path", "/");
        path = normalize(path);
        return "http://localhost:" + port + path;
    }
}
