/*******************************************************************************
 * Copyright (c) 2019-2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.tool.ToolDelegate;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.QUARKUS_DEPLOYMENT_LIBRARY_NAME;
import static com.redhat.devtools.intellij.quarkus.tool.ToolDelegate.BINARY;
import static com.redhat.devtools.intellij.quarkus.tool.ToolDelegate.SOURCES;

public class QuarkusPostStartupActivity implements StartupActivity, DumbAware {
    @Override
    public void runActivity(@NotNull Project project) {
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            QuarkusModuleUtil.ensureQuarkusLibrary(module);
        }
    }
}
