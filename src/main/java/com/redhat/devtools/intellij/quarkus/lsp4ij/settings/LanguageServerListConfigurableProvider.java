/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
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
package com.redhat.devtools.intellij.quarkus.lsp4ij.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageServersRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Language server list UI settings provider.
 */
public class LanguageServerListConfigurableProvider extends ConfigurableProvider {

    @Override
    public boolean canCreateConfigurable() {
        return !getLanguageServeDefinitions().isEmpty();
    }

    @Override
    public Configurable createConfigurable() {
        return new LanguageServerListConfigurable(getLanguageServeDefinitions());
    }

    private static @NotNull Set<LanguageServersRegistry.LanguageServerDefinition> getLanguageServeDefinitions() {
        return LanguageServersRegistry.getInstance().getAllDefinitions();
    }
}
