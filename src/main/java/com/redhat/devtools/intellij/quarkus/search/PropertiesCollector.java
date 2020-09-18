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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.eclipse.lsp4mp.commons.metadata.ConfigurationMetadata;
import org.eclipse.lsp4mp.commons.metadata.ItemHint;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;

/**
 * Properties collector implementation.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/PropertiesCollector.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/PropertiesCollector.java</a>
 */
public class PropertiesCollector implements IPropertiesCollector {

	private final ConfigurationMetadata configuration;

	private final Map<String, ItemHint> hintsCache;

	private final boolean onlySources;

	public PropertiesCollector(ConfigurationMetadata configuration, List<MicroProfilePropertiesScope> scopes) {
		this.configuration = configuration;
		this.configuration.setProperties(new ArrayList<>());
		this.configuration.setHints(new ArrayList<>());
		this.hintsCache = new HashMap<>();
		this.onlySources = MicroProfilePropertiesScope.isOnlySources(scopes);
	}

	@Override
	public ItemMetadata addItemMetadata(String name, String type, String description, String sourceType,
			String sourceField, String sourceMethod, String defaultValue, String extensionName, boolean binary,
			int phase) {
		ItemMetadata property = new ItemMetadata();
		property.setName(name);
		property.setType(type);
		property.setDescription(description);
		property.setSourceType(sourceType);
		property.setSourceField(sourceField);
		property.setSourceMethod(sourceMethod);
		property.setDefaultValue(defaultValue);

		// Extra properties

		property.setExtensionName(extensionName);
		if (!binary) {
			property.setSource(Boolean.TRUE);
		}
		property.setPhase(phase);
		property.setRequired(defaultValue == null);

		configuration.getProperties().add(property);
		return property;
	}

	@Override
	public boolean hasItemHint(String hint) {
		return hintsCache.containsKey(hint);
	}

	private void addItemHint(ItemHint itemHint) {
		configuration.getHints().add(itemHint);
		hintsCache.put(itemHint.getName(), itemHint);
	}

	@Override
	public ItemHint getItemHint(String hint) {
		ItemHint itemHint = hintsCache.get(hint);
		if (itemHint != null) {
			return itemHint;
		}
		itemHint = new ItemHint();
		itemHint.setName(hint);
		itemHint.setValues(new ArrayList<>());
		addItemHint(itemHint);
		return itemHint;
	}

	@Override
	public void merge(ConfigurationMetadata metadata) {
		List<ItemMetadata> properties = metadata.getProperties();
		if (properties != null) {
			if (!onlySources) {
				configuration.getProperties().addAll(properties);
			} else {
				for (ItemMetadata property : properties) {
					// In the case of the scopes is only sources, the property which is a binary
					// property must not be added.
					if (property.getSource() != null && property.getSource()) {
						configuration.getProperties().add(property);
					}
				}
			}

		}
		List<ItemHint> hints = metadata.getHints();
		if (hints != null) {
			for (ItemHint itemHint : hints) {
				addItemHint(itemHint);
			}
		}
	}
}
