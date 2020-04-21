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

import com.intellij.facet.ui.FacetBasedFrameworkSupportProvider;
import com.intellij.framework.library.DownloadableLibraryType;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class QuarkusLibraryType extends DownloadableLibraryType {

    public QuarkusLibraryType() {
        super("Quarkus", "quarkus", "quarkus", QuarkusLibraryType.class.getResource("/quarkus.xml"));
    }

    @NotNull
    @Override
    protected String[] getDetectionClassNames() {
        return new String[]{"io.quarkus.runtime.Quarkus"};
    }

    @NotNull
    @Override
    public Icon getLibraryTypeIcon() {
        return IconLoader.getIcon("/quarkus_icon_rgb_16px_default.png", QuarkusLibraryType.class);
    }

    public final String getUnderlyingFrameworkTypeId() {
        return FacetBasedFrameworkSupportProvider.getProviderId(QuarkusFacet.FACET_TYPE_ID);
    }
}
