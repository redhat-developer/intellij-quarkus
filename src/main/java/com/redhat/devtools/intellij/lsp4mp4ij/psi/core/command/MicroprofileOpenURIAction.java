package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.command;

import com.google.gson.JsonPrimitive;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.redhat.devtools.lsp4ij.commands.LSPCommandAction;
import org.eclipse.lsp4j.Command;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MicroprofileOpenURIAction extends LSPCommandAction {

    @Override
    protected void commandPerformed(@NotNull Command command, @NotNull AnActionEvent anActionEvent) {
        String url = getURL(command.getArguments());
        if (url != null) {
            BrowserUtil.browse(url);
        }
    }

    private String getURL(List<Object> arguments) {
        String url = null;
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
