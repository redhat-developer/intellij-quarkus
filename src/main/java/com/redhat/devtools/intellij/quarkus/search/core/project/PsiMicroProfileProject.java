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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

	/**
	 * Returns the value of this property or <code>defaultValue</code> if it is not
	 * defined in this project
	 *
	 * @param propertyKey  the property to get with the profile included, in the
	 *                     format used by microprofile-config.properties
	 * @param defaultValue the value to return if the value for the property is not
	 *                     defined in this project
	 * @return the value of this property or <code>defaultValue</code> if it is not
	 *         defined in this project
	 */
	public String getProperty(String propertyKey, String defaultValue) {
		for (IConfigSource configSource : configSources) {
			String propertyValue = configSource.getProperty(propertyKey);
			if (propertyValue != null) {
				return propertyValue;
			}
		}
		return defaultValue;
	}

	/**
	 * Returns the value of this property or null if it is not defined in this
	 * project
	 *
	 * @param propertyKey the property to get with the profile included, in the
	 *                    format used by microprofile-config.properties
	 * @return the value of this property or null if it is not defined in this
	 *         project
	 */
	public String getProperty(String propertyKey) {
		return getProperty(propertyKey, null);
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

	/**
	 * Returns a list of the property information (values for each profile and where
	 * they are assigned) for a given property
	 *
	 * @param propertyKey the name of the property to collect the values for
	 * @return a list of all values for properties and different profiles that are
	 *         defined in this project
	 */
	public List<MicroProfileConfigPropertyInformation> getPropertyInformations(String propertyKey) {
		// Use a map to override property values
		// eg. if application.yaml defines a value for a property it should override the
		// value defined in application.properties
		Map<String, MicroProfileConfigPropertyInformation> propertyToInfoMap = new HashMap<>();
		IConfigSource configSource;
		// Go backwards so that application.properties replaces
		// microprofile-config.properties, etc.
		for (int i = configSources.size() - 1; i >= 0; i--) {
			configSource = configSources.get(i);
			propertyToInfoMap.putAll(configSource.getPropertyInformations(propertyKey));
		}
		return propertyToInfoMap.values().stream() //
				.sorted((a, b) -> {
					return a.getPropertyNameWithProfile().compareTo(b.getPropertyNameWithProfile());
				}) //
				.collect(Collectors.toList());
	}
}