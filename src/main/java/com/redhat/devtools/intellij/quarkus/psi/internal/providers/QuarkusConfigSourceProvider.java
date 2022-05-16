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
package com.redhat.devtools.intellij.quarkus.psi.internal.providers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.IConfigSource;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.IConfigSourceProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PropertiesConfigSource;

/**
 * Provides configuration sources specific to Quarkus
 *
 * This should be moved to quarkus-ls in the future
 *
 * @author datho7561
 */
public class QuarkusConfigSourceProvider implements IConfigSourceProvider {

	public static final String APPLICATION_PROPERTIES_FILE = "application.properties";
	public static final String APPLICATION_YAML_FILE = "application.yaml";
	public static final String APPLICATION_YML_FILE = "application.yml";
	private static final Pattern PER_PROFILE_FILE_NAME_PTN = Pattern.compile("application-([A-Za-z]+)\\.properties");

	@Override
	public List<IConfigSource> getConfigSources(Module javaProject, VirtualFile outputFolder) {
		List<IConfigSource> configSources = new ArrayList<>();
		List<VirtualFile> folders = new ArrayList<>();
		folders.addAll(Arrays.asList(ModuleRootManager.getInstance(javaProject).getSourceRoots(false)));
		if (outputFolder != null) {
			folders.add(outputFolder);
		}
		Set<String> fileNames = new HashSet<>();
		for(VirtualFile folder : folders) {
			for (VirtualFile file : folder.getChildren()) {
				if (!file.isDirectory() && !fileNames.contains(file.getName())) {
					String fileName = file.getName();
					IConfigSource configSource = createConfigSource(fileName, javaProject);
					if (configSource != null) {
						configSources.add(configSource);
						fileNames.add(file.getName());
					}
				}
			}
		}
		return configSources;
	}

	private static IConfigSource createConfigSource(String fileName, Module javaProject) {
		if (APPLICATION_PROPERTIES_FILE.equals(fileName)) {
			return new PropertiesConfigSource(fileName, 250, javaProject);
		}
		if (APPLICATION_YAML_FILE.equals(fileName) || APPLICATION_YML_FILE.equals(fileName)) {
			return new YamlConfigSource(fileName, javaProject);
		}
		Matcher m = PER_PROFILE_FILE_NAME_PTN.matcher(fileName);
		if (m.matches()) {
			// I don't think Quarkus assigns a specific ordinal to
			// application-${profile}.properties files.
			// This ordinal means that application-${profile}.properties files take
			// precedence over application.properties
			return new PropertiesConfigSource(fileName, m.group(1), 261, javaProject);
		}
		return null;
	}

	@Override
	public boolean isConfigSource(String fileName) {
		return APPLICATION_PROPERTIES_FILE.equals(fileName) || APPLICATION_YAML_FILE.equals(fileName)
				|| APPLICATION_YML_FILE.equals(fileName) || PER_PROFILE_FILE_NAME_PTN.matcher(fileName).matches();
	}
}
