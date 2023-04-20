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
package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Key;
import com.redhat.devtools.intellij.quarkus.lsp4ij.operations.codeactions.LSPCodeActionUpdater;

/**
 * LSP editor wrapper used to:
 *
 * <ul>
 *     <li>collect LSP code action only when an LSP diagnostic is hovered ot when caret cursor changed.</li>
 * </ul>
 */
public class LSPEditorWrapper {

    private static final Key<LSPEditorWrapper> KEY = Key.create(LSPEditorWrapper.class.getName());

    private final Editor editor;

    private final LSPCodeActionUpdater codeActionUpdater;

    private LSPEditorWrapper(Editor editor) {
        this.editor = editor;
        this.codeActionUpdater = new LSPCodeActionUpdater(editor);
    }


    /**
     * Returns the Intellij editor instance which is wrapped.
     *
     * @return the Intellij editor instance which is wrapped.
     */
    public Editor getEditor() {
        return editor;
    }

    /**
     * Returns true if the given Intellij editor is wrapped and false otherwise.
     * @param editor the Intellij editor instance.
     * @return true if the given Intellij editor is wrapped and false otherwise.
     */
    public static boolean hasWrapper(Editor editor) {
        return editor.getUserData(KEY) != null;
    }

    /**
     * Dispose the LSP editor wrapper.
     */
    public void dispose() {
        this.codeActionUpdater.dispose();
        editor.putUserData(KEY, null);
    }

    /**
     * Returns the LSP editor wrapper from the given Intellij editor.
     * @param editor the Intellij editor instance
     * @return the LSP editor wrapper from the given Intellij editor.
     */
    public static LSPEditorWrapper getLSPEditorWrapper(Editor editor) {
        LSPEditorWrapper wrapper = editor.getUserData(KEY);
        if (wrapper != null) {
            return wrapper;
        }
        return getLSPEditorWrapperSync(editor);
    }

    private static synchronized LSPEditorWrapper getLSPEditorWrapperSync(Editor editor) {
        LSPEditorWrapper wrapper = editor.getUserData(KEY);
        if (wrapper != null) {
            return wrapper;
        }
        wrapper = new LSPEditorWrapper(editor);
        editor.putUserData(KEY, wrapper);
        return wrapper;
    }

}