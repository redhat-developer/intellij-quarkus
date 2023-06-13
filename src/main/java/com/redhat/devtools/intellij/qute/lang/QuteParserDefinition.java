/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.lang;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import com.redhat.devtools.intellij.qute.lang.psi.QuteLexer;
import com.redhat.devtools.intellij.qute.lang.psi.QuteParser;
import com.redhat.devtools.intellij.qute.lang.psi.QutePsiFile;
import com.redhat.devtools.intellij.qute.lang.psi.QuteElementTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Qute parser definition.
 */
public class QuteParserDefinition implements ParserDefinition {

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return new QuteLexer();
    }

    @Override
    public PsiParser createParser(Project project) {
        return new QuteParser();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return QuteElementTypes.QUTE_FILE;
    }

    @Override
    public @NotNull TokenSet getCommentTokens() {
        return TokenSet.create(QuteElementTypes.QUTE_COMMENT);
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements() {
        return TokenSet.create(QuteElementTypes.QUTE_STRING);
    }

    @Override
    public @NotNull PsiElement createElement(ASTNode node) {
        // This method seems never called, since whe have defined a QuteASTFactory.
        return new ASTWrapperPsiElement(node);
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new QutePsiFile(viewProvider);
    }
}
