/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.microprofile.psi.internal.quarkus.providers;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.IProjectLabelProvider;
import com.redhat.microprofile.psi.quarkus.PsiQuarkusUtils;

import java.util.Collections;
import java.util.List;


/**
 * Provides a Quarkus-specific label to a project if the project is a
 * Quarkus project
 *
 * @author dakwon
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.quarkus/src/main/java/com/redhat/microprofile/jdt/internal/quarkus/providers/QuarkusProjectLabelProvider.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.quarkus/src/main/java/com/redhat/microprofile/jdt/internal/quarkus/providers/QuarkusProjectLabelProvider.java</a>
 *
 */
public class QuarkusProjectLabelProvider implements IProjectLabelProvider {
	
	public static String QUARKUS_LABEL = "quarkus";

	@Override
	public List<String> getProjectLabels(Module project) {
		if (PsiQuarkusUtils.isQuarkusProject(project)) {
			return Collections.singletonList(QUARKUS_LABEL);
		};
		return Collections.emptyList();
	}
}
