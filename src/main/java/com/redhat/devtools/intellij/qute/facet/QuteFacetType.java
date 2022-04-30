/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuteFacetType extends FacetType<QuteFacet, QuteFacetConfiguration> {
    QuteFacetType() {
        super(QuteFacet.FACET_TYPE_ID, "Qute", "Qute");
    }

    @Override
    public QuteFacetConfiguration createDefaultConfiguration() {
        return new QuteFacetConfigurationImpl();
    }

    @Override
    public QuteFacet createFacet(@NotNull Module module, String name, @NotNull QuteFacetConfiguration configuration, @Nullable Facet underlyingFacet) {
        return new QuteFacet(this, module, name, configuration, underlyingFacet);
    }

    @Override
    public boolean isSuitableModuleType(ModuleType moduleType) {
        return moduleType instanceof JavaModuleType;
    }
}
