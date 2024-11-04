/*******************************************************************************
 * Copyright (c) 2024 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.internal.extensions.roq;

import com.intellij.java.library.JavaLibraryUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Roq Utilities.
 */
public class RoqUtils {

    /**
     * Returns true if the given module is a Roq project and false otherwise.
     *
     * @param module the module.
     * @return true if the given module is a Roq project and false otherwise.
     */
    public static boolean isRoqProject(@NotNull Module module) {
        if (JavaLibraryUtil.hasAnyLibraryJar(module, RoqJavaConstants.ROQ_MAVEN_COORS)) {
            return true;
        }

        Set<Module> projectDependencies = new HashSet<>();
        ModuleUtilCore.getDependencies(module, projectDependencies);
        return projectDependencies
                .stream()
                .anyMatch(m -> RoqJavaConstants.ROQ_ARTIFACT_ID.equals(m.getName()));
    }
}
