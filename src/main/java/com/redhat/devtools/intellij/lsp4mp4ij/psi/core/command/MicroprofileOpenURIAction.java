package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.command;

import com.google.gson.JsonPrimitive;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.redhat.devtools.intellij.lsp4ij.commands.CommandExecutor;

import java.util.List;

public class MicroprofileOpenURIAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        String url = getURL(e);
        if (url != null) {
            BrowserUtil.browse(url);
        }
    }

    private String getURL(AnActionEvent e) {
        String url = null;
        List<Object> arguments = e.getData(CommandExecutor.LSP_COMMAND).getArguments();
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
