/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs;

import java.util.List;
import java.util.Set;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.IConfigSourceProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Provides a list of jax-rs methods in a project or file.
 */
public interface IJaxRsInfoProvider {

	/**
	 * Returns true if the provider can provide information on the JAX-RS methods in the given class and false otherwise.
	 *
	 * @param typeRoot the class to check
	 * @param monitor the progress monitor
	 * @return true if the provider can provide information on the JAX-RS methods in the given class and false otherwise
	 */
	boolean canProvideJaxRsMethodInfoForClass(@NotNull PsiFile typeRoot, @NotNull Module project, @NotNull ProgressIndicator monitor);

	/**
	 * Returns a non-null set of all the classes in the given project that this provider can provide JAX-RS method information for.
	 *
	 * @param javaProject the project to check for JAX-RS method information
	 * @param monitor the progress monitor
	 * @return a non-null set of all the classes in the given project that this provider can provide JAX-RS method information for
	 */
	@NotNull Set<PsiClass> getAllJaxRsClasses(@NotNull Module javaProject, @NotNull ProgressIndicator monitor);

	/**
	 * Returns a list of all the JAX-RS methods in the given type.
	 *
	 * @param type    the type to check for JAX-RS methods
	 * @param monitor the progress monitor
	 * @return a list of all the JAX-RS methods in the given type
	 */
	@NotNull List<JaxRsMethodInfo> getJaxRsMethodInfo(@NotNull PsiFile type, @NotNull JaxRsContext jaxrsContext, @NotNull IPsiUtils utils, @NotNull ProgressIndicator monitor);

}