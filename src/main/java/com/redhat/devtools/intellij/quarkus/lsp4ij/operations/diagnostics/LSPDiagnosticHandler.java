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
package com.redhat.devtools.intellij.quarkus.lsp4ij.operations.diagnostics;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPVirtualFileWrapper;
import org.eclipse.lsp4j.PublishDiagnosticsParams;

import java.util.function.Consumer;

/**
 * Utility class which receive LSP {@link PublishDiagnosticsParams}
 * from a language server and refresh the Annotation of the Intellij editor.
 *
 * @author Angelo ZERR
 */
public class LSPDiagnosticHandler implements Consumer<PublishDiagnosticsParams> {

    private final String languageServerId;

    public LSPDiagnosticHandler(String languageServerId) {
        this.languageServerId = languageServerId;
    }

    @Override
    public void accept(PublishDiagnosticsParams params) {
        ApplicationManager.getApplication().runReadAction(() -> {
            VirtualFile file = LSPIJUtils.findResourceFor(params.getUri());
            if (file == null) {
                return;
            }
            Module module = LSPIJUtils.getProject(file);
            if (module == null) {
                return;
            }
            Project project = module.getProject();
            final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile == null) {
                return;
            }
            LSPVirtualFileWrapper wrapper = LSPVirtualFileWrapper.getLSPVirtualFileWrapper(file);
            synchronized (wrapper) {
                // Update LSP diagnostic reported by the language server id
                wrapper.updateDiagnostics(params.getDiagnostics(), languageServerId);
            }
            // Trigger Intellij validation to execute
            // {@link com.redhat.devtools.intellij.quarkus.lsp4ij.operations.diagnostics.LSPDiagnosticAnnotator}.
            // which translates LSP Diagnostics into Intellij Annotation
            DaemonCodeAnalyzer.getInstance(module.getProject()).restart(psiFile);
        });
    }
}