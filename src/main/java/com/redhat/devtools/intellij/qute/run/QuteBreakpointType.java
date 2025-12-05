/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.run;

import com.intellij.lang.Language;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.DocumentUtil;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.redhat.devtools.intellij.qute.lang.QuteLanguage;
import com.redhat.devtools.intellij.qute.lang.injector.QuteJavaInjectionRegistry;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.dap.breakpoints.DAPBreakpointTypeBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * Qute breakpoint type.
 */
public class QuteBreakpointType extends DAPBreakpointTypeBase<XBreakpointProperties<?>> {

    private static final String BREAKPOINT_ID = "qute-breakpoint";

    public QuteBreakpointType() {
        super(BREAKPOINT_ID, "Qute Breakpoint");
    }

    @Override
    public @Nullable XBreakpointProperties<?> createBreakpointProperties(@NotNull VirtualFile virtualFile, int line) {
        return null;
    }

    @Override
    public boolean canPutAt(@NotNull VirtualFile file, int line, @NotNull Project project) {
        Language language = LSPIJUtils.getFileLanguage(file, project);
        if (language == null) {
            return false;
        }
        if (QuteLanguage.isQuteLanguage(language)) {
            return true;
        }
        if (language.equals(JavaLanguage.INSTANCE)) {
            return canPutAtElement(file, line, project, (element, document) -> {
                if (element instanceof PsiAnnotation annotation) {
                    return QuteJavaInjectionRegistry.getInstance().getDescriptor(annotation) != null;
                }
                return false;
            });
        }
        return false;
    }

    private static boolean canPutAtElement(final @NotNull VirtualFile file,
                                           final int line,
                                           @NotNull Project project,
                                           @NotNull BiFunction<? super PsiElement, ? super Document, Boolean> processor) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return false;
        }
        Language language = psiFile.getLanguage();
        if (!JavaLanguage.INSTANCE.equals(language)) {
            return false;
        }

        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document != null) {
            Ref<Boolean> res = Ref.create(false);
            XDebuggerUtil.getInstance().iterateLine(project, document, line, element -> {
                // avoid comments
                if ((element instanceof PsiWhiteSpace)
                        || (PsiTreeUtil.getNonStrictParentOfType(element, PsiComment.class, PsiImportStatementBase.class, PsiPackageStatement.class) != null)) {
                    return true;
                }
                if (element instanceof PsiJavaToken javaToken && javaToken.getTokenType() == JavaTokenType.TEXT_BLOCK_LITERAL) {
                    PsiElement parent = element;
                    while (element != null) {
                        element = element.getParent();
                        if (element instanceof PsiAnnotation) {
                            if (processor.apply(element, document)) {
                                res.set(true);
                                return false;
                            }
                        }
                    }
                    return false;
                }
                if (element == JavaTokenType.AT) {
                    PsiElement prev = element.getPrevSibling();
                    if (prev instanceof PsiAnnotation) {
                        return true;
                    }
                }


                PsiElement parent = element;
                while (element != null) {
                    // skip modifiers

                    if (element instanceof PsiModifierList) {
                        element = element.getParent();
                        continue;
                    }

                    final int offset = element.getTextOffset();
                    if (!DocumentUtil.isValidOffset(offset, document) || document.getLineNumber(offset) != line) {
                        break;
                    }
                    parent = element;
                    element = element.getParent();
                }

                if (processor.apply(parent, document)) {
                    res.set(true);
                    return false;
                }
                return true;
            });
            return res.get();
        }
        return false;
    }

}
