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
package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.lang.annotation.Annotation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.quarkus.lsp4ij.operations.codeactions.LSPCodeActionIntentionAction;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * LSP wrapper for VirtualFile which maintains the current diagnostics
 * for all language servers mapped with this file.
 *
 * @author Angelo ZERR
 */
public class LSPVirtualFileWrapper {

    private static final Key<LSPVirtualFileWrapper> KEY = new Key(LSPVirtualFileWrapper.class.getName());

    private final VirtualFile file;

    private final Map<String /* language server id */, List<Diagnostic> /* current diagnostics */> diagnosticsPerServer;

    private boolean diagnosticDirty;

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
     * @param languageServerId the language server id which has published those diagnostics.
     */
    public void updateDiagnostics(List<Diagnostic> diagnostics, String languageServerId) {
        diagnosticDirty = true;
        diagnosticsPerServer.put(languageServerId, diagnostics);
    }

    /**
     * Returns all current diagnostics reported by all language servers mapped with the file.
     *
     * @return all current diagnostics reported by all language servers mapped with the file.
     */
    public List<Diagnostic> getAllDiagnostics() {
        diagnosticDirty = false;
        if (diagnosticsPerServer.isEmpty()) {
            return Collections.emptyList();
        }
        return diagnosticsPerServer.values()
                .stream()
                .flatMap(diagnostics -> diagnostics.stream())
                .collect(Collectors.toList());
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
        return getLSPVirtualFileWrapperSync(file);
    }

    private static synchronized LSPVirtualFileWrapper getLSPVirtualFileWrapperSync(VirtualFile file) {
        LSPVirtualFileWrapper wrapper = file.getUserData(KEY);
        if (wrapper != null) {
            return wrapper;
        }
        wrapper = new LSPVirtualFileWrapper(file);
        file.putUserData(KEY, wrapper);
        return wrapper;
    }

}