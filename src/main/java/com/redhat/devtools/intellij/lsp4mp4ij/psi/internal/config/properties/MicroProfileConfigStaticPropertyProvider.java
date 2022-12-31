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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.config.properties;

import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.AbstractStaticPropertiesProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.SearchContext;

/**
 * MicroProfile Config provider.
 * 
 * @see https://download.eclipse.org/microprofile/microprofile-config-1.4/microprofile-config-spec.html#_microprofile_config
 */
public class MicroProfileConfigStaticPropertyProvider extends AbstractStaticPropertiesProvider {

    public MicroProfileConfigStaticPropertyProvider() {
        super("/static-properties/mp-config-metadata.json");
    }

    @Override
    protected boolean isAdaptedFor(SearchContext context) {
        // MicroProfile config properties are always available with MicroProfile
        return true;
    }
}
