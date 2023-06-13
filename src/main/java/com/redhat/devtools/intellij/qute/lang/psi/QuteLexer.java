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
package com.redhat.devtools.intellij.qute.lang.psi;

import com.intellij.lexer.LexerBase;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.tree.IElementType;
import com.redhat.qute.parser.template.scanner.ScannerState;
import com.redhat.qute.parser.template.scanner.TemplateScanner;
import com.redhat.qute.parser.template.scanner.TokenType;
import org.jetbrains.annotations.NotNull;

/**
 * Qute lexer based on the Qute LS scanner to parse Qute template.
 */
public class QuteLexer extends LexerBase {

    private IElementType myTokenType;
    private CharSequence myText;

    private int myTokenStart;
    private int myTokenEnd;

    private int myBufferEnd;
    private int myState;

    private boolean myFailed;

    private TemplateScanner scanner;

    private AbstractQuteSubLexer currentSubLexer;
    private int startExpressionOffset = -1;

    private int startTagOpenOffset;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        myText = buffer;
        myTokenStart = myTokenEnd = startOffset;
        myBufferEnd = endOffset;
        startTagOpenOffset = -1;
        startExpressionOffset = -1;
        currentSubLexer = null;
        scanner = (TemplateScanner) TemplateScanner.createScanner(buffer.subSequence(0, endOffset).toString(), startOffset);
        myTokenType = null;
    }

    @Override
    public int getState() {
        locateToken();
        return myState;
    }

    @Override
    public IElementType getTokenType() {
        locateToken();
        return myTokenType;
    }

    @Override
    public int getTokenStart() {
        locateToken();
        return myTokenStart;
    }

    @Override
    public int getTokenEnd() {
        locateToken();
        return myTokenEnd;
    }

    @Override
    public void advance() {
        locateToken();
        myTokenType = null;
    }

    @NotNull
    @Override
    public CharSequence getBufferSequence() {
        return myText;
    }

    @Override
    public int getBufferEnd() {
        return myBufferEnd;
    }

    protected void locateToken() {
        if (myTokenType != null) return;

        myTokenStart = myTokenEnd;
        if (myFailed) return;

        try {
            if (startExpressionOffset != -1 && currentSubLexer == null) {
                // create a sub lexer to parse content of Qute expression (ex: {|foo.bar(0)|})
                currentSubLexer = new QuteLexerForExpression(myText.toString(), scanner, startExpressionOffset);
            } else if (startTagOpenOffset != -1 && currentSubLexer == null) {
                // create a sub lexer to parse content of Qute start section (ex: {#let |name='foo'|}{/let})
                currentSubLexer = new QuteLexerForStartTag(myText.toString(), scanner, startTagOpenOffset);
            }
            boolean continueToScanTemplate = currentSubLexer == null;
            if (currentSubLexer != null) {
                // parse content of expression or content of Qute start section
                myTokenType = currentSubLexer.getTokenType();
                if (myTokenType == null) {
                    // The parse of the content is fisnished, we can continue to parse another tokens of the template.
                    continueToScanTemplate = true;
                    startExpressionOffset = -1;
                    startTagOpenOffset = -1;
                    currentSubLexer = null;
                } else {
                    // collect token from the sub lexer
                    myState = currentSubLexer.getState();
                    myTokenEnd = currentSubLexer.getTokenEnd();
                    currentSubLexer.advance();
                }
            }

            if (continueToScanTemplate) {
                // Parse tokens from the template
                TokenType tokenType = scanner.scan();
                while (tokenType != TokenType.EOS) {
                    IElementType elementType = getTokenType(tokenType);
                    if (elementType != null) {
                        myState = getStateAsInt(scanner.getScannerState());
                        myTokenType = elementType;
                        myTokenEnd = scanner.getTokenEnd();
                        if (myTokenType == QuteTokenType.QUTE_START_EXPRESSION) {
                            // It is a start expression (ex : |{|foo},
                            // We will return this token and for the next iteration we will use a sub lexer
                            // to parse expression content
                            startExpressionOffset = scanner.getTokenEnd();
                        } else if (myTokenType == QuteTokenType.QUTE_START_TAG) {
                            // It is a start tag (ex : |{#|let ...},
                            // We will return this token and for the next iteration we will use a sub lexer
                            // to parse start tag content
                            startTagOpenOffset = scanner.getTokenEnd();
                        }
                        break;
                    }
                    tokenType = scanner.scan();
                }
            }
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Throwable e) {
            myFailed = true;
            myTokenType = com.intellij.psi.TokenType.BAD_CHARACTER;
            myTokenEnd = myBufferEnd;
        }
    }

    /**
     * Returns the IJ {@link IElementType} from the given Qute parser token type.
     *
     * @param tokenType the Qute parser token type.
     *
     * @return the IJ {@link IElementType} from the given Qute parser token type.
     */
    static IElementType getTokenType(TokenType tokenType) {
        switch (tokenType) {
            case StartComment:
                return QuteTokenType.QUTE_COMMENT_START;
            case Comment:
                return QuteElementTypes.QUTE_COMMENT;
            case EndComment:
                return QuteTokenType.QUTE_COMMENT_END;
            case StartParameterDeclaration:
                return QuteTokenType.QUTE_START_PARAMETER_DECLARATION;
            case ParameterDeclaration:
                return QuteTokenType.QUTE_PARAMETER_DECLARATION;
            case EndParameterDeclaration:
                return QuteTokenType.QUTE_END_PARAMETER_DECLARATION;
            case StartExpression:
                return QuteTokenType.QUTE_START_EXPRESSION;
            case EndExpression:
                return QuteTokenType.QUTE_END_EXPRESSION;
            case StartTagOpen:
                return QuteTokenType.QUTE_START_TAG_OPEN;
            case StartTagClose:
                return QuteTokenType.QUTE_START_TAG_CLOSE;
            case StartTagSelfClose:
                return QuteTokenType.QUTE_START_TAG_SELF_CLOSE;
            case StartTag:
                return QuteTokenType.QUTE_START_TAG;
            case EndTag:
                return QuteTokenType.QUTE_END_TAG;
            case EndTagSelfClose:
                return QuteTokenType.QUTE_END_TAG_SELF_CLOSE;
            case EndTagOpen:
                return QuteTokenType.QUTE_END_TAG_OPEN;
            case EndTagClose:
                return QuteTokenType.QUTE_END_TAG_CLOSE;
            case Whitespace:
                return QuteTokenType.QUTE_WHITESPACE;
        }
        return QuteElementTypes.QUTE_TEXT;
    }

    static int getStateAsInt(ScannerState state) {
        return state.ordinal();
    }

    @Override
    public String toString() {
        return "QuteLexer for " + scanner.getClass().getName();
    }

}
