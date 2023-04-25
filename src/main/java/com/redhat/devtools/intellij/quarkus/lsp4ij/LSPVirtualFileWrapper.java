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
 *     Some piece of code has been inspired by https://github.com/ballerina-platform/lsp4intellij/blob/master/src/main/java/org/wso2/lsp4intellij/editor/EditorEventManager.java
 *******************************************************************************/
package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.lsp4ij.operations.diagnostics.LSPDiagnosticsForServer;
import org.eclipse.lsp4j.Diagnostic;

import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * LSP wrapper for VirtualFile which maintains the current diagnostics
 * for all language servers mapped with this file.
 *
 * @author Angelo ZERR
 */
public class LSPVirtualFileWrapper {

    private static final Key<LSPVirtualFileWrapper> KEY = new Key<>(LSPVirtualFileWrapper.class.getName());

    private final VirtualFile file;

    private final Map<LanguageServerWrapper, LSPDiagnosticsForServer /* current diagnostics */> diagnosticsPerServer;

    LSPVirtualFileWrapper(VirtualFile file) {
        this.file = file;
        this.diagnosticsPerServer = new HashMap<>();
    }

    public VirtualFile getFile() {
        return file;
    }

    // ------------------------ LSP Diagnostics

    /**
     * Update the new published diagnostics for the given language server id.
     *
     * @param diagnostics      the new diagnostics list
     * @param languageServerWrapper the language server id which has published those diagnostics.
     */
    public void updateDiagnostics(List<Diagnostic> diagnostics, LanguageServerWrapper languageServerWrapper) {
        LSPDiagnosticsForServer diagnosticsForServer = diagnosticsPerServer.get(languageServerWrapper);
        if (diagnosticsForServer == null) {
            diagnosticsForServer = new LSPDiagnosticsForServer(languageServerWrapper, getFile());
            diagnosticsPerServer.put(languageServerWrapper, diagnosticsForServer);
        }
        diagnosticsForServer.update(diagnostics);
    }

    /**
     * Returns all current diagnostics reported by all language servers mapped with the file.
     *
     * @return all current diagnostics reported by all language servers mapped with the file.
     */
    public Collection<LSPDiagnosticsForServer> getAllDiagnostics() {
        if (diagnosticsPerServer.isEmpty()) {
            return Collections.emptyList();
        }
        return diagnosticsPerServer.values();
    }

    // ------------------------ Static accessor

    /**
     * Returns the LSPVirtualFileWrapper for the given file.
     *
     * @param file the virtual file.
     * @return the LSPVirtualFileWrapper for the given file.
     */
    public static LSPVirtualFileWrapper getLSPVirtualFileWrapper(VirtualFile file) {
        LSPVirtualFileWrapper wrapper = file.getUserData(KEY);
        if (wrapper != null) {
            return wrapper;
        }
        return getOrCreateLSPVirtualFileWrapper(file);
    }

    private static synchronized LSPVirtualFileWrapper getOrCreateLSPVirtualFileWrapper(VirtualFile file) {
        LSPVirtualFileWrapper wrapper = file.getUserData(KEY);
        if (wrapper != null) {
            return wrapper;
        }
        wrapper = new LSPVirtualFileWrapper(file);
        file.putUserData(KEY, wrapper);
        return wrapper;
    }

}