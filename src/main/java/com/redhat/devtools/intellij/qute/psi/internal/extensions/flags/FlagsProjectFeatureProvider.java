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

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.qute.psi.template.project.IProjectFeatureProvider;
import com.redhat.qute.commons.ProjectFeature;
import com.redhat.qute.commons.config.flags.FlagsConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static com.redhat.devtools.intellij.qute.psi.internal.extensions.flags.FlagsUtils.isFlagsProject;
import static com.redhat.devtools.intellij.qute.psi.internal.extensions.renarde.RenardeUtils.isRenardeProject;

/**
 * Flags project feature.
 */
public class FlagsProjectFeatureProvider implements IProjectFeatureProvider {

    @Override
    public void collectProjectFeatures(@NotNull Module javaProject, Set<ProjectFeature> projectFeatures) {
        if (isFlagsProject(javaProject)) {
            projectFeatures.add(FlagsConfig.PROJECT_FEATURE);
        }

    }

}
