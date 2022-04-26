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

import com.intellij.lang.ASTFactory;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.impl.source.tree.LazyParseableElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.ILazyParseableElementType;
import com.redhat.qute.parser.template.Template;
import com.redhat.qute.parser.template.TemplateParser;
import org.jetbrains.annotations.NotNull;

/**
 * Qute parser. Reqyired because some features are related to special
 * PSI nodes thus we need to implement them.
 * Inspired by TemplateParser as there seem no way to reuse it.
 *
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/HEAD/qute.ls/com.redhat.qute.ls/src/main/java/com/redhat/qute/parser/template/TemplateParser.java">https://github.com/redhat-developer/quarkus-ls/blob/HEAD/qute.ls/com.redhat.qute.ls/src/main/java/com/redhat/qute/parser/template/TemplateParser.java</a>
 */
public class QuteParser implements PsiParser {
    @Override
    public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
        Template template = TemplateParser.parse(builder.getOriginalText().toString(), "");
        LazyParseableElement rootNode = ASTFactory.lazy((ILazyParseableElementType) root, null);
        template.getChildren().forEach(child -> rootNode.rawAddChildrenWithoutNotifications(QuteASTNode.getNode(child)));
        return rootNode;
    }
}
