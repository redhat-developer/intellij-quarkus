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
package com.redhat.devtools.intellij.qute.editor;

import com.intellij.codeInsight.highlighting.PairedBraceMatcherAdapter;
import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.redhat.devtools.intellij.qute.lang.QuteLanguage;
import com.redhat.devtools.intellij.qute.lang.psi.QuteTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Qute brace matcher for Qute expression and section tag.
 */
public class QuteBraceMatcher extends PairedBraceMatcherAdapter {

    private static final BracePair[] BRACE_PAIRS = new BracePair[]{
            new BracePair(QuteTokenType.QUTE_START_EXPRESSION, QuteTokenType.QUTE_END_EXPRESSION, true),
            new BracePair(QuteTokenType.QUTE_START_TAG_OPEN, QuteTokenType.QUTE_START_TAG_CLOSE, true),
            new BracePair(QuteTokenType.QUTE_START_TAG_OPEN, QuteTokenType.QUTE_START_TAG_SELF_CLOSE, true),
            new BracePair(QuteTokenType.QUTE_END_TAG_OPEN, QuteTokenType.QUTE_END_TAG_CLOSE, true),
            new BracePair(QuteTokenType.QUTE_END_TAG_OPEN, QuteTokenType.QUTE_END_TAG_SELF_CLOSE, true)};

    public QuteBraceMatcher() {
        super(new QutePairedBraceMatcher(), QuteLanguage.INSTANCE);
    }

    private static class QutePairedBraceMatcher implements PairedBraceMatcher {
        @Override
        public BracePair @NotNull [] getPairs() {
            return BRACE_PAIRS;
        }

        @Override
        public boolean isPairedBracesAllowedBeforeType(@NotNull final IElementType lbraceType, @Nullable final IElementType type) {
            return true;
        }

        @Override
        public int getCodeConstructStart(final PsiFile file, int openingBraceOffset) {
            return openingBraceOffset;
        }
    }
}