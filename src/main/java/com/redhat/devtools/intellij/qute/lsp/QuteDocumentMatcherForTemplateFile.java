package com.redhat.devtools.intellij.qute.lsp;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.lsp4ij.LSPIJUtils;
import com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils;

import static com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils.isQuteTemplate;

public class QuteDocumentMatcherForTemplateFile extends AbstractQuteDocumentMatcher {

    @Override
    public boolean match(Document document, VirtualFile file, Project fileProject) {
        if (!super.match(document, file, fileProject)) {
            return false;
        }
        return isQuteTemplate(file, LSPIJUtils.getModule(file));
    }
}
