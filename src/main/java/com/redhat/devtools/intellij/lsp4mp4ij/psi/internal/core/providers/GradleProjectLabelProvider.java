/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.providers;

import java.util.Collections;
import java.util.List;

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.IProjectLabelProvider;
import org.jetbrains.plugins.gradle.util.GradleConstants;

/**
 * Provides a Gradle-specific label to a project if the project is a
 * Gradle project
 *
 * @author dakwon
 *
 */
public class GradleProjectLabelProvider implements IProjectLabelProvider {

	public static final String GRADLE_LABEL = "gradle";
	private static final String GRADLE_NATURE_ID = "org.eclipse.buildship.core.gradleprojectnature";

	@Override
	public List<String> getProjectLabels(Module project) {
		if (GradleProjectLabelProvider.isGradleProject(project)) {
			return Collections.singletonList(GRADLE_LABEL);
		}
		return Collections.emptyList();
	}

	private static boolean isGradleProject(Module project) {
		return ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, project);
	}
}
