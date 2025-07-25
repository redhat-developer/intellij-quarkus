/*******************************************************************************
 * Copyright (c) 2019-2025 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4mp.commons.MicroProfileJavaProjectLabelsParams;
import org.eclipse.lsp4mp.commons.ProjectLabelInfoEntry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Project label manager which provides <code>ProjectLabelInfo</code> containing
 * project labels for all projects in the workspace
 *
 * @see <a ref="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/ProjectLabelManager.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/ProjectLabelManager.java</a>
 */
public class ProjectLabelManager {

    private final @NotNull Project project;

    public static ProjectLabelManager getInstance(@NotNull Project project) {
        return project.getService(ProjectLabelManager.class);
    }

    private ProjectLabelManager(@NotNull Project project) {
        this.project = project;
    }

    /**
     * Returns project label results for all projects in the workspace
     *
     * @return project label results for all projects in the workspace
     */
    public List<ProjectLabelInfoEntry> getProjectLabelInfo(IPsiUtils utils) {
        List<ProjectLabelInfoEntry> results = new ArrayList<>();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            ProjectLabelInfoEntry info = getProjectLabelInfo(module, null, utils);
            if (info != null) {
                results.add(info);
            }
        }
        return results;
    }

    /**
     * Returns project label results for the given Eclipse project.
     *
     * @param project Eclipse project.
     * @param types   the Java type list to check.
     * @return project label results for the given Eclipse project.
     */
    private ProjectLabelInfoEntry getProjectLabelInfo(Module project, List<String> types, IPsiUtils utils) {
        String uri = PsiUtilsLSImpl.getProjectURI(project);
        if (uri != null) {
            return new ProjectLabelInfoEntry(uri, project.getName(), getProjectLabels(project, types, utils));
        }
        return null;
    }

    /**
     * Returns project label results for the given Java file uri parameter.
     *
     * @param params the Java file uri parameter.
     * @param utils  the JDT utilities.
     * @return project label results for the given Java file uri parameter.
     */
    public ProjectLabelInfoEntry getProjectLabelInfo(MicroProfileJavaProjectLabelsParams params, IPsiUtils utils) {
        try {
            VirtualFile file = utils.findFile(params.getUri());
            if (file == null) {
                // The uri doesn't belong to an IDEA project
                return ProjectLabelInfoEntry.EMPTY_PROJECT_INFO;
            }
            Module module = utils.getModule(file);
            if (module == null) {
                return ProjectLabelInfoEntry.EMPTY_PROJECT_INFO;
            }
            return getProjectLabelInfo(module, params.getTypes(), utils);
        } catch (IOException e) {
            return ProjectLabelInfoEntry.EMPTY_PROJECT_INFO;
        }
    }

    /**
     * Returns the project labels for the given project.
     *
     * @param project the Eclipse project.
     * @param types   the Java type list to check.
     * @return the project labels for the given project.
     */
    private List<String> getProjectLabels(Module project, List<String> types, IPsiUtils utils) {
        // Update labels by using the
        // "com.redhat.microprofile.jdt.core.projectLabelProviders" extension point (ex
        // : "maven", "gradle", "quarkus", "microprofile").
        List<String> projectLabels = new ArrayList<>();
        List<IProjectLabelProvider> definitions = IProjectLabelProvider.EP_NAME.getExtensionList();
        for (IProjectLabelProvider definition : definitions) {
            projectLabels.addAll(definition.getProjectLabels(project));
        }
        // Update labels by checking if some Java types are in the classpath of the Java
        // project.
        if (types != null) {
            for (String type : types) {
                if (utils.findClass(project, type) != null) {
                    projectLabels.add(type);
                }
            }
        }

        return projectLabels;
    }

}
