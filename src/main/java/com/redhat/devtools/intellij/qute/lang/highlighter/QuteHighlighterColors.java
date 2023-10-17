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

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.XmlHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;

/**
 * Qute higlighter colors constants.
 */
public class QuteHighlighterColors {

    public static final TextAttributesKey COMMENT = TextAttributesKey.createTextAttributesKey("QUTE_TOOLS.COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);

    public static final TextAttributesKey QUTE_EDGE = TextAttributesKey.createTextAttributesKey("QUTE_TOOLS.EXPRESSION", XmlHighlighterColors.XML_TAG);

    public static final TextAttributesKey SECTION_TAG_NAME = TextAttributesKey.createTextAttributesKey("QUTE_TOOLS.SECTION_TAG_NAME", XmlHighlighterColors.XML_TAG_NAME);

    public static final TextAttributesKey STRING = TextAttributesKey.createTextAttributesKey("QUTE_TOOLS.STRING", XmlHighlighterColors.XML_ATTRIBUTE_VALUE);

    public static final TextAttributesKey NUMERIC = TextAttributesKey.createTextAttributesKey("QUTE_TOOLS.NUMERIC", DefaultLanguageHighlighterColors.NUMBER);

    public static final TextAttributesKey BOOLEAN = TextAttributesKey.createTextAttributesKey("QUTE_TOOLS.BOOLEAN", DefaultLanguageHighlighterColors.KEYWORD);

    public static final TextAttributesKey KEYWORD = TextAttributesKey.createTextAttributesKey("QUTE_TOOLS.KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);

}
