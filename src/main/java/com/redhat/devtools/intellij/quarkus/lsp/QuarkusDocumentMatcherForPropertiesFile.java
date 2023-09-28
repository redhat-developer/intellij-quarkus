package com.redhat.devtools.intellij.quarkus.lsp;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;

public class QuarkusDocumentMatcherForPropertiesFile extends AbstractQuarkusDocumentMatcher {

    @Override
    public boolean match(Document document, VirtualFile file, Project fileProject) {
        if (!matchFile(file, fileProject)) {
            return false;
        }
        return super.match(document, file, fileProject);
    }

    private boolean matchFile(VirtualFile file, Project fileProject) {
        return QuarkusModuleUtil.isQuarkusPropertiesFile(file, fileProject) ||  QuarkusModuleUtil.isQuarkusYAMLFile(file, fileProject);
    }
}
