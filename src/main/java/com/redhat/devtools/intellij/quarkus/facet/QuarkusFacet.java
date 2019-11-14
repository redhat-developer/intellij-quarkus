/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.openapi.module.Module;

public class QuarkusFacet extends Facet<QuarkusFacetConfiguration> {
    public static final FacetTypeId<QuarkusFacet> FACET_TYPE_ID = new FacetTypeId<>("quarkus");

    protected QuarkusFacet(FacetType facetType, Module module, String name, QuarkusFacetConfiguration configuration, Facet underlyingFacet) {
        super(facetType, module, name, configuration, underlyingFacet);
    }

    public static FacetType<QuarkusFacet, QuarkusFacetConfiguration> getQuarkusFacetType() {
        return FacetTypeRegistry.getInstance().findFacetType(FACET_TYPE_ID);
    }
}
