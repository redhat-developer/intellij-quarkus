/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.tool.ToolDelegate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.QUARKUS_DEPLOYMENT_LIBRARY_NAME;

public class QuarkusModuleComponent implements ModuleComponent {
    private final Module module;

    public QuarkusModuleComponent(Module module) {
        this.module = module;
    }

    @Override
    public void moduleAdded() {
        ToolDelegate toolDelegate = ToolDelegate.getDelegate(module);
        if (toolDelegate != null) {
            List<VirtualFile> deploymentFiles = toolDelegate.getDeploymentFiles(module);
            if (!deploymentFiles.isEmpty()) {
                ModuleRootModificationUtil.addModuleLibrary(module, QUARKUS_DEPLOYMENT_LIBRARY_NAME, deploymentFiles.stream().map(file -> file.getUrl()).collect(Collectors.toList()), Collections.emptyList(), DependencyScope.PROVIDED);
            }
        }
    }
}
