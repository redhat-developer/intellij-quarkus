/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search.internal.core.project;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.quarkus.search.core.project.MicroProfileConfigPropertyInformation;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * {@link Properties} config file implementation.
 * 
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/project/PropertiesConfigSource.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/project/PropertiesConfigSource.java</a>
 *
 */
public class PropertiesConfigSource extends AbstractConfigSource<Properties> {

	public PropertiesConfigSource(String configFileName, Module javaProject) {
		super(configFileName, javaProject);
	}

	@Override
	protected String getProperty(String key, Properties properties) {
		return properties.getProperty(key);
	}

	@Override
	protected Properties loadConfig(InputStream input) throws IOException {
		Properties properties = new Properties();
		properties.load(input);
		return properties;
	}

	@Override
	public Map<String, MicroProfileConfigPropertyInformation> getPropertyInformations(String propertyKey,
																					  Properties properties) {
		Map<String, MicroProfileConfigPropertyInformation> infos = new HashMap<>();
		if (properties != null) {
			properties.stringPropertyNames().stream() //
					.filter(key -> {
						return propertyKey
								.equals(MicroProfileConfigPropertyInformation.getPropertyNameWithoutProfile(key))
								&& getProperty(key) != null;
					}) //
					.forEach(matchingKey -> {
						infos.put(matchingKey, new MicroProfileConfigPropertyInformation(matchingKey,
								getProperty(matchingKey), getSourceConfigFileURI(), getConfigFileName()));
					});
		}
		return infos;
	}
}
