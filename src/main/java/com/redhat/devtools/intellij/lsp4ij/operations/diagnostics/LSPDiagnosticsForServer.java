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
package com.redhat.devtools.intellij.lsp4ij.operations.diagnostics;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.lsp4ij.CompletableFutures;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.lsp4ij.LanguageServerWrapper;
import com.redhat.devtools.intellij.lsp4ij.operations.codeactions.LSPCodeActionIntentionAction;
import org.eclipse.lsp4j.*;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * LSP diagnostics holder for a file reported by a language server. This class holds:
 *
 * <ul>
 *     <li>the current LSP diagnostics reported by the language server.</li>
 *     <li>load for each diagnostic the available LSP code actions (QuickFix)</li>
 * </ul>
 *
 * @author Angelo ZERR
 */
public class LSPDiagnosticsForServer {

    private static final List<IntentionAction> CODE_ACTIONS_LOADING = new ArrayList<>();

    private final LanguageServerWrapper languageServerWrapper;

    private final boolean codeActionSupported;

    private final VirtualFile file;

    // Map which contains all current diagnostics (as key) and future which load associated quick fixes (as value)
    private Map<Diagnostic, CompletableFuture<List<IntentionAction>>> diagnostics;

    // Future which refreshes Intellij QuickFixes when all code actions of all diagnostics are loaded.
    private CompletableFuture<Void> refreshQuickFixes;

    public LSPDiagnosticsForServer(LanguageServerWrapper languageServerWrapper, VirtualFile file) {
        this.languageServerWrapper = languageServerWrapper;
        this.codeActionSupported = isCodeActionSupported(languageServerWrapper);
        this.file = file;
        this.diagnostics = Collections.emptyMap();
    }

    private static boolean isCodeActionSupported(LanguageServerWrapper languageServerWrapper) {
        if (!languageServerWrapper.isActive() || languageServerWrapper.isStopping()) {
            // This use-case comes from when a diagnostics is published and the language server is stopped
            // We cannot use here languageServerWrapper.getServerCapabilities() otherwise it will restart the language server.
            return false;
        }
        ServerCapabilities serverCapabilities = languageServerWrapper.getServerCapabilities();
        return serverCapabilities != null && LSPIJUtils.hasCapability(serverCapabilities.getCodeActionProvider());
    }

    /**
     * Update the new LSP published diagnosics.
     *
     * @param diagnostics the new LSP published diagnosics
     */
    public void update(List<Diagnostic> diagnostics) {
        // cancel futures
        cancel(diagnostics);
        // initialize diagnostics map
        this.diagnostics = toMap(diagnostics, this.diagnostics);
    }

    private static Map<Diagnostic, CompletableFuture<List<IntentionAction>>> toMap(List<Diagnostic> diagnostics, Map<Diagnostic, CompletableFuture<List<IntentionAction>>> existingsDiagnostics) {
        Map<Diagnostic, CompletableFuture<List<IntentionAction>>> map = new HashMap<>(diagnostics.size());
        for (Diagnostic diagnostic : diagnostics) {
            // Try to get existing code actions for the given diagnostic to avoid reloading them
            CompletableFuture<List<IntentionAction>> existingQuickFixesFuture = existingsDiagnostics.get(diagnostic);
            if (existingQuickFixesFuture != null && (existingQuickFixesFuture.isCancelled() || existingQuickFixesFuture.isCompletedExceptionally())) {
                // The existing quickfixes future has been cancelled or completed with exception,don't reuse it.
                existingQuickFixesFuture = null;
            }
            map.put(diagnostic, existingQuickFixesFuture);
        }
        return map;
    }

    private void cancel(List<Diagnostic> newDiagnostics) {
        // Cancel futures which have loaded code actions
        for (Map.Entry<Diagnostic, CompletableFuture<List<IntentionAction>>> entry : this.diagnostics.entrySet()) {
            Diagnostic diagnostic = entry.getKey();
            CompletableFuture<List<IntentionAction>> quickFixesFuture = entry.getValue();
            if (shouldCancelQuickFixFuture(quickFixesFuture, diagnostic, newDiagnostics)) {
                quickFixesFuture.cancel(true);
            }
        }

        // Cancel the future which refreshes the validation again to takes care of QuickFix which are loaded.
        if (refreshQuickFixes != null) {
            refreshQuickFixes.cancel(true);
            refreshQuickFixes = null;
        }
    }

    private static boolean shouldCancelQuickFixFuture(CompletableFuture<List<IntentionAction>> quickFixesFuture, Diagnostic diagnostic, List<Diagnostic> newDiagnostics) {
        if (quickFixesFuture == null) {
            // The quickfix future is null, don't cancel it.
            return false;
        }
        if (!newDiagnostics.contains(diagnostic)) {
            // The quickfix future doesn't belong to the new reported diagnostics, cancel it.
            return true;
        }
        // Reuse the quickfix future
        return false;
    }

