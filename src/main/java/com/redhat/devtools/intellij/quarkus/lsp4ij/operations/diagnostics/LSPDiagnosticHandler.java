package com.redhat.devtools.intellij.quarkus.lsp4ij.operations.diagnostics;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LSPVirtualFileWrapper;
import org.eclipse.lsp4j.PublishDiagnosticsParams;

import java.util.Objects;

public class LSPDiagnosticHandler {

    public static void publishDiagnostics(PublishDiagnosticsParams params) {
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
            LSPVirtualFileWrapper cache = LSPVirtualFileWrapper.getCache(file);
            synchronized (cache) {
                if (cache.update(params)) {
                    return;
                }
            }
            cache.setUpdatingDiagnostic(true);
            DaemonCodeAnalyzer.getInstance(module.getProject()).restart(psiFile);
        });
    }
}
