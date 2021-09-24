/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.project;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.MicroProfileConfigPropertyInformation;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.YamlUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Yaml config source implementation
 * 
 * @author dakwon
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/project/YamlConfigSource.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/project/YamlConfigSource.java</a>
 *
 */
public class YamlConfigSource extends AbstractConfigSource<Map<String, Object>> {

	public YamlConfigSource(String configFileName, Module javaProject) {
		super(configFileName, javaProject);
	}

	@Override
	protected Map<String, Object> loadConfig(InputStream input) throws IOException {
		Yaml yaml = new Yaml();
		return (Map<String, Object>) yaml.load(input);
	}

	@Override
	protected String getProperty(String key, Map<String, Object> config) {
		String[] keyArray = key.split("\\.");
		Map<String, Object> curr = config;

		Object value;
		for (int i = 0; i < keyArray.length - 1; i++) {
			value = curr.get(keyArray[i]);
			if (value == null || value instanceof String) {
				return null;
			}

			curr = (Map<String, Object>) value;
		}

		value = curr.get(keyArray[keyArray.length - 1]);
		if (value instanceof Map) {
			// In this case:
			//
			// cors:
			// ~: true
			//
			// map.get(null) returns the value of ~
			value = ((Map<String, Object>) value).get(null);
		}

		if (value == null) {
			return null;
		}
		
		return String.valueOf(value);
	}

	@Override
	public Map<String, MicroProfileConfigPropertyInformation> getPropertyInformations(String propertyKey,
																					  Map<String, Object> config) {

		Map<String, MicroProfileConfigPropertyInformation> result = new HashMap<>();

		if (config == null) {
			return result;
		}

		final List<String> segments = MicroProfileConfigPropertyInformation.getSegments(propertyKey);
		if (segments.size() < 1) {
			return result;
		}

		for (String key : config.keySet()) {
			if (key.equals(segments.get(0))) {
				String value = YamlUtils.getValueRecursively(segments, config);
				if (value != null) {
					result.put(propertyKey,
							new MicroProfileConfigPropertyInformation(propertyKey, value, getSourceConfigFileURI(), getConfigFileName()));
				}
			} else if (key.charAt(0) == '%') {
				if (config.get(key) instanceof Map) {
					String value = YamlUtils.getValueRecursively(segments, config.get(key));
					if (value != null) {
						String propertyAndProfile = key + "." + propertyKey;
						result.put(propertyAndProfile, new MicroProfileConfigPropertyInformation(propertyAndProfile,
								value, getSourceConfigFileURI(), getConfigFileName()));
					}
				}
			}
		}

		return result;
	}
}
