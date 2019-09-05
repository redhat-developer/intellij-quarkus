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
