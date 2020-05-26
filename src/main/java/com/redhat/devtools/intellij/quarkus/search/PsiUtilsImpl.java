/*******************************************************************************
 * Copyright (c) 2019-2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.search;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.redhat.devtools.intellij.quarkus.javadoc.JavadocContentAccess;
import com.redhat.devtools.intellij.quarkus.search.core.utils.IPsiUtils;
import com.redhat.microprofile.commons.ClasspathKind;
import com.redhat.microprofile.commons.DocumentFormat;
import org.eclipse.lsp4j.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * {@link IPsiUtils} implementation.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/ls/JDTUtilsLSImpl.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/ls/JDTUtilsLSImpl.java</a>
 */
public class PsiUtilsImpl implements IPsiUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsiUtilsImpl.class);

    private static final IPsiUtils INSTANCE = new PsiUtilsImpl();

    public static IPsiUtils getInstance() {
        return INSTANCE;
    }

    private PsiUtilsImpl() {
    }

    @Override
    public Module getModule(VirtualFile file) {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            Module module = ProjectFileIndex.getInstance(project).getModuleForFile(file);
            if (module != null) {
                return module;
            }
        }
        return null;
    }

    @Override
    public Module getModule(String uri) throws URISyntaxException {
        VirtualFile file = findFile(uri);
        return file!=null?getModule(file):null;
    }

    @Override
    public VirtualFile findFile(String uri) throws URISyntaxException {
        return LocalFileSystem.getInstance().findFileByIoFile(Paths.get(new URI(uri)).toFile());
    }

    @Override
    public String getJavadoc(PsiMethod method, DocumentFormat documentFormat) {
        boolean markdown = DocumentFormat.Markdown.equals(documentFormat);
        Reader reader = markdown ? JavadocContentAccess.getMarkdownContentReader(method)
                : JavadocContentAccess.getPlainTextContentReader(method);
        return reader != null ? toString(reader) : null;
    }

    private static String toString(Reader reader) {
        try (Scanner s = new Scanner(reader)) {
            s.useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    @Override
    public Range toRange(PsiElement element, int offset, int length) {
        return PsiUtils.toRange(element, offset, length);
    }

    @Override
    public int toOffset(Document document, int line, int character) {
        return JsonRpcHelpers.toOffset(document, line, character);
    }

    @Override
    public int toOffset(PsiFile file, int line, int character) {
        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        return document!=null?toOffset(document, line, character):0;
    }

    @Override
    public PsiFile resolveCompilationUnit(String uri) {
        try {
            VirtualFile file = findFile(uri);
            if (file != null) {
                return PsiManager.getInstance(getModule(file).getProject()).findFile(file);
            }
        } catch (URISyntaxException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return null;
    }

    public static ClasspathKind getClasspathKind(VirtualFile file, Module module) {
        return ModuleRootManager.getInstance(module).getFileIndex().isInTestSourceContent(file)?ClasspathKind.TEST:ClasspathKind.SRC;
    }

    public static String getProjectURI(Module module) {
        return module.getModuleFilePath();
    }
}
