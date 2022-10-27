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
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4mp.commons.DocumentFormat;

import java.io.IOException;

/**
 * PSI utils provides some helpful utilities.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/utils/IJDTUtils.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/utils/IJDTUtils.java</a>
 *
 */
public interface IPsiUtils {
    Project getProject();

    Module getModule();

    IPsiUtils refine(Module module);

    VirtualFile findFile(String uri) throws IOException;

    String getJavadoc(PsiMethod method, DocumentFormat documentFormat);

    Range toRange(PsiElement element, int offset, int length);

    Range toRange(Document document, int offset, int length);

    PsiFile resolveCompilationUnit(String uri);

    int toOffset(Document document, int line, int character);

    int toOffset(PsiFile file, int line, int character);

    Module getModule(VirtualFile file);

    Module getModule(String uri) throws IOException;

    PsiClass findClass(Module module, String className);

    void discoverSource(PsiFile classFile);

    Location toLocation(PsiElement fieldOrMethod);

    String toUri(PsiFile typeRoot);

    boolean isHiddenGeneratedElement(PsiElement element);

    PsiFile resolveClassFile(String uri);
}
