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
package com.redhat.devtools.intellij.quarkus.search.core.utils;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.redhat.microprofile.commons.DocumentFormat;
import org.eclipse.lsp4j.Range;

import java.net.URISyntaxException;

/**
 * PSI utils provides some helpful utilities.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/utils/IJDTUtils.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/utils/IJDTUtils.java</a>
 *
 */
public interface IPsiUtils {
    VirtualFile findFile(String uri) throws URISyntaxException;

    String getJavadoc(PsiMethod method, DocumentFormat documentFormat);

    Range toRange(PsiElement element, int offset, int length);

    PsiFile resolveCompilationUnit(String uri);

    int toOffset(Document document, int line, int character);

    int toOffset(PsiFile file, int line, int character);

    Module getModule(VirtualFile file);

    Module getModule(String uri) throws URISyntaxException;
}
