/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.psi.internal.providers;

import com.intellij.openapi.module.Module;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.AbstractConfigSource;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.MicroProfileConfigPropertyInformation;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PropertiesConfigSource;
import com.redhat.devtools.intellij.quarkus.psi.internal.utils.YamlUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Yaml config source implementation
 * 
 * @author dakwon
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/project/YamlConfigSource.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/project/YamlConfigSource.java</a>
 *
 */
public class YamlConfigSource extends PropertiesConfigSource {

	public YamlConfigSource(String configFileName, Module javaProject) {
		super(configFileName, javaProject);
	}

	@Override
	protected Properties loadConfig(InputStream input) throws IOException {
		// Convert Yaml document into flattern properties
		return YamlUtils.loadYamlAsProperties(input);
	}

	@Override
	public int getOrdinal() {
		// See https://github.com/quarkusio/quarkus/blob/main/extensions/config-yaml/runtime/src/main/java/io/quarkus/config/yaml/runtime/ApplicationYamlConfigSourceLoader.java#L29
		// (or Quarkus --> yaml config extension --> ApplicationYamlConfigSourceLoader if the link is dead)
		return 255;
	}
}
