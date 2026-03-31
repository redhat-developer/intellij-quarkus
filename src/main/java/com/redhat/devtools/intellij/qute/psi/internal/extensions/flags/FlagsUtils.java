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
package com.redhat.devtools.intellij.qute.psi.internal.extensions.flags;

import com.intellij.java.library.JavaLibraryUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.redhat.devtools.intellij.qute.psi.internal.extensions.renarde.RenardeJavaConstants;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Flags project utilities.
 */
public class FlagsUtils {

    public static boolean isFlagsProject(@NotNull Module module) {
        if (JavaLibraryUtil.hasAnyLibraryJar(module, FlagsJavaConstants.FLAGS_MAVEN_COORS)) {
            return true;
        }

        Set<Module> projectDependencies = new HashSet<>();
        ModuleUtilCore.getDependencies(module, projectDependencies);
        return projectDependencies
                .stream()
                .anyMatch(m -> FlagsJavaConstants.FLAGS_ARTIFACT_ID.equals(m.getName()));
    }
}
