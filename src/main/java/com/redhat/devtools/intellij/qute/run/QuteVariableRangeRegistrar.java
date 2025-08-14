package com.redhat.devtools.intellij.qute.run;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.tree.IElementType;
import com.redhat.devtools.intellij.qute.lang.psi.QuteTokenType;
import com.redhat.devtools.lsp4ij.dap.client.variables.providers.DebugVariableContext;
import com.redhat.devtools.lsp4ij.dap.client.variables.providers.DefaultVariableRangeRegistrar;
import org.jetbrains.annotations.NotNull;

public class QuteVariableRangeRegistrar extends DefaultVariableRangeRegistrar {

    @Override
    public boolean tryRegisterVariableRange(@NotNull IElementType tokenType, int start, int end, @NotNull Document document, @NotNull DebugVariableContext context) {
        if (tokenType == QuteTokenType.QUTE_EXPRESSION_OBJECT_PART) {
            TextRange textRange = new TextRange(start, end);
            String variableName = document.getText(textRange);
            context.addVariableRange(variableName, textRange);
        }
        return true;
    }
}
