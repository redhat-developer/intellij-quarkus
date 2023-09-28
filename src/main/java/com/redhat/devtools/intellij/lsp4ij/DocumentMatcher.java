package com.redhat.devtools.intellij.lsp4ij;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public interface DocumentMatcher {

    boolean match(Document document, VirtualFile file, Project fileProject);
}
