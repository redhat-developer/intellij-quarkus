/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4ij;

import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4ij.operations.diagnostics.LSPDiagnosticsForServer;
import com.redhat.devtools.intellij.lsp4ij.operations.documentLink.LSPDocumentLinkForServer;

/**
 * LSP data stored in {@link VirtualFile} which are used by some LSP operations.
 *
 * @author Angelo ZERR
 */
public class LSPVirtualFileData {

    private final LSPDiagnosticsForServer diagnosticsForServer;

    private final LSPDocumentLinkForServer documentLinkForServer;

    public LSPVirtualFileData(LanguageServerWrapper languageServerWrapper, VirtualFile file) {
        this.diagnosticsForServer = new LSPDiagnosticsForServer(languageServerWrapper,file);
        this.documentLinkForServer = new LSPDocumentLinkForServer(languageServerWrapper, file);
    }

    public LSPDiagnosticsForServer getLSPDiagnosticsForServer() {
        return diagnosticsForServer;
    }

    public LSPDocumentLinkForServer getDocumentLinkForServer() {
        return documentLinkForServer;
    }
}
