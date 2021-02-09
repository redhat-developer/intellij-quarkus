/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search.core.project;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.quarkus.search.internal.core.project.IConfigSource;
import com.redhat.devtools.intellij.quarkus.search.internal.core.project.PropertiesConfigSource;
import com.redhat.devtools.intellij.quarkus.search.internal.core.project.YamlConfigSource;

import java.util.ArrayList;
import java.util.List;

/**
 * JDT MicroProfile project.
 * 
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/project/JDTMicroProfileProject.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/project/JDTMicroProfileProject.java</a>
 *
 */
public class PsiMicroProfileProject {

	public static final String MICROPROFILE_CONFIG_PROPERTIES_FILE = "META-INF/microprofile-config.properties";
	public static final String APPLICATION_PROPERTIES_FILE = "application.properties";
	public static final String APPLICATION_YAML_FILE = "application.yaml";

	private final List<IConfigSource> configSources;

	public PsiMicroProfileProject(Module javaProject) {
		this.configSources = new ArrayList<IConfigSource>(3);
		configSources.add(new YamlConfigSource(APPLICATION_YAML_FILE, javaProject));
		configSources.add(new PropertiesConfigSource(APPLICATION_PROPERTIES_FILE, javaProject));
		configSources.add(new PropertiesConfigSource(MICROPROFILE_CONFIG_PROPERTIES_FILE, javaProject));
	}

	public String getProperty(String key, String defaultValue) {
		for (IConfigSource configSource : configSources) {
			String property = configSource.getProperty(key);
			if (property != null) {
				return property;
			}
		}
		return defaultValue;
	}

	public Integer getPropertyAsInteger(String key, Integer defaultValue) {
		for (IConfigSource configSource : configSources) {
			Integer property = configSource.getPropertyAsInt(key);
			if (property != null) {
				return property;
			}
		}
		return defaultValue;
	}
}