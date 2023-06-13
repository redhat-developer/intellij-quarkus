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
package com.redhat.devtools.intellij.qute.lang.psi;

import com.intellij.psi.tree.IElementType;

/**
 * Abstract class for sub lexer.
 */
public abstract class AbstractQuteSubLexer {

    protected IElementType myTokenType;
    protected int myTokenStart;
    protected int myTokenEnd;
    protected int myState;
    protected boolean myFailed;

    public int getState() {
        locateToken();
        return myState;
    }

    public IElementType getTokenType() {
        locateToken();
        return myTokenType;
    }

    public int getTokenEnd() {
        locateToken();
        return myTokenEnd;
    }

    public void advance() {
        locateToken();
        myTokenType = null;
    }

    public void locateToken() {
        if (myTokenType != null) return;

        myTokenStart = myTokenEnd;
        if (myFailed) return;

        doLocateToken();
    }

    protected abstract void doLocateToken();
}
