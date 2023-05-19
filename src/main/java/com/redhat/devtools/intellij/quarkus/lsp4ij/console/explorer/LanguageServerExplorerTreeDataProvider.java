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
package com.redhat.devtools.intellij.quarkus.lsp4ij.console.explorer;

import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreePath;

/**
 * Language server explorer tree data provider.
 */
public class LanguageServerExplorerTreeDataProvider implements DataProvider {

    public static final String LANGUAGE_SERVER_DATA_ID = "languageServer";

    private final Tree tree;

    public LanguageServerExplorerTreeDataProvider(Tree tree) {
        this.tree = tree;
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        if (LANGUAGE_SERVER_DATA_ID.equals(dataId)) {
            TreePath path = tree.getSelectionPath();
            Object node = path != null ? path.getLastPathComponent() : null;
            if (node instanceof LanguageServerProcessTreeNode) {
                return ((LanguageServerProcessTreeNode) node).getLanguageServer();
            }
        }
        return null;
    }
}
