package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.quarkus.lsp4ij.operations.diagnostics.LSPDiagnosticHandler;
import com.redhat.devtools.intellij.quarkus.lsp4ij.operations.diagnostics.LSPDiagnosticsToMarkers;
import org.eclipse.lsp4j.ApplyWorkspaceEditParams;
import org.eclipse.lsp4j.ApplyWorkspaceEditResponse;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.UnregistrationParams;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LanguageClientImpl implements LanguageClient {
    private final Project project;
    private LSPDiagnosticsToMarkers diagnosticHandler;

    private LanguageServer server;
    private LanguageServerWrapper wrapper;

    public LanguageClientImpl(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public final void connect(LanguageServer server, LanguageServerWrapper wrapper) {
        this.server = server;
        this.wrapper = wrapper;
        this.diagnosticHandler = new LSPDiagnosticsToMarkers(wrapper.serverDefinition.id);
    }

    protected final LanguageServer getLanguageServer() {
        return server;
    }

    @Override
    public void telemetryEvent(Object object) {
        // TODO
    }

    @Override
    public final CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
        return ServerMessageHandler.showMessageRequest(wrapper, requestParams);
    }

    @Override
    public final void showMessage(MessageParams messageParams) {
        ServerMessageHandler.showMessage(wrapper.serverDefinition.label, messageParams);
    }

    @Override
    public final void publishDiagnostics(PublishDiagnosticsParams params) {
        LSPDiagnosticHandler.publishDiagnostics(params);
    }

    @Override
    public final void logMessage(MessageParams message) {
        CompletableFuture.runAsync(() -> ServerMessageHandler.logMessage(wrapper, message));
    }

    @Override
    public final CompletableFuture<ApplyWorkspaceEditResponse> applyEdit(ApplyWorkspaceEditParams params) {
        CompletableFuture<ApplyWorkspaceEditResponse> future = new CompletableFuture<>();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            LSPIJUtils.applyWorkspaceEdit(params.getEdit());
            future.complete(new ApplyWorkspaceEditResponse(true));
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> registerCapability(RegistrationParams params) {
        return CompletableFuture.runAsync(() -> wrapper.registerCapability(params));
    }

    @Override
    public CompletableFuture<Void> unregisterCapability(UnregistrationParams params) {
        return CompletableFuture.runAsync(() -> wrapper.unregisterCapability(params));
    }

    @Override
    public CompletableFuture<List<WorkspaceFolder>> workspaceFolders() {
        List<WorkspaceFolder> res = new ArrayList<>(wrapper.allWatchedProjects.size());
        for (final Module project : wrapper.allWatchedProjects) {
            res.add(LSPIJUtils.toWorkspaceFolder(project));
        }
        return CompletableFuture.completedFuture(res);
    }

}
