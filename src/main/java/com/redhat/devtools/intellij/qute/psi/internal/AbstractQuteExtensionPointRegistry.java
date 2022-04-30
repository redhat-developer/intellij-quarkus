/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.KeyedLazyInstanceEP;

/**
 * Registry to hold providers for an extension point
 *
 * @param T the interface that the providers implement
 * 
 * @author datho7561
 */
public abstract class AbstractQuteExtensionPointRegistry<T,K extends KeyedLazyInstanceEP<T>>{

	private static final String CLASS_ATTR = "class";

	private static final Logger LOGGER = Logger.getLogger(AbstractQuteExtensionPointRegistry.class.getName());

	private boolean extensionProvidersLoaded;
	private boolean registryListenerIntialized;
	private final List<T> providers;

	public AbstractQuteExtensionPointRegistry() {
		super();
		this.extensionProvidersLoaded = false;
		this.registryListenerIntialized = false;
		this.providers = new ArrayList<>();
	}

	/**
	 * Returns the extension id of the provider extension point
	 *
	 * @return the extension id of the provider extension point
	 */
	public abstract ExtensionPointName<K> getProviderExtensionId();

	/**
	 * Returns all the providers.
	 *
	 * @return all the providers.
	 */
	public List<T> getProviders() {
		loadExtensionProviders();
		return providers;
	}

	private synchronized void loadExtensionProviders() {
		if (extensionProvidersLoaded)
			return;

		// Immediately set the flag, as to ensure that this method is never
		// called twice
		extensionProvidersLoaded = true;

		LOGGER.log(Level.INFO, "->- Loading ." + getProviderExtensionId() + " extension point ->-");

		ExtensionPointName<K> cf = getProviderExtensionId();
		addExtensionProviders(cf);

		LOGGER.log(Level.INFO, "-<- Done loading ." + getProviderExtensionId() + " extension point -<-");
	}

	private void addExtensionProviders(ExtensionPointName<K> cf) {
		for (K ce : cf.getExtensionList()) {
			try {
				T provider = createProvider(ce);
				synchronized (providers) {
					providers.add(provider);
				}
				LOGGER.log(Level.INFO, "  Loaded " + getProviderExtensionId() + ": " + provider.getClass().getName());
			} catch (Throwable t) {
				LOGGER.log(Level.SEVERE, "  Loaded while loading " + getProviderExtensionId(), t);
			}
		}
	}

	protected T createProvider(K ce) {
		return ce.getInstance();
	}
}