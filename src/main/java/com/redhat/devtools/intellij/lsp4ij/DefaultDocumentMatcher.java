package com.redhat.devtools.intellij.lsp4ij;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class DefaultDocumentMatcher implements DocumentMatcher{

    public static final DocumentMatcher INSTANCE = new DefaultDocumentMatcher();

    @Override
    public boolean match(Document document, VirtualFile file, Project fileProject) {
       return true;
    }
}
