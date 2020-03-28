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
package com.redhat.devtools.intellij.quarkus.search;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public class PsiUtils {
    public static Range toRange(PsiElement element, int offset, int length) {
        Range range = newRange();
        if (offset > 0 || length > 0) {
            int[] loc = null;
            int[] endLoc = null;
            Document buffer = PsiDocumentManager.getInstance(element.getProject()).getDocument(element.getContainingFile());
            if (buffer != null) {
                loc = JsonRpcHelpers.toLine(buffer, offset);
                endLoc = JsonRpcHelpers.toLine(buffer, offset + length);
            }
            if (loc == null) {
                loc = new int[2];
            }
            if (endLoc == null) {
                endLoc = new int[2];
            }
            setPosition(range.getStart(), loc);
            setPosition(range.getEnd(), endLoc);
        }
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

}
