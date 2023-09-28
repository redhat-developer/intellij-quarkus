package com.redhat.devtools.intellij.quarkus.lsp;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4ij.DocumentMatcher;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils;

public class AbstractQuarkusDocumentMatcher implements DocumentMatcher {
    @Override
    public boolean match(Document document, VirtualFile file, Project fileProject) {
        Module module = LSPIJUtils.getModule(file);
        return module != null &&  QuarkusModuleUtil.isQuarkusModule(module);
    }
}
