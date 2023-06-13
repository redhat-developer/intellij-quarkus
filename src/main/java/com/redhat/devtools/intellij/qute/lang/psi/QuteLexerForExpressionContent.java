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
import com.redhat.qute.parser.expression.scanner.ScannerState;
import com.redhat.qute.parser.expression.scanner.TokenType;
import com.redhat.qute.parser.template.LiteralSupport;

/**
 * Base class to parse expression content.
 */
public abstract class QuteLexerForExpressionContent extends AbstractQuteSubLexer {

    private static final String NULL_TYPE = "null";
    private static final String STRING_TYPE = "java.lang.String";
    private static final String BOOLEAN_TYPE = "java.lang.Boolean";
    private static final String DOUBLE_TYPE = "java.lang.Double";
    private static final String FLOAT_TYPE = "java.lang.Float";
    private static final String INTEGER_TYPE = "java.lang.Integer";
    private static final String LONG_TYPE = "java.lang.Long";

    private final String text;
    private ExpressionScanner scanner;
    protected int myLastState;
    protected IElementType myLastTokenType;
    protected int myLastTokenEnd;
    private AbstractQuteSubLexer currentSubLexer;
    private int startOpenBracketOffset;

    QuteLexerForExpressionContent(String text) {
        this.text = text;
    }

    public void initialize(ExpressionScanner scanner) {
        this.scanner = scanner;
        this.startOpenBracketOffset = -1;
    }

    @Override
    protected void doLocateToken() {
        if (startOpenBracketOffset != -1 && currentSubLexer == null) {
            currentSubLexer = new QuteLexerForMethodParameters(text, scanner, startOpenBracketOffset);
        }
        boolean continueToScanTemplate = currentSubLexer == null;
        if (currentSubLexer != null) {
            myTokenType = currentSubLexer.getTokenType();
            if (myTokenType == null) {
                continueToScanTemplate = true;
                startOpenBracketOffset = -1;
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
                    if (myTokenType == QuteTokenType.QUTE_EXPRESSION_OPEN_BRACKET) {
                        // Current token is an open bracket of the method
                        // we stores this offset to parse in thenext iteration method parameters.
                        startOpenBracketOffset = scanner.getTokenEnd();
                    } else if (myTokenType == QuteTokenType.QUTE_EXPRESSION_OBJECT_PART || myTokenType == QuteTokenType.QUTE_EXPRESSION_INFIX_PARAMETER){
                        String text = scanner.getTokenText();
                        String javaType = LiteralSupport.getLiteralJavaType(text);
                        if (javaType != null && !javaType.isEmpty()) {
                            // It is an object part or an infix parameter
                            // Instead of returning this token, we try to return the best type (String,Number, Boolean, Token)
                           switch(javaType) {
                               case STRING_TYPE:
                                   myTokenType = QuteElementTypes.QUTE_STRING;
                                   break;
                               case INTEGER_TYPE:
                               case LONG_TYPE:
                               case DOUBLE_TYPE:
                               case FLOAT_TYPE:
                                   myTokenType = QuteElementTypes.QUTE_NUMERIC;
                                   break;
                               case BOOLEAN_TYPE:
                                   myTokenType = QuteElementTypes.QUTE_BOOLEAN;
                                   break;
                               case NULL_TYPE:
                                   myTokenType = QuteElementTypes.QUTE_KEYWORD;
                                   break;
                            }
                        }
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

    /**
     * Returns the IJ {@link IElementType} from the given Qute parser token type.
     *
     * @param tokenType the Qute parser token type.
     *
     * @return the IJ {@link IElementType} from the given Qute parser token type.
     */
    public static IElementType getTokenType(TokenType tokenType) {
        switch (tokenType) {
            case NamespacePart:
                return QuteTokenType.QUTE_EXPRESSION_NAMESPACE_PART;
            case ObjectPart:
                return QuteTokenType.QUTE_EXPRESSION_OBJECT_PART;
            case PropertyPart:
                return QuteTokenType.QUTE_EXPRESSION_PROPERTY_PART;
            case MethodPart:
                return QuteTokenType.QUTE_EXPRESSION_METHOD_PART;
            case OpenBracket:
                return QuteTokenType.QUTE_EXPRESSION_OPEN_BRACKET;
            case CloseBracket:
                return QuteTokenType.QUTE_EXPRESSION_CLOSE_BRACKET;
            case InfixMethodPart:
                return QuteTokenType.QUTE_EXPRESSION_INFIX_METHOD_PART;
            case InfixParameter:
                return QuteTokenType.QUTE_EXPRESSION_INFIX_PARAMETER;
            case Dot:
                return QuteTokenType.QUTE_EXPRESSION_DOT;
            case ColonSpace:
                return QuteTokenType.QUTE_EXPRESSION_COLON_SPACE;
            case StartString:
                return QuteTokenType.QUTE_EXPRESSION_START_STRING;
            case String:
                return QuteTokenType.QUTE_EXPRESSION_STRING;
            case EndString:
                return QuteTokenType.QUTE_EXPRESSION_END_STRING;
            case Whitespace:
                return QuteTokenType.QUTE_EXPRESSION_WHITESPACE;
        }
        return null;
    }

    public static int getStateAsInt(ScannerState state) {
        return 20 + state.ordinal();
    }

}
