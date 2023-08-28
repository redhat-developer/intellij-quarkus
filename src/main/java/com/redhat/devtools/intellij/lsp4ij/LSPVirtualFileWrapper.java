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
package com.redhat.devtools.intellij.lsp4ij;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.redhat.devtools.intellij.lsp4ij.operations.diagnostics.LSPDiagnosticsForServer;
import com.redhat.devtools.intellij.lsp4ij.operations.documentLink.LSPDocumentLinkForServer;
import com.redhat.devtools.intellij.lsp4ij.operations.documentation.LSPTextHoverForFile;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.MarkupContent;

import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * LSP wrapper for VirtualFile which maintains the current diagnostics
 * for all language servers mapped with this file.
 *
 * @author Angelo ZERR
 */
public class LSPVirtualFileWrapper implements Disposable {

    private static final Key<LSPVirtualFileWrapper> KEY = new Key<>(LSPVirtualFileWrapper.class.getName());

    private final VirtualFile file;

    private final Map<LanguageServerWrapper, LSPVirtualFileData /* current LSP data (diagnostics, documentLink, etc) */> dataPerServer;
    private final LSPTextHoverForFile hover;

    LSPVirtualFileWrapper(VirtualFile file) {
        this.file = file;
        this.dataPerServer = new HashMap<>();
        this.hover = new LSPTextHoverForFile();
        Module project = LSPIJUtils.getModule(file);
        if (project != null) {
            Disposer.register(project, this);
        }
    }

    public VirtualFile getFile() {
        return file;
    }

    // ------------------------ LSP Diagnostics

    /**
     * Update the new published diagnostics for the given language server id.
     *
     * @param diagnostics           the new diagnostics list
     * @param languageServerWrapper the language server id which has published those diagnostics.
     */
    public void updateDiagnostics(List<Diagnostic> diagnostics, LanguageServerWrapper languageServerWrapper) {
        LSPDiagnosticsForServer diagnosticsForServer = getLSPVirtualFileData(languageServerWrapper).getLSPDiagnosticsForServer();
        diagnosticsForServer.update(diagnostics);
    }

    /**
     * Returns all current diagnostics reported by all language servers mapped with the file.
     *
     * @return all current diagnostics reported by all language servers mapped with the file.
     */
    public Collection<LSPDiagnosticsForServer> getAllDiagnostics() {
        return getData(LSPVirtualFileData::getLSPDiagnosticsForServer);
    }

    // ------------------------ LSP textDocument/documentLink

    /**
     * Update the new collected documentLinks for the given language server id.
     *
     * @param documentLinks           the new documentLink list
     * @param languageServerWrapper the language server id which has collected those documentLinks.
     */
    public void updateDocumentLink(List<DocumentLink> documentLinks, LanguageServerWrapper languageServerWrapper) {
        LSPDocumentLinkForServer documentLinkForServer = getLSPVirtualFileData(languageServerWrapper).getDocumentLinkForServer();
        documentLinkForServer.update(documentLinks);
    }

    /**
     * Returns all current document link reported by all language servers mapped with the file.
     *
     * @return all current document link reported by all language servers mapped with the file.
     */
    public Collection<LSPDocumentLinkForServer> getAllDocumentLink() {
        return getData(LSPVirtualFileData::getDocumentLinkForServer);
    }


    public List<MarkupContent> getHoverContent(PsiElement element, int targetOffset, Editor editor) {
        return hover.getHoverContent(element, targetOffset, editor);
    }

    // ------------------------ Other methods

    private LSPVirtualFileData getLSPVirtualFileData(LanguageServerWrapper languageServerWrapper) {
        LSPVirtualFileData dataForServer = dataPerServer.get(languageServerWrapper);
        if (dataForServer != null) {
            return dataForServer;
        }
        return getOrCreateLSPVirtualFileData(languageServerWrapper);
    }

    private synchronized LSPVirtualFileData getOrCreateLSPVirtualFileData(LanguageServerWrapper languageServerWrapper) {
        LSPVirtualFileData dataForServer = dataPerServer.get(languageServerWrapper);
        if (dataForServer != null) {
            return dataForServer;
        }
        dataForServer = new LSPVirtualFileData(languageServerWrapper, getFile());
        dataPerServer.put(languageServerWrapper, dataForServer);
        return dataForServer;
    }

    private <T> Collection<T> getData(Function<LSPVirtualFileData, ? extends T> mapper) {
        if (dataPerServer.isEmpty()) {
            return Collections.emptyList();
        }
        return dataPerServer
                .values()
                .stream()
                .map(mapper)
                .collect(Collectors.toList());
    }

    // ------------------------ Static accessor

    @Override
    public void dispose() {
        file.putUserData(KEY, null);
        this.dataPerServer.clear();
    }

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

    public static boolean hasWrapper(VirtualFile file) {
        return file != null && file.getUserData(KEY) != null;
    }

    public static void dispose(VirtualFile file) {
        LSPVirtualFileWrapper wrapper = file.getUserData(KEY);
        if (wrapper != null) {
            wrapper.dispose();
        }
    }

}