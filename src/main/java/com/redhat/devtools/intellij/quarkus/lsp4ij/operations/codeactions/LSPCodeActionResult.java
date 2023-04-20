package com.redhat.devtools.intellij.quarkus.lsp4ij.operations.codeactions;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;

import java.util.List;

public class LSPCodeActionResult {

    private final CodeActionContext context;

    private final List<Either<Command, CodeAction>> codeActions;

    private final LanguageServer languageServer;

    public LSPCodeActionResult(CodeActionContext context, List<Either<Command, CodeAction>> codeActions, LanguageServer languageServer) {
        this.context = context;
        this.codeActions = codeActions;
        this.languageServer = languageServer;
    }

    public boolean isAdapted(Diagnostic diagnostic) {
        return context.getDiagnostics() != null && context.getDiagnostics().contains(diagnostic);
    }

    public LanguageServer getLanguageServer() {
        return languageServer;
    }

    public List<Either<Command, CodeAction>> getCodeActions() {
        return codeActions;
    }
}
