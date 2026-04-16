/*******************************************************************************
 * Copyright (c) 2026 Red Hat Inc. and others.
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
import com.redhat.qute.parser.scanner.Scanner;
import com.redhat.qute.parser.yaml.scanner.YamlScannerState;
import com.redhat.qute.parser.yaml.scanner.YamlTokenType;
import com.redhat.qute.parser.template.scanner.TemplateScanner;
import com.redhat.qute.parser.yaml.scanner.YamlScanner;

/**
 * Qute lexer to parse language intection content (ex: Roq Yaml front matter).
 *
 * <code>
 *     ---
 *     layout: foo
 *     ---
 *
 * </code>
 */
public class QuteLexerForLanguageInjection extends AbstractQuteSubLexer {

    private final Scanner<YamlTokenType, YamlScannerState> scanner;
    private final int endLanguageInjectionEnd;
    private int myLastState;
    private IElementType myLastTokenType;
    private int myLastTokenEnd;
    private AbstractQuteSubLexer currentSubLexer;

    public QuteLexerForLanguageInjection(String text, TemplateScanner templateScanner, int startLanguageInjectionOffset) {
        myTokenEnd = startLanguageInjectionOffset;
        boolean isClosed = false;
        // Get the token end of the language injection --> ---foo: bar|---|
        com.redhat.qute.parser.template.scanner.TokenType tokenType = templateScanner.scan();
        while (tokenType != com.redhat.qute.parser.template.scanner.TokenType.EOS) {
            if (tokenType == com.redhat.qute.parser.template.scanner.TokenType.LanguageInjectionEnd) {
                isClosed = true;
                break;
            }
            tokenType = templateScanner.scan();
        }
        if (isClosed) {
            // The language injection is closed, stores state, token type and end position of the end ---
            myLastState = QuteLexer.getStateAsInt(templateScanner.getScannerState());
            myLastTokenType = QuteLexer.getTokenType(templateScanner.getTokenType());
            myLastTokenEnd = templateScanner.getTokenEnd();
        }
        // Initialize the parameter scanner to parse the start section content
        endLanguageInjectionEnd = templateScanner.getTokenOffset();
        scanner = YamlScanner.createScanner(text, startLanguageInjectionOffset, endLanguageInjectionEnd);
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
                YamlTokenType tokenType = scanner.scan();
                while (tokenType != YamlTokenType.EOS) {
                    IElementType elementType = getTokenType(tokenType);
                    if (elementType != null) {
                        myState = getStateAsInt(scanner.getScannerState());
                        myTokenType = elementType;
                        myTokenEnd = scanner.getTokenEnd();
                        break;
                    }
                    tokenType = scanner.scan();
                }
                if (tokenType == YamlTokenType.EOS) {
                    if (myLastTokenType != null) {
                        myState = myLastState;
                        myTokenType = myLastTokenType;
                        myTokenEnd = myLastTokenEnd;
                        myLastTokenType = null;
                    } else {
                        // No closing tag - set myTokenEnd to the end of the scanned region
                        myTokenType = null;
                        myTokenEnd = endLanguageInjectionEnd;
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
    public static IElementType getTokenType(YamlTokenType tokenType) {
        return switch (tokenType) {
            case Key -> QuteTokenType.QUTE_YAML_KEY;
            case Colon -> QuteTokenType.QUTE_YAML_COLON;
            case Value -> QuteTokenType.QUTE_YAML_VALUE;
            case StartString, String, EndString, ScalarString -> QuteTokenType.QUTE_YAML_STRING;
            case ScalarNumber -> QuteTokenType.QUTE_YAML_NUMBER;
            case ScalarBoolean -> QuteTokenType.QUTE_YAML_BOOLEAN;
            case ScalarNull -> QuteTokenType.QUTE_YAML_NULL;
            default -> QuteTokenType.QUTE_YAML_WHITESPACE;
        };
    }

    public static int getStateAsInt(YamlScannerState state) {
        return 20 + state.ordinal();
    }
}
