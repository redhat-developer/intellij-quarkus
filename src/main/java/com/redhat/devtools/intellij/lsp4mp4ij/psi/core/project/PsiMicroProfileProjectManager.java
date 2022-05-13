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

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link PsiMicroProfileProject} manager.
 * 
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/project/JDTMicroProfileProjectManager.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/project/JDTMicroProfileProjectManager.java</a>
 *
 */
@Service
public final class PsiMicroProfileProjectManager {

	public static PsiMicroProfileProjectManager getInstance(Project project) {
		return ServiceManager.getService(project, PsiMicroProfileProjectManager.class);
	}

	private final Map<Module, PsiMicroProfileProject> projects;

	private PsiMicroProfileProjectManager() {
		this.projects = new HashMap<>();
	}

	public PsiMicroProfileProject getJDTMicroProfileProject(Module project) {
		return getJDTMicroProfileProject(project, true);
	}

	private PsiMicroProfileProject getJDTMicroProfileProject(Module project, boolean create) {
		Module javaProject = project;
		PsiMicroProfileProject info = projects.get(javaProject);
		if (info == null) {
			if (!create) {
				return null;
			}
			info = new PsiMicroProfileProject(javaProject);
			projects.put(javaProject, info);
		}
		return info;
	}

	public boolean isConfigSource(VirtualFile file) {
		String fileName = file.getName();
		for (IConfigSourceProvider provider : IConfigSourceProvider.EP_NAME.getExtensions()) {
			if (provider.isConfigSource(fileName)) {
				return true;
			}
		}
		return false;
	}
}
