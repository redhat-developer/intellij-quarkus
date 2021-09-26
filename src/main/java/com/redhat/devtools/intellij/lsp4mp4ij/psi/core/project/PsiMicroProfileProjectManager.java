/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project;

import com.intellij.openapi.module.Module;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link PsiMicroProfileProject} manager.
 * 
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/project/JDTMicroProfileProjectManager.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/project/JDTMicroProfileProjectManager.java</a>
 *
 */
public class PsiMicroProfileProjectManager {

	private static final PsiMicroProfileProjectManager INSTANCE = new PsiMicroProfileProjectManager();

	public static PsiMicroProfileProjectManager getInstance() {
		return INSTANCE;
	}

	private final Map<Module, PsiMicroProfileProject> projects;

	private PsiMicroProfileProjectManager() {
		this.projects = new HashMap<>();
	}

	public PsiMicroProfileProject getJDTMicroProfileProject(Module project) {
		Module javaProject = project;
		PsiMicroProfileProject info = projects.get(javaProject);
		if (info == null) {
			info = new PsiMicroProfileProject(javaProject);
			projects.put(javaProject, info);
		}
		return info;
	}
}
