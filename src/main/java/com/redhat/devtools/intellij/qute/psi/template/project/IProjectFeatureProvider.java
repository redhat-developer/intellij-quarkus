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
package com.redhat.devtools.intellij.qute.psi.template.project;

import com.intellij.openapi.module.Module;
import com.redhat.qute.commons.ProjectFeature;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Project feature provider API.
 *
 * @author Angelo ZERR
 *
 */
public interface IProjectFeatureProvider {

    /**
     * Collect project feature for the given Java project.
     *
     * @param javaProject the Java project.
     */
    void collectProjectFeatures(@NotNull Module javaProject, @NotNull Set<ProjectFeature> projectFeatures);
}
