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
import com.intellij.openapi.project.DumbService;
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
 * <p>
 * Fully dumb-aware and safe during indexing.
 */
public class QuteBreakpointType extends DAPBreakpointTypeBase<XBreakpointProperties<?>> {

    private static final String BREAKPOINT_ID = "qute-breakpoint";

    public QuteBreakpointType() {
        super(BREAKPOINT_ID, "Qute Breakpoint");
    }

    private static boolean canPutAtElement(
            @NotNull VirtualFile file,
            int line,
            @NotNull Project project,
            @NotNull BiFunction<? super PsiElement, ? super Document, Boolean> processor
    ) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return false;
        }

        if (!JavaLanguage.INSTANCE.equals(psiFile.getLanguage())) {
            return false;
        }

        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            return false;
        }

        Ref<Boolean> result = Ref.create(false);

        XDebuggerUtil.getInstance().iterateLine(
                project,
                document,
                line,
                element -> {
                    // Skip whitespace, comments, imports, package
                    if (element instanceof PsiWhiteSpace ||
                            PsiTreeUtil.getNonStrictParentOfType(
                                    element,
                                    PsiComment.class,
                                    PsiImportStatementBase.class,
                                    PsiPackageStatement.class
                            ) != null) {
                        return true;
                    }

                    // Text block literal inside annotation
                    if (element instanceof PsiJavaToken javaToken &&
                            javaToken.getTokenType() == JavaTokenType.TEXT_BLOCK_LITERAL) {

                        PsiElement parent = element;
                        while (parent != null) {
                            parent = parent.getParent();
                            if (parent instanceof PsiAnnotation) {
                                if (processor.apply(parent, document)) {
                                    result.set(true);
                                }
                                return false;
                            }
                        }
                        return false;
                    }

                    PsiElement current = element;
                    PsiElement parent = element;

                    while (current != null) {
                        if (current instanceof PsiModifierList) {
                            current = current.getParent();
                            continue;
                        }

                        int offset = current.getTextOffset();
                        if (!DocumentUtil.isValidOffset(offset, document)
                                || document.getLineNumber(offset) != line) {
                            break;
                        }

                        parent = current;
                        current = current.getParent();
                    }

                    if (processor.apply(parent, document)) {
                        result.set(true);
                        return false;
                    }

                    return true;
                }
        );

        return result.get();
    }

    @Override
    public @Nullable XBreakpointProperties<?> createBreakpointProperties(
            @NotNull VirtualFile virtualFile,
            int line
    ) {
        return null;
    }

    @Override
    public boolean canPutAt(
            @NotNull VirtualFile file,
            int line,
            @NotNull Project project
    ) {
        // 🔒 CRITICAL: debugger calls this during indexing
        if (DumbService.isDumb(project)) {
            return false;
        }

        Language language = LSPIJUtils.getFileLanguage(file, project);
        if (language == null) {
            return false;
        }

        if (QuteLanguage.isQuteLanguage(language)) {
            return true;
        }

        if (JavaLanguage.INSTANCE.equals(language)) {
            return canPutAtElement(
                    file,
                    line,
                    project,
                    (element, document) -> {
                        if (element instanceof PsiAnnotation annotation) {
                            // Registry is already dumb-safe
                            return QuteJavaInjectionRegistry.getInstance()
                                    .getDescriptor(annotation) != null;
                        }
                        return false;
                    }
            );
        }

        return false;
    }
}
