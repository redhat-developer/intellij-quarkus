package com.redhat.devtools.intellij.qute.run;

import com.redhat.devtools.lsp4ij.dap.client.variables.providers.*;

import java.util.List;

public class QuteDebugVariablePositionProvider extends HighlighterDebugVariablePositionProvider
{
    public QuteDebugVariablePositionProvider() {
        super(List.of(new QuteVariableRangeRegistrar()));
    }
}
