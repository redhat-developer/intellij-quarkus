package com.redhat.devtools.intellij.qute.run;

import com.redhat.devtools.lsp4ij.dap.client.variables.providers.DebugVariablePositionProvider;
import com.redhat.devtools.lsp4ij.dap.client.variables.providers.HighlighterDebugVariablePositionProvider;
import com.redhat.devtools.lsp4ij.dap.descriptors.DebugAdapterVariableSupport;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public class QuteDebugAdapterVariableSupport extends DebugAdapterVariableSupport {

    @Override
    public @NotNull Collection<DebugVariablePositionProvider> getDebugVariablePositionProvider() {
        return Collections.singletonList(new QuteDebugVariablePositionProvider());
    }
}
