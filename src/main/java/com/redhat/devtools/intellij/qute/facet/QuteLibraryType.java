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

import com.intellij.facet.ui.FacetBasedFrameworkSupportProvider;
import com.intellij.framework.library.DownloadableLibraryType;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class QuteLibraryType extends DownloadableLibraryType {

    public QuteLibraryType() {
        super(() -> "Qute", "qute", "qute", QuteLibraryType.class.getResource("/qute.xml"));
    }

    @NotNull
    @Override
    protected String @NotNull [] getDetectionClassNames() {
        return new String[]{"io.quarkus.qute.Engine"};
    }

    @NotNull
    @Override
    public Icon getLibraryTypeIcon() {
        return IconLoader.getIcon("/quarkus_icon_rgb_16px_default.png", QuteLibraryType.class);
    }

    public final String getUnderlyingFrameworkTypeId() {
        return FacetBasedFrameworkSupportProvider.getProviderId(QuteFacet.FACET_TYPE_ID);
    }
}
