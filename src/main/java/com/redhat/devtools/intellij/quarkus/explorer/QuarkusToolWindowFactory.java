package com.redhat.devtools.intellij.quarkus.explorer;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.redhat.devtools.intellij.lsp4ij.LanguageServerBundle;
import com.redhat.devtools.intellij.quarkus.QuarkusBundle;
import org.jetbrains.annotations.NotNull;

public class QuarkusToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        QuarkusExplorer explorer = new QuarkusExplorer(project);
        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.getFactory().createContent(explorer,
                QuarkusBundle.message("quarkus.tool.window.display.name"), false);
        content.setDisposer(explorer);
        contentManager.addContent(content);
    }
}
