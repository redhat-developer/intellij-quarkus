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

import com.intellij.psi.tree.IElementType;
import com.redhat.qute.parser.expression.scanner.ExpressionScanner;
import com.redhat.qute.parser.parameter.scanner.TokenType;
import com.redhat.qute.parser.parameter.scanner.ParameterScanner;

/**
 * Qute lexer to parse Qute method parameters content.
 *
 * <code>
 *      {foo.bar(|'foo',true|)}
 * </code>
 */
public class QuteLexerForMethodParameters extends AbstractQuteSubLexer {

    private final ParameterScanner scanner;
    private final String text;
    protected int myLastState;
    protected IElementType myLastTokenType;
    protected int myLastTokenEnd;
    private AbstractQuteSubLexer currentSubLexer;

    public QuteLexerForMethodParameters(String text, ExpressionScanner expressionScanner, int startExpressionOffset) {
        this.text = text;
        boolean isClosed = false;
        // Get the token end of the method parameter --> {foo.bar('foo',true|)|}
        com.redhat.qute.parser.expression.scanner.TokenType tokenType = expressionScanner.scan();
        while (tokenType != com.redhat.qute.parser.expression.scanner.TokenType.EOS) {
            if (tokenType == com.redhat.qute.parser.expression.scanner.TokenType.CloseBracket) {
                isClosed = true;
                break;
            }
            tokenType = expressionScanner.scan();
        }
        if (isClosed) {
            // The method parameters is closed, stores state, token type and end position of the end ')'
            myLastState = QuteLexerForExpressionContent.getStateAsInt(expressionScanner.getScannerState());
            myLastTokenType = QuteLexerForExpressionContent.getTokenType(expressionScanner.getTokenType());
            myLastTokenEnd = expressionScanner.getTokenEnd();
        }
        // Initialize the parameter scanner to parse the method parameters content
        int endExpressionOffset = expressionScanner.getTokenOffset();
        scanner = ParameterScanner.createScanner(text, startExpressionOffset, endExpressionOffset, true, false);
    }

    @Override
    protected void doLocateToken() {
        boolean continueToScanTemplate = currentSubLexer == null;
        if (currentSubLexer != null) {
            myTokenType = currentSubLexer.getTokenType();
            if (myTokenType == null) {
                continueToScanTemplate = true;
                currentSubLexer = null;
            } else {
                myState = currentSubLexer.getState();
                myTokenEnd = currentSubLexer.getTokenEnd();
                currentSubLexer.advance();
            }
        }

        if (continueToScanTemplate) {
            TokenType tokenType = scanner.scan();
            while (tokenType != TokenType.EOS) {
                IElementType elementType = QuteLexerForStartTag.getTokenType(tokenType);
                if (elementType != null) {
                    myState = QuteLexerForStartTag.getStateAsInt(scanner.getScannerState());
                    myTokenType = elementType;
                    myTokenEnd = scanner.getTokenEnd();
                    if (myTokenType == QuteTokenType.QUTE_PARAMETER_NAME) {
                        // Parse parameter name as expression
                        int startExpressionOffset = scanner.getTokenOffset();
                        int endExpressionOffset = scanner.getTokenEnd();
                        myTokenType = null;
                        myTokenEnd = startExpressionOffset;
                        currentSubLexer = new QuteLexerForExpressionMethodParameter(text.toString(), startExpressionOffset, endExpressionOffset);
                        locateToken();
                        return;
                    }
                    break;
                }
                tokenType = scanner.scan();
            }
            if (tokenType == TokenType.EOS) {
                if (myLastTokenType != null) {
                    myState = myLastState;
                    myTokenType = myLastTokenType;
                    myTokenEnd = myLastTokenEnd;
                    myLastTokenType = null;
                } else {
                    myTokenType = null;
                }
            }
        }
    }
}
