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

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.tree.IElementType;
import com.redhat.qute.parser.expression.scanner.ExpressionScanner;
import com.redhat.qute.parser.parameter.scanner.ParameterScanner;
import com.redhat.qute.parser.parameter.scanner.ScannerState;
import com.redhat.qute.parser.template.scanner.TemplateScanner;
import com.redhat.qute.parser.parameter.scanner.TokenType;

/**
 * Qute lexer to parse Qute start tag content.
 *
 * <code>
 *      {#let |name='foo' bar=true|}
 * </code>
 */
public class QuteLexerForStartTag extends AbstractQuteSubLexer {

    private final String text;

    private final ParameterScanner scanner;
    private int myLastState;
    private IElementType myLastTokenType;
    private int myLastTokenEnd;
    private AbstractQuteSubLexer currentSubLexer;

    public QuteLexerForStartTag(String text, TemplateScanner templateScanner, int startTagOpenOffset) {
        this.text = text;
        boolean isClosed = false;
        // Get the token end of the start section --> {#let name='foo' bar=true|}|
        com.redhat.qute.parser.template.scanner.TokenType tokenType = templateScanner.scan();
        while (tokenType != com.redhat.qute.parser.template.scanner.TokenType.EOS) {
            if (tokenType == com.redhat.qute.parser.template.scanner.TokenType.StartTagClose ||
                    tokenType == com.redhat.qute.parser.template.scanner.TokenType.StartTagSelfClose) {
                isClosed = true;
                break;
            }
            tokenType = templateScanner.scan();
        }
        if (isClosed) {
            // The start section is closed, stores state, token type and end position of the end '}' or '/}'
            myLastState = QuteLexer.getStateAsInt(templateScanner.getScannerState());
            myLastTokenType = QuteLexer.getTokenType(templateScanner.getTokenType());
            myLastTokenEnd = templateScanner.getTokenEnd();
        }
        // Initialize the parameter scanner to parse the start section content
        int endTagOpenOffset = templateScanner.getTokenOffset();
        scanner = ParameterScanner.createScanner(text, startTagOpenOffset, endTagOpenOffset, false, true);
    }

    @Override
    protected void doLocateToken() {
        if (myTokenType != null) return;

        myTokenStart = myTokenEnd;
        if (myFailed) return;

        try {
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
                    IElementType elementType = getTokenType(tokenType);
                    if (elementType != null) {
                        myState = getStateAsInt(scanner.getScannerState());
                        myTokenType = elementType;
                        myTokenEnd = scanner.getTokenEnd();
                        if (myTokenType == QuteTokenType.QUTE_PARAMETER_NAME || myTokenType == QuteTokenType.QUTE_PARAMETER_VALUE) {
                            // Parse parameter name as expression
                            int startExpressionOffset = scanner.getTokenOffset();
                            int endExpressionOffset = scanner.getTokenEnd();
                            myTokenType = null;
                            myTokenEnd = startExpressionOffset;
                            currentSubLexer = new QuteLexerForExpressionParameter(text.toString(), startExpressionOffset, endExpressionOffset);
                            locateToken();
                            return;
                        }
                        break;
                    }
                    tokenType = scanner.scan();
                }
                if (tokenType == com.redhat.qute.parser.parameter.scanner.TokenType.EOS) {
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
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Throwable e) {
            myFailed = true;
            myTokenType = com.intellij.psi.TokenType.BAD_CHARACTER;
        }
    }

    /**
     * Returns the IJ {@link IElementType} from the given Qute parser token type.
     *
     * @param tokenType the Qute parser token type.
     *
     * @return the IJ {@link IElementType} from the given Qute parser token type.
     */
    public static IElementType getTokenType(com.redhat.qute.parser.parameter.scanner.TokenType tokenType) {
        switch (tokenType) {
            case ParameterName:
                return QuteTokenType.QUTE_PARAMETER_NAME;
            case Assign:
                return QuteTokenType.QUTE_PARAMETER_ASSIGN;
            case ParameterValue:
                return QuteTokenType.QUTE_PARAMETER_VALUE;
            case Whitespace:
                return QuteTokenType.QUTE_EXPRESSION_WHITESPACE;
        }
        return null;
    }

    public static int getStateAsInt(ScannerState state) {
        return 20 + state.ordinal();
    }
}
