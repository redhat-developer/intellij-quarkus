package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import org.jetbrains.annotations.NotNull;

public class LSPEditorFactoryListener implements EditorFactoryListener  {

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        if (LSPEditorWrapper.hasWrapper(event.getEditor())) {

        }
    }

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {

    }
}
