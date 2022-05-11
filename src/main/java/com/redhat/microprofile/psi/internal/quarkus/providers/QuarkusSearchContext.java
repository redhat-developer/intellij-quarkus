/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.microprofile.psi.internal.quarkus.providers;

import java.util.HashMap;
import java.util.Map;

import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.SearchContext;

/**
 * The Quarkus context used while search process.
 * 
 * @author Angelo ZERR
 *
 */
public class QuarkusSearchContext {

	public interface PropertyMapKeyReplacer {

		String replace(String baseKey, int keyIndex);
	}

	private static final String QUARKUS_CONTEXT_KEY = QuarkusSearchContext.class.getName();

	private final Map<String, PropertyMapKeyReplacer> propertyMapKeyReplacers;

	private QuarkusSearchContext() {
		this.propertyMapKeyReplacers = new HashMap<>();
	}

	/**
	 * Returns the Quarkus context of the current search context.
	 * 
	 * @param context the current search context
	 * @return the Quarkus context of the current search context.
	 */
	public static QuarkusSearchContext getQuarkusContext(SearchContext context) {
		QuarkusSearchContext quarkusContext = (QuarkusSearchContext) context.get(QUARKUS_CONTEXT_KEY);
		if (quarkusContext == null) {
			quarkusContext = new QuarkusSearchContext();
			context.put(QUARKUS_CONTEXT_KEY, quarkusContext);

		}
		return quarkusContext;
	}

	/**
	 * Register a property map key replacer for a given quarkus extension.
	 * 
	 * @param extensionName the Quarkus extension name.
	 * @param replacer      the replacer.
	 */
	public void registerPropertyMapKeyReplacer(String extensionName, PropertyMapKeyReplacer replacer) {
		propertyMapKeyReplacers.put(extensionName, replacer);
	}

	/**
	 * Returns the map key to use while generating Quarkus property.
	 * <ul>
	 * <li>If a {@link PropertyMapKeyReplacer} is registered for the given quarkus
	 * extension name, it is used (ex: to return ${quarkus.cache.name} for
	 * quarkus-cache extension to generate the final property
	 * quarkus.cache.caffeine.${quarkus.cache.name}.initial-capacity)</li>
	 * <li>otherwise this method returns '{*}'.</li>
	 * </ul>
	 * 
	 * @param extensionName the Quarkus extension name.
	 * @param baseKey       the current generated property.
	 * @param keyIndex      the map key index.
	 * 
	 * @return the map key to use while generating Quarkus property.
	 */
	public String getPropertyMapKey(String extensionName, String baseKey, int keyIndex) {
		PropertyMapKeyReplacer replacer = propertyMapKeyReplacers.get(extensionName);
		return replacer != null ? replacer.replace(baseKey, keyIndex) : "{*}";
	}

}
