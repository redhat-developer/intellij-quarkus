/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;

import java.util.List;

/**
 * Project label provider API
 * 
 * @author dakwon
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/IProjectLabelProvider.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/IProjectLabelProvider.java</a>
 *
 */
public interface IProjectLabelProvider {
	public static final ExtensionPointName<IProjectLabelProvider> EP_NAME = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.projectLabelProvider");

	/**
	 * Returns a list of project labels ("maven", "microprofile", etc.) for the given project
	 * @param project the project to get labels for
	 * @return a list of project labels for the given project
	 */
	List<String> getProjectLabels(Module project);
}
