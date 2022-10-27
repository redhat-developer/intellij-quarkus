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
import com.intellij.openapi.editor.event.DocumentEvent;

import java.util.List;

public abstract class ChangeCorrectionProposal {
    private String name;
    private final String kind;
    private final int relevance;

    public ChangeCorrectionProposal(String name, String kind, int relevance) {
        this.name = name;
        this.kind = kind;
        this.relevance = relevance;
    }

    public String getName() {
        return name;
    }

    public void setDisplayName(String name) {
        this.name = name;
    }

    public String getKind() {
        return kind;
    }

    public int getRelevance() {
        return relevance;
    }

    public abstract Change getChange();
}