    /**
     * Refresh Intellij QuickFixes when all code actions for all diagnostics are loaded.
     */
    public void refreshQuickFixesIfNeeded() {
        refreshQuickFixes = CompletableFutures
                .computeAsyncCompose(cancelChecker -> {
                    return CompletableFuture.allOf(diagnostics
                            .values()
                            .toArray(new CompletableFuture[diagnostics.values().size()]))
                            .thenRun(() -> {
                                // All quickfix are loaded
                                if (cancelChecker.isCanceled()) {
                                    return;
                                }
                                ApplicationManager.getApplication().runReadAction(() -> {
                                    Project project = LSPIJUtils.getProject(file);
                                    if (project.isDisposed()) {
                                        return;
                                    }
                                    Document document = LSPIJUtils.getDocument(file);

                                    final PsiFile file = PsiDocumentManager.getInstance(project)
                                            .getCachedPsiFile(document);
                                    if (file == null) {
                                        return;
                                    }
                                    if (cancelChecker.isCanceled()) {
                                        return;
                                    }
                                    // Refresh the Intellij validation to update quickfixes
                                    DaemonCodeAnalyzer.getInstance(project).restart(file);
                                });
                            });
                });

    }

    /**
     * Returns the current diagnostics for the file reported by the language server.
     *
     * @return
     */
    public Set<Diagnostic> getDiagnostics() {
        return diagnostics.keySet();
    }

    /**
     * Returns Intellij quickfixes for the given diagnostic if there available.
     *
     * @param diagnostic the diagnostic.
     * @return Intellij quickfixes for the given diagnostic if there available.
     */
    public List<IntentionAction> getQuickFixesFor(Diagnostic diagnostic) {
        if (!codeActionSupported || diagnostics.isEmpty()) {
            return Collections.emptyList();
        }
        // Get future which load QuickFix for the given diagnostic
        CompletableFuture<List<IntentionAction>> fixes = diagnostics.get(diagnostic);
        if (!CompletableFutures.isDoneNormally(fixes)) {
            // Load code actions
            synchronized (diagnostics) {
                fixes = loadCodeActionsFor(diagnostic);
                diagnostics.put(diagnostic, fixes);
            }
        }
        // Try to get code action from the future now
        List<IntentionAction> result = fixes.getNow(null);
        if (result != null) {
            return result;
        }
        // Specify that code actions are loading.
        return CODE_ACTIONS_LOADING;
    }

    /**
     * Returns true if the given result is a code actions loading and false otherwise.
     *
     * @param result the code actions.
     *
     * @return true if the given result is a code actions loading and false otherwise.
     */
    public static boolean isCodeActionsLoading(List<IntentionAction> result) {
        return result == CODE_ACTIONS_LOADING;
    }

    /**
     * load code actions for the given diagnostic.
     *
     * @param diagnostic the LSP diagnostic.
     * @return list of Intellij {@link IntentionAction} which are used to create Intellij QuickFix.
     */
    private CompletableFuture<List<IntentionAction>> loadCodeActionsFor(Diagnostic diagnostic) {
        return CompletableFutures
                .computeAsyncCompose(cancelChecker -> {
                    return languageServerWrapper
                            .getInitializedServer()
                            .thenCompose(ls -> {
                                // Language server is initialized here
                                cancelChecker.checkCanceled();

                                // Collect code action for the given file by using the language server
                                CodeActionParams params = createCodeActionParams(diagnostic, file);
                                return ls.getTextDocumentService()
                                        .codeAction(params)
                                        .thenApply(codeActions -> {
                                            // Code action are collected here
                                            cancelChecker.checkCanceled();
                                            if (codeActions == null || codeActions.isEmpty()) {
                                                return Collections.emptyList();
                                            }
                                            // Translate LSP code action  into Intellij IntentionAction
                                            return codeActions
                                                    .stream()
                                                    .map(ca -> new LSPCodeActionIntentionAction(ca, languageServerWrapper))
                                                    .collect(Collectors.toList());
                                        });
                            });
                });
    }


    private static CodeActionParams createCodeActionParams(Diagnostic diagnostic, VirtualFile file) {
        CodeActionParams params = new CodeActionParams();
        URI fileUri = LSPIJUtils.toUri(file);
        params.setTextDocument(LSPIJUtils.toTextDocumentIdentifier(fileUri));
        Range range = diagnostic.getRange();
        params.setRange(range);

        CodeActionContext context = new CodeActionContext(Arrays.asList(diagnostic));
        params.setContext(context);
        return params;
    }
}
