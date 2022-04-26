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
package com.redhat.devtools.intellij.qute.lang;

import com.intellij.psi.templateLanguages.TemplateDataElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.OuterLanguageElementType;
import com.redhat.qute.parser.template.Node;

import java.util.HashMap;
import java.util.Map;

public class QuteElementTypes {
    public static final IFileElementType QUTE_FILE = new IFileElementType("QUTE_FILE", QuteLanguage.INSTANCE);

    /*
     Non Qute content in Qute PSI file
     */
    public static final IElementType QUTE_CONTENT = new QuteElementType("#text");

    /*
     Qute comment
     */
    public static final IElementType QUTE_COMMENT = new QuteElementType("#comment");

    /*
     Qute block in non Qute PSI file
     */
    public static final IElementType QUTE_BLOCK = new OuterLanguageElementType("QUTE-BLOCK", QuteLanguage.INSTANCE);

    /*
     This is the pseudo type that will be assigned to the Qute PSI file so that Qute blocks in non Qute PSI file is assigned
     the OuterLanguageElement so that it is not used by completion,...
     */
    public static final IElementType QUTE_FILE_DATA = new TemplateDataElementType("QUTE_FILE_DATA", QuteLanguage.INSTANCE, QUTE_CONTENT, QUTE_BLOCK);
    private static final Map<String, IElementType> types = new HashMap<>();

    static {
        types.put("#text", QUTE_CONTENT);
        types.put("#comment", QUTE_COMMENT);
    }

    public static IElementType fromNode(Node node) {
        return types.computeIfAbsent(node.getNodeName(), s -> new QuteElementType(s));
    }
}
