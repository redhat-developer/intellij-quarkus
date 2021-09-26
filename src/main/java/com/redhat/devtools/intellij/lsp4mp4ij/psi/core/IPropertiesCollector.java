/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core;

import org.eclipse.lsp4mp.commons.metadata.ConfigurationMetadata;
import org.eclipse.lsp4mp.commons.metadata.ItemHint;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;

/**
 * Properties collector API.
 *
 * @author Angelo ZERR
 *
 */
public interface IPropertiesCollector {

	/**
	 * Defines the strategy for merging properties
	 */
	enum MergingStrategy {
		REPLACE,
		FORCE,
		IGNORE_IF_EXISTS
	}

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
	default void merge(ConfigurationMetadata metadata) {
		merge(metadata, MergingStrategy.FORCE);
	}

	/**
	 * Merges the properties and hints from <code>metadata</code>
	 * to the current <code>ConfigurationMetadata</code> instance
	 * according to the specified merging strategy
	 *
	 * @param metadata the metadata to merge
	 * @param mergingStrategy stategy to use
	 */
	void merge(ConfigurationMetadata metadata, MergingStrategy mergingStrategy);
}
