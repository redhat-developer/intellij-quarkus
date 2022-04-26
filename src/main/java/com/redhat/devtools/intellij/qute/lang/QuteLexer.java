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

import com.intellij.lexer.Lexer;
import com.intellij.lexer.LexerPosition;
import com.intellij.psi.tree.IElementType;
import com.redhat.qute.parser.template.Node;
import com.redhat.qute.parser.template.Template;
import com.redhat.qute.parser.template.TemplateParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuteLexer extends Lexer {
    Template template;
    private CharSequence buffer;
    private int endOffset;

    private int index;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.endOffset = endOffset;
        template = TemplateParser.parse(buffer.subSequence(startOffset, endOffset).toString(), "");
        this.index = initialState;
    }

    @Override
    public int getState() {
        return index;
    }

    private Node getToken() {
        return index< template.getChildCount()?template.getChild(index):null;
    }
    @Override
    public @Nullable IElementType getTokenType() {
        Node node = getToken();
        return node!=null?QuteElementTypes.fromNode(node):null;
    }

    @Override
    public int getTokenStart() {
        Node node = getToken();
        return node!=null?node.getStart():-1;
    }

    @Override
    public int getTokenEnd() {
        Node node = getToken();
        return node!=null?node.getEnd():-1;

    }

    @Override
    public void advance() {
        ++index;
    }

    @Override
    public @NotNull LexerPosition getCurrentPosition() {
        return new LexerPosition() {
            @Override
            public int getOffset() {
                return getTokenStart();
            }

            @Override
            public int getState() {
                return getState();
            }
        };
    }

    @Override
    public void restore(@NotNull LexerPosition position) {
        index = position.getState();
    }

    @Override
    public @NotNull CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return endOffset;
    }
}
