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

import com.redhat.qute.parser.expression.scanner.ExpressionScanner;
import com.redhat.qute.parser.template.scanner.TemplateScanner;
import com.redhat.qute.parser.template.scanner.TokenType;

/**
 * Qute lexer to parse Qute expression content.
 *
 * <code>
 *      {|foo.bar(0)|}
 * </code>
 */
public class QuteLexerForExpression extends QuteLexerForExpressionContent {

    QuteLexerForExpression(String text, TemplateScanner templateScanner, int startExpressionOffset) {
        super(text);
        // Get the token end of the expression --> {foo.bar(0)|}|
        boolean isClosed = false;
        TokenType tokenType = templateScanner.scan();
        while (tokenType != TokenType.EOS) {
            if (tokenType == TokenType.EndExpression) {
                isClosed = true;
                break;
            }
            tokenType = templateScanner.scan();
        }
        if (isClosed) {
            // The expression is closed, stores state, token type and end position of the end expression '}'
            myLastState = QuteLexer.getStateAsInt(templateScanner.getScannerState());
            myLastTokenType = QuteLexer.getTokenType(templateScanner.getTokenType());
            myLastTokenEnd = templateScanner.getTokenEnd();
        }
        // Initialize the expression scanner to parse the expression content
        int endExpressionOffset = templateScanner.getTokenOffset();
        initialize(ExpressionScanner.createScanner(text, true, startExpressionOffset, endExpressionOffset));
    }

}
