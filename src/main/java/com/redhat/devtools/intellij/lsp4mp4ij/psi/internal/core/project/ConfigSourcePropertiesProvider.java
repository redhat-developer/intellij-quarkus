/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.project;

import java.util.Set;
import java.util.stream.Collectors;

import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.IConfigSource;
import org.eclipse.lsp4mp.commons.utils.IConfigSourcePropertiesProvider;
import org.eclipse.lsp4mp.commons.utils.StringUtils;

/**
 * Adapts an <code>IConfigSource</code> to <code>IConfigSourcePropertiesProvider</code>
 * 
 * @author datho7561
 */
public class ConfigSourcePropertiesProvider implements IConfigSourcePropertiesProvider {

	private final IConfigSource configSource;
	private transient Set<String> keys;

	public ConfigSourcePropertiesProvider(IConfigSource configSource) {
		this.configSource = configSource;
		this.keys = null;
	}

	@Override
	public Set<String> keys() {
		if (keys != null) {
			return keys;
		}
		keys = configSource.getAllKeys().stream() //
				.filter(key -> {
					return StringUtils.hasText(configSource.getProperty(key));
				}) //
				.collect(Collectors.toSet());
		return keys;
	}

	@Override
	public boolean hasKey(String key) {
		return StringUtils.hasText(configSource.getProperty(key));
	}

	@Override
	public String getValue(String key) {
		return configSource.getProperty(key);
	}

}
