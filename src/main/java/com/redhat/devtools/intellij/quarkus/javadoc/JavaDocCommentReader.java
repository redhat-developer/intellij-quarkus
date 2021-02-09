/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 * IBM Corporation - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.javadoc;

/**
 * Reads a java doc comment from a java doc comment. Skips star-character on begin of line.
 */
public class JavaDocCommentReader extends SingleCharReader {

    private String fSource;

    private int fCurrPos;

    private int fStartPos;

    private int fEndPos;

    private boolean fWasNewLine;

    public JavaDocCommentReader(String source, int start, int end) {
        fSource= source;
        fStartPos= start + 3;
        fEndPos= end - 2;
        reset();
    }

    public JavaDocCommentReader(String source) {
        this(source, 0, source.length());
    }

    /**
     * @see java.io.Reader#read()
     */
    @Override
    public int read() {
        if (fCurrPos < fEndPos) {
            char ch= getChar(fCurrPos++);
            if (fWasNewLine && !isLineDelimiterChar(ch)) {
                while (fCurrPos < fEndPos && Character.isWhitespace(ch)) {
                    ch= getChar(fCurrPos++);
                }
                if (ch == '*') {
                    if (fCurrPos < fEndPos) {
                        do {
                            ch= getChar(fCurrPos++);
                        } while (ch == '*');
                    } else {
                        return -1;
                    }
                }
            }
            fWasNewLine= isLineDelimiterChar(ch);

            return ch;
        }
        return -1;
    }

    private boolean isLineDelimiterChar(char ch) {
        return ch == '\n' || ch == '\r';
    }

    /**
     * @see java.io.Reader#close()
     */
    @Override
    public void close() {
        fSource = null;
    }

    /**
     * @see java.io.Reader#reset()
     */
    @Override
    public void reset() {
        fCurrPos= fStartPos;
        fWasNewLine= true;
        // skip first line delimiter:
        if (fCurrPos < fEndPos && '\r' == getChar(fCurrPos)) {
            fCurrPos++;
        }
        if (fCurrPos < fEndPos && '\n' == getChar(fCurrPos)) {
            fCurrPos++;
        }
    }

    private char getChar(int pos) {
        return fSource.charAt(pos);
    }

    /**
     * Returns the offset of the last read character in the passed buffer.
     *
     * @return the offset
     */
    public int getOffset() {
        return fCurrPos;
    }
}
