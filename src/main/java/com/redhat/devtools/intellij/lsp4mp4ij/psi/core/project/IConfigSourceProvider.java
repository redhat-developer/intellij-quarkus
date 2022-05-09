/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project;

import java.util.List;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * Config source provider API
 *
 * @author datho7561
 */
public interface IConfigSourceProvider {

	public static final ExtensionPointName<IConfigSourceProvider> EP_NAME = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.configSourceProvider");

	/**
	 * Returns a list of configuration sources for a given Java project
	 *
	 * @param project the Java project to get configuration sources for
	 * @return a list of configuration sources for a given Java project
	 */
	List<IConfigSource> getConfigSources(Module project, VirtualFile outputFolder);

	/**
	 * Returns true if the given file name is a config source (ex :
	 * microprofile-config.properties) and false otherwise.
	 *
	 * @param fileName the file name to check.
	 *
	 * @return true if the given file name is a config source (ex :
	 *         microprofile-config.properties) and false otherwise.
	 */
	boolean isConfigSource(String fileName);
}
