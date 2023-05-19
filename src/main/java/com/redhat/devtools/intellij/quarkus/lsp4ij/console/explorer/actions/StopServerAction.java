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
package com.redhat.devtools.intellij.quarkus.lsp4ij.console.explorer.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageServerBundle;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageServerWrapper;
import com.redhat.devtools.intellij.quarkus.lsp4ij.console.explorer.LanguageServerExplorerTreeDataProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Action to stop the selected language server process from the language explorer.
 */
public class StopServerAction extends AnAction {

    public static final String ACTION_ID = "com.redhat.devtools.intellij.lsp4ij.console.explorer.actions.StopServerAction";

    public StopServerAction() {
        super(LanguageServerBundle.message("lsp.console.explorer.actions.stop"));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Object lsData = e.getDataContext().getData(LanguageServerExplorerTreeDataProvider.LANGUAGE_SERVER_DATA_ID);
        if (lsData instanceof LanguageServerWrapper) {
            ((LanguageServerWrapper)lsData).stop();
        }
    }
}
