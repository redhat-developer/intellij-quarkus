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

/**
 * Qute token type.
 */
public interface QuteTokenType {

    IElementType QUTE_COMMENT_START = new QuteElementType("QUTE_COMMENT_START");

    IElementType QUTE_COMMENT_END = new QuteElementType("QUTE_COMMENT_END");

    IElementType QUTE_START_PARAMETER_DECLARATION = new QuteElementType("QUTE_START_PARAMETER_DECLARATION");

    IElementType QUTE_PARAMETER_DECLARATION = new QuteElementType("QUTE_PARAMETER_DECLARATION");

    IElementType QUTE_END_PARAMETER_DECLARATION = new QuteElementType("QUTE_END_PARAMETER_DECLARATION");

    IElementType QUTE_START_EXPRESSION = new QuteElementType("QUTE_START_EXPRESSION");

    IElementType QUTE_END_EXPRESSION = new QuteElementType("QUTE_END_EXPRESSION");

    IElementType QUTE_START_TAG = new QuteElementType("QUTE_START_TAG");
    IElementType QUTE_END_TAG = new QuteElementType("QUTE_END_TAG");
    IElementType QUTE_END_TAG_SELF_CLOSE = new QuteElementType("QUTE_END_TAG_SELF_CLOSE");
    IElementType QUTE_END_TAG_OPEN = new QuteElementType("QUTE_END_TAG_OPEN");
    IElementType QUTE_END_TAG_CLOSE = new QuteElementType("QUTE_END_TAG_CLOSE");
    IElementType QUTE_START_TAG_OPEN = new QuteElementType("QUTE_START_TAG_OPEN");
    IElementType QUTE_START_TAG_CLOSE = new QuteElementType("QUTE_START_TAG_CLOSE");
    IElementType QUTE_START_TAG_SELF_CLOSE = new QuteElementType("QUTE_START_TAG_SELF_CLOSE");
    IElementType QUTE_WHITESPACE = new QuteElementType("QUTE_WHITESPACE");

    // Qute expression
    IElementType QUTE_EXPRESSION_NAMESPACE_PART =  new QuteElementType("QUTE_EXPRESSION_NAMESPACE_PART");
    IElementType QUTE_EXPRESSION_OBJECT_PART =  new QuteElementType("QUTE_EXPRESSION_OBJECT_PART");
    IElementType QUTE_EXPRESSION_PROPERTY_PART = new QuteElementType("QUTE_EXPRESSION_PROPERTY_PART");
    IElementType QUTE_EXPRESSION_METHOD_PART =  new QuteElementType("QUTE_EXPRESSION_METHOD_PART");
    IElementType QUTE_EXPRESSION_OPEN_BRACKET = new QuteElementType("QUTE_EXPRESSION_OPEN_BRACKET");
    IElementType QUTE_EXPRESSION_CLOSE_BRACKET = new QuteElementType("QUTE_EXPRESSION_CLOSE_BRACKET");
    IElementType QUTE_EXPRESSION_INFIX_METHOD_PART = new QuteElementType("QUTE_EXPRESSION_INFIX_METHOD_PART");
    IElementType QUTE_EXPRESSION_INFIX_PARAMETER = new QuteElementType("QUTE_EXPRESSION_INFIX_PARAMETER");
    IElementType QUTE_EXPRESSION_DOT = new QuteElementType("QUTE_EXPRESSION_DOT");
    IElementType QUTE_EXPRESSION_COLON_SPACE = new QuteElementType("QUTE_EXPRESSION_COLON_SPACE");
    IElementType QUTE_EXPRESSION_START_STRING = new QuteElementType("QUTE_EXPRESSION_START_STRING");
    IElementType QUTE_EXPRESSION_STRING = new QuteElementType("QUTE_EXPRESSION_STRING");
    IElementType QUTE_EXPRESSION_END_STRING = new QuteElementType("QUTE_EXPRESSION_END_STRING");
    IElementType QUTE_EXPRESSION_WHITESPACE = new QuteElementType("QUTE_EXPRESSION_WHITESPACE");

    // Parameters
    IElementType QUTE_PARAMETER_NAME =  new QuteElementType("QUTE_PARAMETER_NAME");
    IElementType QUTE_PARAMETER_ASSIGN =  new QuteElementType("QUTE_PARAMETER_ASSIGN");
    IElementType QUTE_PARAMETER_VALUE =  new QuteElementType("QUTE_PARAMETER_VALUE");
}
