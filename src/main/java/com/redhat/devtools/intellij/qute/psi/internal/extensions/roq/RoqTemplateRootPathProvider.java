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
package com.redhat.devtools.intellij.qute.psi.internal.extensions.roq;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProject;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.qute.psi.template.rootpath.ITemplateRootPathProvider;
import com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils;
import com.redhat.qute.commons.TemplateRootPath;
import com.redhat.qute.commons.config.roq.RoqConfig;

import java.util.List;

import static com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils.resolveRelativePath;
import static com.redhat.qute.commons.config.roq.RoqConfig.ROQ_DIR;
import static com.redhat.qute.commons.config.roq.RoqConfig.SITE_CONTENT_DIR;

/**
 * Roq template root path provider for Roq project.
 */
public class RoqTemplateRootPathProvider implements ITemplateRootPathProvider {

    private static final String ORIGIN = RoqConfig.EXTENSION_ID;

    @Override
    public boolean isApplicable(Module javaProject) {
        return RoqUtils.isRoqProject(javaProject);
    }

    @Override
    public void collectTemplateRootPaths(Module javaProject, List<TemplateRootPath> rootPaths) {
        VirtualFile moduleDir = QuarkusModuleUtil.getModuleDirPath(javaProject);
        PsiMicroProfileProject mpProject = PsiMicroProfileProjectManager.getInstance(javaProject.getProject()).getMicroProfileProject(javaProject);

        String roqDir = mpProject.getProperty(ROQ_DIR);
        String contentDir = mpProject.getProperty(SITE_CONTENT_DIR);

        if (moduleDir != null) {
            // templates
            String templateBaseDir = resolveRelativePath(moduleDir,
                    roqDir,
                    "templates").toASCIIString();
            rootPaths.add(new TemplateRootPath(templateBaseDir, ORIGIN));
            // content
            String contentBaseDir = resolveRelativePath(moduleDir,
                    roqDir,
                    contentDir).toASCIIString();
            rootPaths.add(new TemplateRootPath(contentBaseDir, ORIGIN));
        }
        // src/main/resources/content
        VirtualFile resourcesContentDir = PsiQuteProjectUtils.findBestResourcesDir(javaProject, contentDir);
        if (resourcesContentDir != null) {
            String contentBaseDir = resolveRelativePath(resourcesContentDir,
                    contentDir).toASCIIString();
            rootPaths.add(new TemplateRootPath(contentBaseDir, ORIGIN));
        }
    }

}
