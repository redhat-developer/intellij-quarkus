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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for module root changes (e.g. Maven/Gradle dependency updates) and invalidates
 * the Qute and Quarkus library detection caches for the affected project.
 *
 * <p>This listener is registered in {@code plugin.xml} and is automatically connected
 * to the project's message bus by IntelliJ.</p>
 */
public class QuarkusModuleRootListener implements ModuleRootListener {

    /**
     * Called when the module roots change (e.g. a dependency is added or removed).
     * Invalidates the Quarkus support cache so it is recomputed on the next access.
     *
     * <p>Note: Qute support cache is automatically invalidated via {@link com.intellij.psi.util.CachedValuesManager}
     * when module roots change, so no manual invalidation is needed.</p>
     *
     * @param event the module root event, must not be null.
     */
    @Override
    public void rootsChanged(@NotNull ModuleRootEvent event) {
        Object source = event.getSource();
        if (source instanceof Project project) {
            QuarkusModuleUtil.invalidateCache(project);
        }
    }
}