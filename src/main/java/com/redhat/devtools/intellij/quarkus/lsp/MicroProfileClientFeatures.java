/*******************************************************************************
 * Copyright (c) 2026 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.lsp;

import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures;

/**
 * MicroProfile client features.
 */
public class MicroProfileClientFeatures extends LSPClientFeatures {

    public MicroProfileClientFeatures() {
        super.setBreadcrumbsFeature(new MicroProfileBreadcrumbsFeature());
    }
}
