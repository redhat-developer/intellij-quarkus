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
package com.redhat.devtools.intellij.qute.psi.internal.extensions.webbundler;

import com.intellij.java.library.JavaLibraryUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProject;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.qute.psi.template.rootpath.ITemplateRootPathProvider;
import com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils;
import com.redhat.qute.commons.TemplateRootPath;
import com.redhat.qute.commons.config.webbundler.WebBundlerConfig;

import java.util.List;
import java.util.Set;

import static com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils.resolveRelativePath;
import static com.redhat.qute.commons.config.webbundler.WebBundlerConfig.WEB_DIR;
import static com.redhat.qute.commons.config.webbundler.WebBundlerConfig.WEB_ROOT;


/**
 * Web Bundler template root path provider for Web Bundler project.
 */
public class WebBundlerTemplateRootPathProvider implements ITemplateRootPathProvider {

    private static final String ORIGIN = WebBundlerConfig.EXTENSION_ID;

    @Override
    public boolean isApplicable(Module project) {
        return JavaLibraryUtil.hasAnyLibraryJar(project, WebBundlerJavaConstants.WEB_BUNDLER_MAVEN_COORS);
    }

    @Override
    public void collectTemplateRootPaths(Module javaProject, List<TemplateRootPath> rootPaths) {
        VirtualFile moduleDir = QuarkusModuleUtil.getModuleDirPath(javaProject);
        if (moduleDir != null) {
            PsiMicroProfileProject mpProject = PsiMicroProfileProjectManager.getInstance(javaProject.getProject()).getMicroProfileProject(javaProject);
            String webDir = mpProject.getProperty(WEB_DIR);
            String webRoot = mpProject.getProperty(WEB_ROOT);

            // web/templates
            String webTemplateBaseDir = resolveRelativePath(moduleDir,
                    webDir,
                    "templates").toASCIIString();
            rootPaths.add(new TemplateRootPath(webTemplateBaseDir, ORIGIN));

            // src/main/resorces/web/templates
            var resourcesDir = PsiQuteProjectUtils.findBestResourcesDir(javaProject, webRoot);
            if (resourcesDir != null) {
                String resourcesWebTemplateBaseDir = resolveRelativePath(resourcesDir,
                        webRoot,
                        "templates").toASCIIString();
                rootPaths.add(new TemplateRootPath(resourcesWebTemplateBaseDir, ORIGIN));

                // Qute tags
                // See
                // https://docs.quarkiverse.io/quarkus-web-bundler/dev/config-reference.html#quarkus-web-bundler_quarkus-web-bundler-bundle-bundle-qute-tags
                // quarkus.web-bundler.bundle."bundle".qute-tags
                Set<String> mathingSegments = mpProject.getMatchingSegments(WebBundlerConfig.QUTE_TAGS.getName());
                for (String segment : mathingSegments) {
                    String tagsDir = resolveRelativePath(resourcesDir,
                            webRoot,
                            segment).toASCIIString();
                    rootPaths.add(new TemplateRootPath(tagsDir, true, false, ORIGIN));
                }
            }

        }
    }

}
