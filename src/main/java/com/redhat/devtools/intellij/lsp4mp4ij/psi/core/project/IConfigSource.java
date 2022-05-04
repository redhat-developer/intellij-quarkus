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

import com.intellij.openapi.vfs.VirtualFile;

import java.util.Map;

/**
 * Configuration file API
 * 
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/project/IConfigSource.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/project/IConfigSource.java</a>
 *
 */
public interface IConfigSource {

	/**
	 * Returns the property from the given <code>key</code> and null otherwise.
	 * 
	 * @param key the key
	 * @return the property from the given <code>key</code> and null otherwise.
	 */
	String getProperty(String key);

	/**
	 * Returns the property as Integer from the given <code>key</code> and null
	 * otherwise.
	 * 
	 * @param key the key
	 * @return the property as Integer from the given <code>key</code> and null
	 *         otherwise.
	 */
	Integer getPropertyAsInt(String key);

	/**
	 * Returns the file name of the associated config file
	 *
	 * @return the file name of the associated config file
	 */
	String getConfigFileName();

	/**
	 * Returns the source file URI of the associated config file
	 *
	 * @return the source file URI of the associated config file
	 */
	String getSourceConfigFileURI();

	/**
	 * Returns a map from the property and profile, in the format used by
	 * <code>microprofile-config.properties</code>, to the related property
	 * information, for each property and profile that's assigned a value in this
	 * config source
	 *
	 * A map is used so that it can be merged with another map and override any
	 * property informations from that map
	 *
	 * @param propertyKey the name of the property to collect the values for
	 * @return a map from the property and profile, in the format used by
	 *         <code>microprofile-config.properties</code>, to the related property
	 *         information, for each property and profile that's assigned a value in
	 *         this config source
	 */
	Map<String, MicroProfileConfigPropertyInformation> getPropertyInformations(String propertyKey);

	/**
	 * Returns the ordinal for this config source
	 *
	 * See https://download.eclipse.org/microprofile/microprofile-config-2.0/microprofile-config-spec-2.0.html#_configsource_ordering
	 *
	 * @return the ordinal for this config source
	 */
	default int getOrdinal() {
		return 100;
	}

	/**
	 * Returns true if the given file is the same file as this config source and false otherwise.
	 *
	 * @param file the file to check
	 * @return true if the given file is the same file as this config source and false otherwise
	 */
	boolean isSameFile(VirtualFile file);
}
