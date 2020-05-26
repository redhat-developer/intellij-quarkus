/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search.internal.core.project;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Abstract class for config file.
 * 
 * @author Angelo ZERR
 *
 * @param <T> the config model (ex: Properties for *.properties file)
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/project/AbstractConfigSource.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/project/AbstractConfigSource.java</a>
 */
public abstract class AbstractConfigSource<T> implements IConfigSource {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConfigSource.class);

	private final String configFileName;
	private final Module javaProject;
	private VirtualFile configFile;

	private long lastModified = -1L;

	private T config;

	public AbstractConfigSource(String configFileName, Module javaProject) {
		this.configFileName = configFileName;
		this.javaProject = javaProject;
	}

	/**
	 * Returns the target/classes/$configFile and null otherwise.
	 * 
	 * <p>
	 * Using this file instead of using src/main/resources/$configFile gives the
	 * capability to get the filtered value.
	 * </p>
	 * 
	 * @return the target/classes/$configFile and null otherwise.
	 */
	private VirtualFile getConfigFile() {
		if (configFile != null && configFile.exists()) {
			return configFile;
		}
		if (javaProject.isLoaded()) {
			VirtualFile[] sourceRoots = ModuleRootManager.getInstance(javaProject).getSourceRoots();
			for (VirtualFile sourceRoot : sourceRoots) {
				VirtualFile file = sourceRoot.findFileByRelativePath(configFileName);
				if (file != null && file.exists()) {
					return file;
				}
			}
			return null;
		}
		return null;
	}

	/**
	 * Returns the loaded config and null otherwise.
	 * 
	 * @return the loaded config and null otherwise
	 */
	private T getConfig() {
		VirtualFile configFile = getConfigFile();
		if (configFile == null) {
			reset();
			return null;
		}
		try {
			long currentLastModified = configFile.getModificationStamp();
			if (currentLastModified != lastModified) {
				reset();
				try (InputStream input = configFile.getInputStream()) {
					config = loadConfig(input);
					lastModified = configFile.getModificationStamp();
				} catch (IOException e) {
					reset();
					LOGGER.error("Error while loading properties from '" + configFile + "'.", e);
				}
			}
		} catch (RuntimeException e1) {
			LOGGER.error("Error while getting last modified time for '" + configFile + "'.", e1);
		}
		return config;
	}

	@Override
	public final String getProperty(String key) {
		T config = getConfig();
		if (config == null) {
			return null;
		}
		return getProperty(key, config);
	}

	@Override
	public Integer getPropertyAsInt(String key) {
		String property = getProperty(key);
		if (property != null && !property.trim().isEmpty()) {
			try {
				return Integer.parseInt(property.trim());
			} catch (NumberFormatException e) {
				LOGGER.error("Error while converting '" + property.trim() + "' as Integer for key '" + key + "'", e);
				return null;
			}
		}
		return null;
	}

	private void reset() {
		config = null;
	}

	/**
	 * Load the config model from the given input stream <code>input</code>.
	 * 
	 * @param input the input stream
	 * @return he config model from the given input stream <code>input</code>.
	 * @throws IOException
	 */
	protected abstract T loadConfig(InputStream input) throws IOException;

	/**
	 * Returns the property from the given <code>key</code> and null otherwise.
	 * 
	 * @param key
	 * @param config
	 * @return the property from the given <code>key</code> and null otherwise.
	 */
	protected abstract String getProperty(String key, T config);

}
