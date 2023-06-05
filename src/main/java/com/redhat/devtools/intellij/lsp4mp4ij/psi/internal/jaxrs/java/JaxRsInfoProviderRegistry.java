/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.jaxrs.java;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.jaxrs.IJaxRsInfoProvider;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry which hosts {@link IJaxRsInfoProvider} contributed with the "com.redhat.devtools.intellij.quarkus.jaxRsInfoProvider" extension point.
 */
public class JaxRsInfoProviderRegistry {

    private static final ExtensionPointName<IJaxRsInfoProvider> EP_NAME = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.jaxRsInfoProvider");

    private static final JaxRsInfoProviderRegistry INSTANCE = new JaxRsInfoProviderRegistry();

    private boolean initialized;

    private List<IJaxRsInfoProvider> providers;

    private JaxRsInfoProviderRegistry() {
        super();
    }

    public static JaxRsInfoProviderRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the provider that can provide JAX-RS method info for the given class,
     * or null if no provider can provide info.
     *
     * @param typeRoot the class to collect JAX-RS method info for
     * @param project  the Java project
     * @return the provider that can provide JAX-RS method info for the given class,
     * or null if no provider can provide info
     */
    public @Nullable IJaxRsInfoProvider getProviderForType(PsiFile typeRoot, Module project, ProgressIndicator monitor) {
        for (IJaxRsInfoProvider provider : getProviders()) {
            if (provider.canProvideJaxRsMethodInfoForClass(typeRoot, project, monitor)) {
                return provider;
            }
        }
        return null;
    }

    private List<IJaxRsInfoProvider> getProviders() {
        if (!initialized) {
            providers = loadProviders();
            return providers;
        }
        return providers;
    }

    private synchronized List<IJaxRsInfoProvider> loadProviders() {
        if (initialized) {
            return providers;
        }
        List<IJaxRsInfoProvider> providers = new ArrayList<>();
        for (IJaxRsInfoProvider provider : EP_NAME.getExtensions()) {
            providers.add(provider);
        }
        providers.add(new DefaultJaxRsInfoProvider());
        return providers;
    }

}
