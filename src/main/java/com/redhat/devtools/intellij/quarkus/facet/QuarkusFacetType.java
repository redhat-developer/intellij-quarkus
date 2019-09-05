package com.redhat.devtools.intellij.quarkus.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuarkusFacetType extends FacetType<QuarkusFacet, QuarkusFacetConfiguration> {
    QuarkusFacetType() {
        super(QuarkusFacet.FACET_TYPE_ID, "Quarkus", "Quarkus");
    }

    @Override
    public QuarkusFacetConfiguration createDefaultConfiguration() {
        return new QuarkusFacetConfigurationImpl();
    }

    @Override
    public QuarkusFacet createFacet(@NotNull Module module, String name, @NotNull QuarkusFacetConfiguration configuration, @Nullable Facet underlyingFacet) {
        return new QuarkusFacet(this, module, name, configuration, underlyingFacet);
    }

    @Override
    public boolean isSuitableModuleType(ModuleType moduleType) {
        return moduleType instanceof JavaModuleType;
    }
}
