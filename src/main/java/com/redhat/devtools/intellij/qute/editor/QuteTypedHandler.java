/*******************************************************************************
 * Copyright (c) 2023 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.editor;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.redhat.devtools.intellij.qute.lang.QuteLanguage;
import com.redhat.devtools.intellij.qute.lang.psi.QuteElementType;
import com.redhat.devtools.intellij.qute.lang.psi.QuteTokenType;
import org.jetbrains.annotations.NotNull;

/**
 * Handler for triggering autoclose / autocompletion on Qute templates.
 */
public class QuteTypedHandler extends TypedHandlerDelegate {

  public static final String OPENING_BRACE = "{";
  public static final String CLOSING_BRACE = "}";

  @Override
  public @NotNull Result charTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    FileViewProvider provider = file.getViewProvider();

    if (!file.getLanguage().isKindOf(QuteLanguage.INSTANCE) && !provider.getBaseLanguage().isKindOf(QuteLanguage.INSTANCE)) {
      return Result.CONTINUE;
    }

    // Get the current caret position
    int offset = editor.getCaretModel().getOffset();
    int textLength = editor.getDocument().getTextLength();
    if (offset > textLength) {
      return Result.CONTINUE;
    }
    //Auto-close {|} and trigger completion popup on
    if (c == '{') {
      String nextChar = null;
      if (textLength > offset) {
        nextChar = editor.getDocument().getText(new TextRange(offset, offset+1));
      }
      // Insert the closing brace after the caret, if the next character is not already a closing brace
      if (!CLOSING_BRACE.equals(nextChar)) {
        editor.getDocument().insertString(offset, CLOSING_BRACE);
      }
      // Trigger Qute autocompletion
      AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
      return Result.STOP;
    }
    //Trigger completion popup on {#
    else if (c == '#' && offset > 1) {
      String previousChar = editor.getDocument().getText(new TextRange(offset - 2, offset - 1));
      if (OPENING_BRACE.equals(previousChar)) {
        AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
        return Result.STOP;
      }
    }
    // Auto-close qute expression when typing /
    else if (c == '/' && offset > 1) {
      PsiElement elementAtCaret = provider.findElementAt(offset - 1, QuteLanguage.class);
      if (elementAtCaret != null) {
        ASTNode node = elementAtCaret.getNode();
        if (node != null && QuteTokenType.QUTE_EXPRESSION_OBJECT_PART.equals(node.getElementType())){
          String nextChar = null;
          if (textLength > offset) {
            nextChar = editor.getDocument().getText(new TextRange(offset, offset+1));
          }
          // Insert the closing brace after the caret, if the next character is not already a closing brace
          if (!CLOSING_BRACE.equals(nextChar)) {
            editor.getDocument().insertString(offset, CLOSING_BRACE);
            return Result.STOP;
          }
        }
      }
    }

    return Result.CONTINUE;
  }
}