/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.run;

import com.redhat.devtools.lsp4ij.dap.client.variables.providers.DebugVariablePositionProvider;
import com.redhat.devtools.lsp4ij.dap.client.variables.providers.HighlighterDebugVariablePositionProvider;
import com.redhat.devtools.lsp4ij.dap.descriptors.DebugAdapterVariableSupport;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * Qute debug adapter variable support.
 */
public class QuteDebugAdapterVariableSupport extends DebugAdapterVariableSupport {

    @Override
    public @NotNull Collection<DebugVariablePositionProvider> getDebugVariablePositionProvider() {
        return Collections.singletonList(new QuteDebugVariablePositionProvider());
    }
}
