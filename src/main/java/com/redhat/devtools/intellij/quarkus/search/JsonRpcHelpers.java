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

public class JsonRpcHelpers {
    public static int[] toLine(Document buffer, int offset) {
        int line = buffer.getLineNumber(offset);
        int column = offset - buffer.getLineStartOffset(line);
        return new int[] { line, column };
    }

    public static int toOffset(Document document, int line, int character) {
        return document.getLineStartOffset(line) + character;
    }
}
