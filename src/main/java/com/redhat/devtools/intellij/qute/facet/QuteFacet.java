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
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.openapi.module.Module;

public class QuteFacet extends Facet<QuteFacetConfiguration> {
    public static final FacetTypeId<QuteFacet> FACET_TYPE_ID = new FacetTypeId<>("qute");

    protected QuteFacet(FacetType facetType, Module module, String name, QuteFacetConfiguration configuration, Facet underlyingFacet) {
        super(facetType, module, name, configuration, underlyingFacet);
    }

    public static FacetType<QuteFacet, QuteFacetConfiguration> getQuteFacetType() {
        return FacetTypeRegistry.getInstance().findFacetType(FACET_TYPE_ID);
    }
}
