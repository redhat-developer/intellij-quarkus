/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search.internal.core.providers;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.quarkus.search.IProjectLabelProvider;
import com.redhat.devtools.intellij.quarkus.search.core.utils.PsiMicroProfileUtils;

import java.util.Collections;
import java.util.List;

/**
 * Provides a MicroProfile-specific label to a project if the project is a
 * MicroProfile project
 *
 * @author Angelo ZERR
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/tree/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/providers">https://github.com/redhat-developer/quarkus-ls/tree/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/providers</a>
 *
 */
public class MicroProfileProjectLabelProvider implements IProjectLabelProvider {
	
	public static String MICROPROFILE_LABEL = "microprofile";

	@Override
	public List<String> getProjectLabels(Module project) {
		if (PsiMicroProfileUtils.isMicroProfileProject(project)) {
			return Collections.singletonList(MICROPROFILE_LABEL);
		};
		return Collections.emptyList();
	}
}
