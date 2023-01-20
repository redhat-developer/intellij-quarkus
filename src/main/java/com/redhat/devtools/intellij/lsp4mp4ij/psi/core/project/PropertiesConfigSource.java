/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project;

import com.intellij.openapi.module.Module;
import org.eclipse.lsp4mp.commons.utils.PropertyValueExpander;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link Properties} config file implementation.
 * 
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/project/PropertiesConfigSource.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/project/PropertiesConfigSource.java</a>
 *
 */
public class PropertiesConfigSource extends AbstractConfigSource<Properties> {

	private transient PropertyValueExpander propertyValueExpander = null;

	public PropertiesConfigSource(String configFileName, String profile, int ordinal, Module javaProject) {
		super(configFileName, profile, ordinal, javaProject);
	}

	public PropertiesConfigSource(String configFileName, Module javaProject) {
		super(configFileName, javaProject);
	}

	public PropertiesConfigSource(String configFileName, int ordinal, Module javaProject) {
		super(configFileName, ordinal, javaProject);
	}

	@Override
	public String getProperty(String key) {
		Properties properties = getConfig();
		if (properties == null) {
			return null;
		}
		return properties.getProperty(key);
	}

	@Override
	protected Properties loadConfig(InputStream input) throws IOException {
		propertyValueExpander = null;
		Properties properties = new Properties();
		properties.load(input);
		String profile = getProfile();
		if (profile != null) {
			// Prefix all properties with profile
			Properties adjustedProperties = new Properties();
			properties.forEach((key, val) -> {
				// Ignore any properties with a profile,
				// since they are not valid
				if (!((String) key).startsWith("%")) {
					adjustedProperties.putIfAbsent("%" + profile + "." + key, val);
				}
			});
			return adjustedProperties;
		}
		return properties;
	}

	@Override
	protected Map<String, List<MicroProfileConfigPropertyInformation>> loadPropertyInformations() {
		Properties config = super.getConfig();
		Map<String /* property key without profile */, List<MicroProfileConfigPropertyInformation>> propertiesMap = new HashMap<>();
		config.forEach((key, val) -> {
			if (key != null) {
				String propertyKeyWithProfile = key.toString();
				String propertyValue = val != null ? val.toString() : null;

				String propertyKey = MicroProfileConfigPropertyInformation
						.getPropertyNameWithoutProfile(propertyKeyWithProfile);
				List<MicroProfileConfigPropertyInformation> info = propertiesMap.get(propertyKey);
				if (info == null) {
					info = new ArrayList<>();
					propertiesMap.put(propertyKey, info);
				}
				info.add(new MicroProfileConfigPropertyInformation(propertyKeyWithProfile, propertyValue,
						getSourceConfigFileURI(), getConfigFileName()));
			}
		});
		return propertiesMap;
	}

	@Override
	public Set<String> getAllKeys() {
		Properties properties = getConfig();
		if (properties == null) {
			return Collections.emptySet();
		}
		return properties.keySet().stream().map(key -> (String) key).collect(Collectors.toSet());
	}

}
