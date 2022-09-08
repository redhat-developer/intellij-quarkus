/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.psi.core.command;

import com.google.gson.JsonPrimitive;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.redhat.devtools.intellij.quarkus.lsp4ij.operations.codelens.LSPCodelensInlayProvider;

import java.util.List;

public abstract class QuteAction extends AnAction {
    protected String getURL(AnActionEvent e) {
        String url = null;
        List<Object> arguments = e.getData(LSPCodelensInlayProvider.LSP_COMMAND).getArguments();
        if (!arguments.isEmpty()) {
            Object arg = arguments.get(0);
            if (arg instanceof JsonPrimitive) {
                url = ((JsonPrimitive) arg).getAsString();
            } else if (arg instanceof String) {
                url = (String) arg;
            }
        }
        return url;
    }

}
