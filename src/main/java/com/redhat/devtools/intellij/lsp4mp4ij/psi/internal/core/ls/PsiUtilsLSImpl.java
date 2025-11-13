/*******************************************************************************
 * Copyright (c) 2019-2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightRecordField;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.ClassUtil;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.JsonRpcHelpers;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.quarkus.javadoc.JavadocContentAccess;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.Scanner;

/**
 * {@link IPsiUtils} implementation.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/ls/JDTUtilsLSImpl.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/internal/core/ls/JDTUtilsLSImpl.java</a>
 */
public class PsiUtilsLSImpl implements IPsiUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsiUtilsLSImpl.class);
    private final Project project;
    private final Module module;

    public static IPsiUtils getInstance(Project project) {
        return new PsiUtilsLSImpl(project);
    }

    private PsiUtilsLSImpl(Project project, Module module) {
        this.project = project;
        this.module = module;
    }

    private PsiUtilsLSImpl(Project project) {
        this(project, null);
    }

    public Project getProject() {
        return project;
    }

    public Module getModule() {
        return module;
    }

    public IPsiUtils refine(Module module) {
        return new PsiUtilsLSImpl(module.getProject(), module);
    }

    @Override
    public Module getModule(VirtualFile file) {
        return LSPIJUtils.getModule(file, project);
    }

    @Override
    public Module getModule(String uri) throws IOException {
        VirtualFile file = findFile(uri);
        return file != null ? getModule(file) : null;
    }

    @Override
    public PsiClass findClass(Module module, String className) {
        return ReadAction
            .compute(() -> ClassUtil.findPsiClass(PsiManager.getInstance(module.getProject()), className, null, false,
                GlobalSearchScope.allScope(module.getProject())));
    }

    @Override
    public void discoverSource(PsiFile classFile) {
        //TODO: implements discoverSource
    }

    @Override
    public Location toLocation(PsiElement psiMember) {
        PsiElement sourceElement = getNavigationElement(psiMember);

        if (sourceElement != null) {
            PsiFile file = sourceElement.getContainingFile();
            Document document = PsiDocumentManager.getInstance(psiMember.getProject()).getDocument(file);
            if (document != null) {
                TextRange range = null;
                if (sourceElement instanceof PsiNameIdentifierOwner nameIdentifierOwner) {
                    var nameIdentifier = nameIdentifierOwner.getNameIdentifier();
                    if (nameIdentifier != null) {
                        range = nameIdentifier.getTextRange();
                    }
                }
                if (range == null) {
                    range = sourceElement.getTextRange();
                }
                return toLocation(file, LSPIJUtils.toRange(range, document));
            }
        }
        return null;
    }

    public static Location toLocation(PsiFile file, Range range) {
        return toLocation(file.getVirtualFile(), range);
    }

    public static Location toLocation(VirtualFile file, Range range) {
        return new Location(LSPIJUtils.toUriAsString(file), range);
    }

    private static @Nullable PsiElement getNavigationElement(PsiElement psiMember) {
        if (psiMember instanceof LightRecordField psiRecord) {
            psiMember = psiRecord.getRecordComponent();
        }
        return psiMember.getNavigationElement();
    }

    @Override
    public VirtualFile findFile(String uri) throws IOException {
        return LSPIJUtils.findResourceFor(uri);
    }

    @Override
    public String getJavadoc(PsiMethod method, DocumentFormat documentFormat) {
        boolean markdown = DocumentFormat.Markdown.equals(documentFormat);
        Reader reader = markdown ? JavadocContentAccess.getMarkdownContentReader(method)
                : JavadocContentAccess.getPlainTextContentReader(method);
        return reader != null ? toString(reader) : null;
    }

    @Override
    public String getJavadoc(PsiMember method, com.redhat.qute.commons.DocumentFormat documentFormat) {
        boolean markdown = DocumentFormat.Markdown.name().toLowerCase().equals(documentFormat.name().toLowerCase());
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
    public Range toRange(Document document, int offset, int length) {
        return PsiUtils.toRange(document, offset, length);
    }

    @Override
    public int toOffset(Document document, int line, int character) {
        return JsonRpcHelpers.toOffset(document, line, character);
    }

    @Override
    public int toOffset(PsiFile file, int line, int character) {
        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        return document != null ? toOffset(document, line, character) : 0;
    }

    @Override
    public PsiFile resolveCompilationUnit(String uri) {
        try {
            VirtualFile file = findFile(uri);
            if (file != null) {
                Module module = getModule(file);
                if (module == null) {
                    return null;
                }
                return PsiManager.getInstance(module.getProject()).findFile(file);
            }
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
        return null;
    }

    @Override
    public PsiFile resolveClassFile(String uri) {
        return resolveCompilationUnit(uri);
    }

    public static ClasspathKind getClasspathKind(VirtualFile file, Module module) {
        if (module != null) {
            return ReadAction.compute(() ->
                    ModuleRootManager.getInstance(module).getFileIndex().isInTestSourceContent(file) ?
                            ClasspathKind.TEST : ClasspathKind.SRC);
        }
        return ClasspathKind.NONE;
    }

    public static String getProjectURI(Module module) {
        return LSPIJUtils.getProjectUri(module);
    }

    @Override
    public String toUri(PsiFile typeRoot) {
        return LSPIJUtils.toUriAsString(typeRoot);
    }

    @Override
    public boolean isHiddenGeneratedElement(PsiElement element) {
        return PsiUtils.isHiddenGeneratedElement(element);
    }
}