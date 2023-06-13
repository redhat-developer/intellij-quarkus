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

import com.intellij.psi.templateLanguages.TemplateDataElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.OuterLanguageElementType;
import com.redhat.devtools.intellij.qute.lang.QuteLanguage;
import com.redhat.devtools.intellij.qute.lang.psi.QuteElementType;

public class QuteElementTypes {

    public static final IFileElementType QUTE_FILE = new IFileElementType("QUTE_FILE", QuteLanguage.INSTANCE);

    /*
     Non Qute content in Qute PSI file
     */
    public static final IElementType QUTE_CONTENT = new QuteElementType("QUTE_CONTENT");

    /*
     Qute comment
     */
    public static final IElementType QUTE_COMMENT = new QuteElementType("QUTE_COMMENT");

    public static final IElementType QUTE_EXPRESSION = new QuteElementType("QUTE_EXPRESSION");

    public static final IElementType QUTE_START_SECTION = new QuteElementType("QUTE_START_SECTION");

    public static final IElementType QUTE_STRING = new QuteElementType("QUTE_STRING");

    public static final IElementType QUTE_NUMERIC = new QuteElementType("QUTE_NUMERIC");

    public static final IElementType QUTE_BOOLEAN = new QuteElementType("QUTE_BOOLEAN");

    public static final IElementType QUTE_KEYWORD = new QuteElementType("QUTE_KEYWORD");

    public static final IElementType QUTE_TEXT = new OuterLanguageElementType("QUTE_TEXT", QuteLanguage.INSTANCE);

    public static final IElementType QUTE_OUTER_ELEMENT_TYPE = new OuterLanguageElementType("QUTE_OUTER_ELEMENT_TYPE", QuteLanguage.INSTANCE);

    /*
     This is the pseudo type that will be assigned to the Qute PSI file so that Qute blocks in non Qute PSI file is assigned
     the OuterLanguageElement so that it is not used by completion,...
     */
    public static final IElementType QUTE_FILE_DATA = new TemplateDataElementType("QUTE_FILE_DATA", QuteLanguage.INSTANCE, QUTE_TEXT, QUTE_OUTER_ELEMENT_TYPE);
}
