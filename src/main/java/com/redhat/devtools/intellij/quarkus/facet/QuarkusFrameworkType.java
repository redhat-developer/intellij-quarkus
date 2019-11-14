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

import com.intellij.framework.library.LibraryBasedFrameworkType;
import org.jetbrains.annotations.NotNull;

public class QuarkusFrameworkType extends LibraryBasedFrameworkType {
    protected QuarkusFrameworkType() {
        super("quarkus", QuarkusLibraryType.class);
    }

    @NotNull
    @Override
    public String getPresentableName() {
        return "Quarkus";
    }
}
