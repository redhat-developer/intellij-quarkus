/*******************************************************************************
* Copyright (c) 2024 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.qute.psi.template.rootpath;

import java.util.List;

import com.intellij.openapi.module.Module;
import com.redhat.qute.commons.TemplateRootPath;

/**
 * Template root path provider API.
 *
 * @author Angelo ZERR
 *
 */
public interface ITemplateRootPathProvider {

	/**
	 * Returns true if the given Java project can provide template root path and
	 * false otherwise.
	 * 
	 * @param project the Java project.
	 * @return true if the given Java project can provide template root path and
	 *         false otherwise.
	 */
	boolean isApplicable(Module project);

	/**
	 * Collect template root path for the given Java project.
	 * 
	 * @param javaProject the Java project.
	 */
	void collectTemplateRootPaths(Module javaProject, List<TemplateRootPath> rootPaths);
}