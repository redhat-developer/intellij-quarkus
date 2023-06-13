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

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.MultiMap;
import com.redhat.devtools.intellij.qute.lang.psi.QuteLexer;
import com.redhat.devtools.intellij.qute.lang.psi.QuteElementTypes;
import com.redhat.devtools.intellij.qute.lang.psi.QuteTokenType;
import org.jetbrains.annotations.NotNull;

import static com.redhat.devtools.intellij.qute.lang.highlighter.QuteHighlighterColors.*;

/**
 * Qyte syntax highlighter.
 */
public class QuteSyntaxHighlighter extends SyntaxHighlighterBase  {

    private static final MultiMap<IElementType, TextAttributesKey> ourMap = MultiMap.create();

    static {
        // Qute Comments
        ourMap.putValue(QuteTokenType.QUTE_COMMENT_START, COMMENT);
        ourMap.putValue(QuteTokenType.QUTE_COMMENT_END, COMMENT);
        ourMap.putValue(QuteElementTypes.QUTE_COMMENT, COMMENT);

        // Qute expression (ex: {foo}
        ourMap.putValue(QuteTokenType.QUTE_START_EXPRESSION, QUTE_EDGE);
        ourMap.putValue(QuteTokenType.QUTE_END_EXPRESSION, QUTE_EDGE);

        // String
        ourMap.putValue(QuteTokenType.QUTE_EXPRESSION_START_STRING, STRING);
        ourMap.putValue(QuteTokenType.QUTE_EXPRESSION_STRING, STRING);
        ourMap.putValue(QuteTokenType.QUTE_EXPRESSION_END_STRING, STRING);
        ourMap.putValue(QuteElementTypes.QUTE_STRING, STRING);

        // Numeric
        ourMap.putValue(QuteElementTypes.QUTE_NUMERIC, NUMERIC);

        // Boolean
        ourMap.putValue(QuteElementTypes.QUTE_BOOLEAN, BOOLEAN);

        // Keyword (ex : null)
        ourMap.putValue(QuteElementTypes.QUTE_KEYWORD, KEYWORD);

        // Qute section (ex: {#let name='foo'}
        ourMap.putValue(QuteTokenType.QUTE_START_TAG_OPEN, QUTE_EDGE);
        ourMap.putValue(QuteTokenType.QUTE_START_TAG_CLOSE, QUTE_EDGE);
        ourMap.putValue(QuteTokenType.QUTE_START_TAG_SELF_CLOSE, QUTE_EDGE);
        ourMap.putValue(QuteTokenType.QUTE_END_TAG_OPEN, QUTE_EDGE);
        ourMap.putValue(QuteTokenType.QUTE_END_TAG_CLOSE, QUTE_EDGE);
        ourMap.putValue(QuteTokenType.QUTE_END_TAG_SELF_CLOSE, QUTE_EDGE);
        ourMap.putValue(QuteTokenType.QUTE_START_TAG, SECTION_TAG_NAME);
        ourMap.putValue(QuteTokenType.QUTE_END_TAG, SECTION_TAG_NAME);

        // Qute parameter declaration (ex: {@java.lang.String foo}
        ourMap.putValue(QuteTokenType.QUTE_START_PARAMETER_DECLARATION, QUTE_EDGE);
        ourMap.putValue(QuteTokenType.QUTE_END_PARAMETER_DECLARATION, QUTE_EDGE);
    }

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new QuteLexer();
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        return ourMap.get(tokenType).toArray(TextAttributesKey.EMPTY_ARRAY);
    }
}
