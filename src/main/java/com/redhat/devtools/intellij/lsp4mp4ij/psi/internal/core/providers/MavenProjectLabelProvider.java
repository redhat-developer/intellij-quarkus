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

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.IProjectLabelProvider;
import org.jetbrains.idea.maven.utils.MavenUtil;

import java.util.Collections;
import java.util.List;

/**
 * Provides a Maven-specific label to a project if the project is a
 * Maven project
 *
 * @author dakwon
 *
 */
public class MavenProjectLabelProvider implements IProjectLabelProvider {

	public static final String MAVEN_LABEL = "maven";
	private static final String MAVEN_NATURE_ID = "org.eclipse.m2e.core.maven2Nature";

	@Override
	public List<String> getProjectLabels(Module project) {
		if (MavenProjectLabelProvider.isMavenProject(project)) {
			return Collections.singletonList(MAVEN_LABEL);
		}
		return Collections.emptyList();
	}

	private static boolean isMavenProject(Module project) {
		return MavenUtil.isMavenModule(project);
	}
}
