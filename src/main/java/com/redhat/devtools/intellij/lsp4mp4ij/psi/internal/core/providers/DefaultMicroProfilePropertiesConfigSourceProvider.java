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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.providers;

import java.util.Arrays;
import java.util.List;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.IConfigSource;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PropertiesConfigSource;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.IConfigSourceProvider;

/**
 * Provides the META-INF/microprofile-config.properties configuration source
 *
 * @author datho7561
 */
public class DefaultMicroProfilePropertiesConfigSourceProvider implements IConfigSourceProvider {

	public static final String MICROPROFILE_CONFIG_PROPERTIES_FILE = "META-INF/microprofile-config.properties";

	@Override
	public List<IConfigSource> getConfigSources(Module project) {
		return Arrays.asList(new PropertiesConfigSource(MICROPROFILE_CONFIG_PROPERTIES_FILE, project));
	}
}
