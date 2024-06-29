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
package com.redhat.devtools.intellij.qute.lang.psi;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

import static com.redhat.devtools.intellij.qute.lang.psi.QuteElementTypes.*;
import static com.redhat.devtools.intellij.qute.lang.psi.QuteTokenType.*;

/**
 * Qute parsing used to build a Qute AST from the IJ lexer element type.
 */
public class QuteParsing {

    private final PsiBuilder myBuilder;

    public QuteParsing(PsiBuilder builder) {
        myBuilder = builder;
    }

    public void parseTemplate() {
        final PsiBuilder.Marker template = mark();

        while (isCommentToken(token())) {
            parseComment();
        }

        while (!eof()) {
            parseContent();
        }

        template.done(QUTE_CONTENT);
    }

    private void parseContent() {
        final IElementType tt = token();
        if (tt == QUTE_START_EXPRESSION) {
            parseExpression();
        } else if (tt == QUTE_START_TAG_OPEN) {
            parseSection();
        } else if (isCommentToken(tt)) {
            parseComment();
        } else if (tt == QUTE_TEXT) {
            parseText();
        } else {
            advance();
        }
    }

    private void parseSection() {
        final PsiBuilder.Marker startSection = mark();
        advance();

        while (true) {
            final IElementType tt = token();
            if (tt == null) {
                break;
            } else if (tt == QUTE_EXPRESSION) {
                // Comment content : foo in {! foo !}
                advance();
                continue;
            } else if (tt == QUTE_END_EXPRESSION) {
                // End expression: }
                advance();
            } else if (tt == QUTE_STRING) {
                final PsiBuilder.Marker string = mark();
                advance();
                string.done(QUTE_STRING);
                continue;
            } else if (tt == QUTE_EXPRESSION_NAMESPACE_PART) {
                final PsiBuilder.Marker namespacePart = mark();
                advance();
                namespacePart.done(QUTE_EXPRESSION_NAMESPACE_PART);
                continue;
            } else if (tt == QUTE_EXPRESSION_OBJECT_PART) {
                final PsiBuilder.Marker objectPart = mark();
                advance();
                objectPart.done(QUTE_EXPRESSION_OBJECT_PART);
                continue;
            } else if (tt == QUTE_EXPRESSION_PROPERTY_PART) {
                final PsiBuilder.Marker propertyPart = mark();
                advance();
                propertyPart.done(QUTE_EXPRESSION_PROPERTY_PART);
                continue;
            } else if (tt == QUTE_END_TAG_CLOSE || tt == QUTE_END_TAG_SELF_CLOSE) {
                advance();
                break;
            } else {
                parseContent();
            }
            break;
        }
        startSection.done(QUTE_START_SECTION);
    }


    private void parseText() {
        final PsiBuilder.Marker text = mark();
        advance();
        text.done(QUTE_TEXT);
    }

    private void parseExpression() {
        final PsiBuilder.Marker expression = mark();
        advance();

        while (true) {
            final IElementType tt = token();
            if (tt == null) {
                break;
            } else if (tt == QUTE_EXPRESSION) {
                // Comment content : foo in {! foo !}
                advance();
                continue;
            } else if (tt == QUTE_END_EXPRESSION) {
                // End expression: }
                advance();
            } else if (tt == QUTE_STRING) {
                final PsiBuilder.Marker string = mark();
                advance();
                string.done(QUTE_STRING);
                continue;
            } else if (tt == QUTE_EXPRESSION_NAMESPACE_PART) {
                final PsiBuilder.Marker namespacePart = mark();
                advance();
                namespacePart.done(QUTE_EXPRESSION_NAMESPACE_PART);
                continue;
            } else if (tt == QUTE_EXPRESSION_OBJECT_PART) {
                final PsiBuilder.Marker objectPart = mark();
                advance();
                objectPart.done(QUTE_EXPRESSION_OBJECT_PART);
                continue;
            } else if (tt == QUTE_EXPRESSION_PROPERTY_PART) {
                final PsiBuilder.Marker propertyPart = mark();
                advance();
                propertyPart.done(QUTE_EXPRESSION_PROPERTY_PART);
                continue;
            } else {
                //final PsiBuilder.Marker error = mark();
                advance();
                // error.error("BAD comments!");
                continue;
            }
            break;
        }
        expression.done(QUTE_EXPRESSION);
    }


    private void parseComment() {
        final PsiBuilder.Marker comment = mark();
        advance();
        while (true) {
            final IElementType tt = token();
            if (tt == null) {
                // Case when comment is not closed.
                // ex : {! foo
                break;
            } else if (tt == QUTE_COMMENT) {
                // Comment content : foo in {! foo !}
                advance();
                continue;
            } else if (tt == QUTE_COMMENT_END) {
                // End comment: !}
                advance();
            } else {
                //final PsiBuilder.Marker error = mark();
                advance();
                // error.error("BAD comments!");
                continue;
            }
            break;
        }
        comment.done(QUTE_COMMENT);
    }

    protected boolean isCommentToken(final IElementType tt) {
        // Start comment: {!
        return tt == QUTE_COMMENT_START;
    }

    protected final PsiBuilder.Marker mark() {
        return myBuilder.mark();
    }

    @Nullable
    protected final IElementType token() {
        return myBuilder.getTokenType();
    }

    protected final boolean eof() {
        return myBuilder.eof();
    }

    protected final void advance() {
        myBuilder.advanceLexer();
    }
}
