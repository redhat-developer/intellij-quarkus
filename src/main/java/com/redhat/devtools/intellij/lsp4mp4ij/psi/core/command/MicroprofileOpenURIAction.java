package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.command;

import com.google.gson.JsonPrimitive;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.redhat.devtools.lsp4ij.commands.LSPCommand;
import com.redhat.devtools.lsp4ij.commands.LSPCommandAction;
import org.jetbrains.annotations.NotNull;

public class MicroprofileOpenURIAction extends LSPCommandAction {

    @Override
    protected void commandPerformed(@NotNull LSPCommand command, @NotNull AnActionEvent anActionEvent) {
        String url = getURL(command);
        if (url != null) {
            BrowserUtil.browse(url);
        }
    }

    private String getURL(LSPCommand command) {
        Object arg = command.getArgumentAt(0);
        if (arg instanceof JsonPrimitive) {
            return ((JsonPrimitive) arg).getAsString();
        }
        if (arg instanceof String) {
            return (String) arg;
        }
        return null;
    }
}
