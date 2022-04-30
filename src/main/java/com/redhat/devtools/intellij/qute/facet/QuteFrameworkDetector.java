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

import com.intellij.facet.FacetType;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.framework.detection.FileContentPattern;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.patterns.ElementPattern;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;

public class QuteFrameworkDetector extends FacetBasedFrameworkDetector<QuteFacet, QuteFacetConfiguration> {

    public QuteFrameworkDetector() {
        super("qute", 1);
    }

    @NotNull
    @Override
    public FacetType<QuteFacet, QuteFacetConfiguration> getFacetType() {
        return FacetType.findInstance(QuteFacetType.class);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return StdFileTypes.XML;
    }

    @NotNull
    @Override
    public ElementPattern<FileContent> createSuitableFilePattern() {
        return FileContentPattern.fileContent().withName("DUMMY_WILL_NEVER_DETECT");
    }
}
