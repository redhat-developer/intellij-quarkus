/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search;

import org.eclipse.lsp4mp.commons.metadata.ConfigurationMetadata;
import org.eclipse.lsp4mp.commons.metadata.ItemHint;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;

/**
 * Properties collector API.
 * 
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/IPropertiesCollector.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/IPropertiesCollector.java</a>
 *
 */
public interface IPropertiesCollector {

	/**
	 * Add item metadata.
	 * 
	 * @param name          the property name
	 * @param type          the property type.
	 * @param description   the property description.
	 * @param sourceType    the property source type (class or interface) and null
	 *                      otherwise.
	 * @param sourceField   the source field (name of Java field) and null
	 *                      otherwise.
	 * @param sourceMethod  the source method (Java method signature) and null
	 *                      otherwise.
	 * @param defaultValue  the default value and null otherwise.
	 * @param extensionName the extension name and null otherwise.
	 * @param binary        true if property comes from JAR and false otherwise.
	 * @param phase         the Quarkus config pahase and 0 otherwise.
	 * @return the item metadata.
	 */
	ItemMetadata addItemMetadata(String name, String type, String description, String sourceType, String sourceField,
                                 String sourceMethod, String defaultValue, String extensionName, boolean binary, int phase);

	/**
	 * Returns true if the given hint exists and false otherwise.
	 * 
	 * @param hint the hint name.
	 * @return true if the given hint exists and false otherwise.
	 */
	boolean hasItemHint(String hint);

	/**
	 * Returns the item hint for the given hint name.
	 * 
	 * <p>
	 * If the item hint doesn't exists, the collector creates it.
	 * </p>
	 * 
	 * @param hint the hint name.
	 * @return the item hint for the given hint name.
	 */
	ItemHint getItemHint(String hint);

	/**
	 * Merges the properties and hints from <code>metadata</code>
	 * to the current <code>ConfigurationMetadata</code> instance
	 *
	 * @param metadata the metadata to merge
	 */
	void merge(ConfigurationMetadata metadata);
}
