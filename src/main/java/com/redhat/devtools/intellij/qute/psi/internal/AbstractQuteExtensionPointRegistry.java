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

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.util.KeyedLazyInstanceEP;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry to hold providers for an extension point
 *
 * @param <T> the interface that the providers implement
 * @author datho7561
 */
public abstract class AbstractQuteExtensionPointRegistry<T, K extends KeyedLazyInstanceEP<T>> {

    private static final Logger LOGGER = Logger.getLogger(AbstractQuteExtensionPointRegistry.class.getName());

    private boolean extensionProvidersLoaded;
    private final List<T> providers;

    public AbstractQuteExtensionPointRegistry() {
        super();
        this.extensionProvidersLoaded = false;
        this.providers = new ArrayList<>();
        // Force the load of providers, to avoid loading it when getProviders() is called
        // because a ProcessCanceledException could be thrown while creating a provider.
        loadExtensionProviders();
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
                T provider = createInstance(ce);
                synchronized (providers) {
                    providers.add(provider);
                }
                LOGGER.log(Level.INFO, "  Loaded " + getProviderExtensionId() + ": " + provider.getClass().getName());
            } catch (ProcessCanceledException e) {
                //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
                //TODO delete block when minimum required version is 2024.2
                extensionProvidersLoaded = false;
                throw e;
            } catch (IndexNotReadyException | CancellationException e) {
                extensionProvidersLoaded = false;
                throw e;
            } catch (Exception t) {
                LOGGER.log(Level.WARNING, "  Loaded while loading " + getProviderExtensionId(), t);
            }
        }
    }

    protected T createInstance(K ce) {
        return ce.getInstance();
    }
}