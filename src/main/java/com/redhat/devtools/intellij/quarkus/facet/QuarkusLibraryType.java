package com.redhat.devtools.intellij.quarkus.facet;

import com.intellij.facet.ui.FacetBasedFrameworkSupportProvider;
import com.intellij.framework.library.DownloadableLibraryType;
import com.intellij.openapi.util.IconLoader;

import java.net.URL;

public class QuarkusLibraryType extends DownloadableLibraryType {
    public QuarkusLibraryType() {
        super("Quarkus", "quarkus", "quarkus", IconLoader.findIcon("/quarkus_icon_rgb_16px_default.png", QuarkusLibraryType.class), new URL[]{QuarkusLibraryType.class.getResource("/quarkus.xml")});
    }
    @Override
    protected String[] getDetectionClassNames() {
        return new String[] {"io.quarkus.runtime.Quarkus"};
    }

    public final String getUnderlyingFrameworkTypeId() {
        return FacetBasedFrameworkSupportProvider.getProviderId(QuarkusFacet.FACET_TYPE_ID);
    }
}
