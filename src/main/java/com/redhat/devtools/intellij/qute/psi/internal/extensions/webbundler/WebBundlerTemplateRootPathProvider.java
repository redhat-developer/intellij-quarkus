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
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.qute.psi.template.rootpath.ITemplateRootPathProvider;
import com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.qute.commons.TemplateRootPath;

import java.util.List;


/**
 * Web Bundler template root path provider for Web Bundler project.
 */
public class WebBundlerTemplateRootPathProvider implements ITemplateRootPathProvider {

    private static final String ORIGIN = "web-bundler";

    @Override
    public boolean isApplicable(Module project) {
        return JavaLibraryUtil.hasAnyLibraryJar(project, WebBundlerJavaConstants.WEB_BUNDLER_MAVEN_COORS);
    }

    @Override
    public void collectTemplateRootPaths(Module javaProject, List<TemplateRootPath> rootPaths) {
        VirtualFile moduleDir = QuarkusModuleUtil.getModuleDirPath(javaProject);
        if (moduleDir != null) {
            // web/templates
            String templateBaseDir = LSPIJUtils.toUri(moduleDir).resolve("web/templates").toASCIIString();
            rootPaths.add(new TemplateRootPath(templateBaseDir, ORIGIN));
        }
    }

}
