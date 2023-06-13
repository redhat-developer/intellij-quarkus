/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.qute.lang.highlighter;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.util.LayerDescriptor;
import com.intellij.openapi.editor.ex.util.LayeredLexerEditorHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.intellij.qute.lang.QuteFileViewProvider;
import com.redhat.devtools.intellij.qute.lang.psi.QuteElementTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuteEditorHighlighter extends LayeredLexerEditorHighlighter {

    public QuteEditorHighlighter(@Nullable Project project, @Nullable VirtualFile virtualFile, @NotNull EditorColorsScheme scheme) {
        super(new QuteSyntaxHighlighter(), scheme);
        if (virtualFile != null) {
            Language templateLanguage = QuteFileViewProvider.getTemplateLanguage(virtualFile);
            this.registerLayer(QuteElementTypes.QUTE_TEXT,
                    new LayerDescriptor(
                            SyntaxHighlighterFactory.getSyntaxHighlighter(templateLanguage, project, virtualFile),""));
        }
    }
}
