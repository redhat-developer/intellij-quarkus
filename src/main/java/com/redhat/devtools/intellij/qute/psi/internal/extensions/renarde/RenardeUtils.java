/*******************************************************************************
 * Copyright (c) 2026 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.internal.extensions.renarde;

import com.intellij.java.library.JavaLibraryUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Renarde project utilities.
 */
public class RenardeUtils {

    public static boolean isRenardeProject(@NotNull Module module) {
        if (JavaLibraryUtil.hasAnyLibraryJar(module, RenardeJavaConstants.RENARDE_MAVEN_COORS)) {
            return true;
        }

        Set<Module> projectDependencies = new HashSet<>();
        ModuleUtilCore.getDependencies(module, projectDependencies);
        return projectDependencies
                .stream()
                .anyMatch(m -> RenardeJavaConstants.RENARDE_ARTIFACT_ID.equals(m.getName()));
    }
}
