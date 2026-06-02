/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnnotationOwner;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNameValuePair;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.JsonRpcHelpers;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;

public class PsiUtils {
    private static Set<String> SILENCED_CODEGENS = Collections.singleton("lombok");

    /**
     * Executes a read action in a cancellable way to avoid UI freezes.
     * <p>
     * This method uses {@link ReadAction#nonBlocking(Callable)} which creates a cancellable
     * read action, preventing UI freezes when the operation takes too long or when the user
     * performs actions that require write access.
     * </p>
     * <p>
     * See <a href="https://blog.jetbrains.com/platform/2026/03/ui-freezes-and-the-dangers-of-non-cancellable-read-actions-in-background-threads/">
     * UI Freezes and the Dangers of Non-Cancellable Read Actions in Background Threads</a>
     * </p>
     *
     * @param action the read action to execute
     * @param <T>    the type of the result
     * @return the result of the read action
     */
    public static <T> T runCancellableReadAction(@NotNull Callable<T> action) {
        return ReadAction.nonBlocking(action).executeSynchronously();
    }

    public static Range toRange(PsiElement element, int offset, int length) {
        Document buffer = PsiDocumentManager.getInstance(element.getProject()).getCachedDocument(element.getContainingFile());
        return toRange(buffer, offset, length);
    }

    public static Range toRange(Document document, int offset, int length) {
        Range range = newRange();
        int[] loc = null;
        int[] endLoc = null;
        if (document != null && (offset > 0 || length > 0)) {
            loc = JsonRpcHelpers.toLine(document, offset);
            endLoc = JsonRpcHelpers.toLine(document, offset + length);
        }
        if (loc == null) {
            loc = new int[2];
        }
        if (endLoc == null) {
            endLoc = new int[2];
        }
        setPosition(range.getStart(), loc);
        setPosition(range.getEnd(), endLoc);
        return range;
    }

    /**
     * Creates a new {@link Range} with its start and end {@link Position}s set to line=0, character=0
     *
     * @return a new {@link Range};
     */
    public static Range newRange() {
        return new Range(new Position(), new Position());
    }

    private static void setPosition(Position position, int[] coords) {
        assert coords.length == 2;
        position.setLine(coords[0]);
        position.setCharacter(coords[1]);
    }

    public static boolean isHiddenGeneratedElement(PsiElement element) {
        if (element instanceof PsiModifierListOwner) {
            return isHiddenGeneratedElement(((PsiModifierListOwner) element).getAnnotations());
        } else if (element instanceof PsiAnnotationOwner) {
            return isHiddenGeneratedElement(((PsiAnnotationOwner) element).getAnnotations());
        }
        return false;
    }

    private static boolean isHiddenGeneratedElement(PsiAnnotation[] annotations) {
        for(PsiAnnotation annotation : annotations) {
            if (isSilencedGeneratedAnnotation(annotation)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSilencedGeneratedAnnotation(PsiAnnotation annotation) {
        if ("javax.annotation.Generated".equals(annotation.getQualifiedName()) || "javax.annotation.processing.Generated".equals(annotation.getQualifiedName())) {
            PsiAnnotationParameterList memberValuePairs = annotation.getParameterList();
            for (PsiNameValuePair m : memberValuePairs.getAttributes()) {
                if ("value".equals(m.getAttributeName())
                        && m.getValue() instanceof PsiLiteral) {
                    return SILENCED_CODEGENS.contains(m.getLiteralValue());
                } else if (m.getValue() instanceof PsiArrayInitializerMemberValue) {
                    for (PsiAnnotationMemberValue val : ((PsiArrayInitializerMemberValue) m.getValue()).getInitializers()) {
                        if (val instanceof PsiLiteral && SILENCED_CODEGENS.contains(((PsiLiteral) val).getValue())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
