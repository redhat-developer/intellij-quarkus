/*******************************************************************************
 * Copyright (c) 2026 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for dumb mode changes and invalidates the Qute and Quarkus library
 * detection caches when indexing completes.
 *
 * <p>During dumb mode (indexing), library detection may return incorrect results.
 * Invalidating the caches on {@link #exitDumbMode()} ensures they are recomputed
 * correctly once indexing is complete.</p>
 *
 * <p>This listener is registered in {@code plugin.xml} and is automatically connected
 * to the project's message bus by IntelliJ.</p>
 */
public class QuarkusDumbModeListener implements DumbService.DumbModeListener {

    private final Project project;

    public QuarkusDumbModeListener(@NotNull Project project) {
        this.project = project;
    }

    /**
     * Called when IntelliJ exits dumb mode (i.e. indexing is complete).
     * Invalidates the Qute and Quarkus support caches so they are recomputed on the next access.
     */
    @Override
    public void exitDumbMode() {
        PsiQuteProjectUtils.invalidateCache(project);
        QuarkusModuleUtil.invalidateCache(project);
    }
}