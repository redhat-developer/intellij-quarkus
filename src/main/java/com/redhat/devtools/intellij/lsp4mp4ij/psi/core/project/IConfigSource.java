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

import java.util.List;
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
	 * Returns a list of all values for properties and different profiles that are
	 * defined in this config source.
	 *
	 * <p>
	 * This list contains information for the property (ex : greeting.message) and
	 * profile property (ex : %dev.greeting.message).
	 * </p>
	 *
	 * @param propertyKey the name of the property to collect the values for
	 *
	 * @return a list of all values for properties and different profiles that are
	 *         defined in this config source.
	 */
	List<MicroProfileConfigPropertyInformation> getPropertyInformations(String propertyKey);

	/**
	 * Returns the ordinal for this config source
	 *
	 * See
	 * https://download.eclipse.org/microprofile/microprofile-config-2.0/microprofile-config-spec-2.0.html#_configsource_ordering
	 *
	 * @return the ordinal for this config source
	 */
	int getOrdinal();

	/**
	 * Returns the profile of the config source and null otherwise.
	 *
	 * @return the profile of the config source and null otherwise.
	 */
	String getProfile();
}
