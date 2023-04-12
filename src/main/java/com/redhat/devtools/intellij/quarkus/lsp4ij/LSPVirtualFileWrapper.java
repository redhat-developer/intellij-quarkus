package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.lang.annotation.Annotation;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.lsp4ij.operations.diagnostics.LSPDiagnosticHandler;
import org.eclipse.lsp4j.PublishDiagnosticsParams;

import java.util.List;
import java.util.Objects;

public class LSPVirtualFileWrapper {

    private static final Key<LSPVirtualFileWrapper> KEY = new Key(LSPVirtualFileWrapper.class.getName());

    private final VirtualFile file;

    private PublishDiagnosticsParams currentDiagnostics;

    private List<Annotation> annotations;
    private boolean updatingCodeAction;
    private boolean updatingDiagnostic;

    LSPVirtualFileWrapper(VirtualFile file) {
        this.file = file;
    }

    public VirtualFile getFile() {
        return file;
    }

    public static LSPVirtualFileWrapper getCache(VirtualFile file) {
        LSPVirtualFileWrapper cache = file.getUserData(KEY);
        if (cache != null) {
            return cache;
        }
        return getCacheSync(file);
    }

    private static synchronized LSPVirtualFileWrapper getCacheSync(VirtualFile file) {
        LSPVirtualFileWrapper cache = file.getUserData(KEY);
        if (cache != null) {
            return cache;
        }
        cache = new LSPVirtualFileWrapper(file);
        file.putUserData(KEY, cache);
        return cache;
    }

    public boolean update(PublishDiagnosticsParams params) {
        if (currentDiagnostics != null && Objects.deepEquals(params.getDiagnostics(), currentDiagnostics.getDiagnostics())) {
            return true;
        }
        currentDiagnostics = params;
        return false;
    }

    public PublishDiagnosticsParams getCurrentDiagnostics() {
        updatingDiagnostic=false;
        return currentDiagnostics;
    }

    public void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
    }

    public List<Annotation> getAnnotations() {
        updatingCodeAction = false;
        return annotations;
    }

    public void setUpdatingCodeAction(boolean updatingCodeAction) {
        this.updatingCodeAction = updatingCodeAction;
    }

    public boolean isUpdatingCodeAction() {
        return updatingCodeAction;
    }

    public boolean isUpdatingDiagnostic() {
        return updatingDiagnostic;
    }

    public void setUpdatingDiagnostic(boolean updatingDiagnostic) {
        this.updatingDiagnostic = updatingDiagnostic;
    }
}
