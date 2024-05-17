/*******************************************************************************
 * Copyright (c) 2019-2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.startup.StartupActivity;
import com.redhat.devtools.intellij.lsp4mp4ij.classpath.ClasspathResourceChangedManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfigurationManager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuarkusPostStartupActivity implements ProjectActivity, DumbAware {

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        QuarkusRunConfigurationManager.getInstance(project);
        ClasspathResourceChangedManager.getInstance(project);
        // Force the instantiation of the manager to be sure that classpath listener
        // are registered before QuarkusLanguageClient classpath listener
        // When an application.properties changed
        // - the manager need to update the properties cache
        // - and after the QuarkusLanguageClient throws an event to trigger Java validation.
        // As java validation requires the properties cache, it needs that cache must be updated before.
        PsiMicroProfileProjectManager.getInstance(project);
        QuarkusProjectService.getInstance(project);
        return null;
    }
}
