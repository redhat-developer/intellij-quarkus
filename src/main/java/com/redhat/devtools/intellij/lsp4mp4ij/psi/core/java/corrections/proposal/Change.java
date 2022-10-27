/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal;

import com.intellij.openapi.editor.Document;
import org.jetbrains.annotations.Nullable;

public class Change {
    @Nullable
    private final Document sourceDocument;
    private final Document targetDocument;

    public Change(@Nullable Document sourceDocument, Document targetDocument) {
        this.sourceDocument = sourceDocument;
        this.targetDocument = targetDocument;
    }

    @Nullable
    public Document getSourceDocument() {
        return sourceDocument;
    }

    public Document getTargetDocument() {
        return targetDocument;
    }
}
