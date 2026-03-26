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
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.redhat.devtools.lsp4ij.dap.DAPDebugProcess;
import com.redhat.devtools.lsp4ij.dap.DAPDebuggerEditorsProvider;
import com.redhat.devtools.lsp4ij.dap.evaluation.DAPExpressionCodeFragment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Qute debugger editors provider.
 */
public class QuteDebuggerEditorsProvider extends DAPDebuggerEditorsProvider {

    public QuteDebuggerEditorsProvider(@Nullable FileType fileType, @NotNull DAPDebugProcess debugProcess) {
        super(fileType, debugProcess);
    }

    @Override
    protected PsiFile createExpressionCodeFragment(@NotNull Project project,
                                                   @NotNull String text,
                                                   @Nullable PsiElement context,
                                                   boolean isPhysical) {
        // Here we force file and language to plaintext to have a proper completion prefix.
        // to show correctly properties/methods completion of an object part
        FileType fileType = PlainTextFileType.INSTANCE;
        Language language = PlainTextLanguage.INSTANCE;
        return super.createExpressionCodeFragment(project, text, fileType, language);
    }

}
