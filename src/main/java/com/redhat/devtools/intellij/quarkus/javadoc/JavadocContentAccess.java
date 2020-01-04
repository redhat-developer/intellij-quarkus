/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.javadoc;

import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.javadoc.PsiDocComment;

import java.io.IOException;
import java.io.Reader;

public class JavadocContentAccess {
    private static Reader getHTMLContentReader(PsiMember member, boolean allowInherited, boolean useAttachedJavadoc) {
        PsiDocComment doc = ((PsiDocCommentOwner) member).getDocComment();
        PsiElement sourceMember = member.getNavigationElement();
        if (sourceMember instanceof PsiDocCommentOwner) {
            doc = ((PsiDocCommentOwner) sourceMember).getDocComment();
        }
        return doc == null ? null : new JavaDocCommentReader(doc.getText());
    }

    /**
     * Gets a reader for an IMember's Javadoc comment content from the source
     * attachment. and renders the tags in Markdown. Returns <code>null</code>
     * if the member does not contain a Javadoc comment or if no source is
     * available.
     *
     * @param member
     *            the member to get the Javadoc of.
     * @return a reader for the Javadoc comment content in Markdown or
     *         <code>null</code> if the member does not contain a Javadoc
     *         comment or if no source is available
     */
    public static Reader getMarkdownContentReader(PsiMember member) {
        Reader contentReader = getHTMLContentReader(member, true, true);
        if (contentReader != null) {
            try {
                return new JavaDoc2MarkdownConverter(contentReader).getAsReader();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    /**
     * Gets a reader for an IMember's Javadoc comment content from the source
     * attachment. and renders the tags in plain text. Returns <code>null</code> if
     * the member does not contain a Javadoc comment or if no source is available.
     *
     * @param member
     *            the member to get the Javadoc of.
     * @return a reader for the Javadoc comment content in plain text or
     *         <code>null</code> if the member does not contain a Javadoc comment or
     *         if no source is available
     */
    public static Reader getPlainTextContentReader(PsiMember member) {
        Reader contentReader = getHTMLContentReader(member, true, true);
        if (contentReader != null) {
            try {
                return new JavaDoc2PlainTextConverter(contentReader).getAsReader();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
