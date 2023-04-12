package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

public class LSPVirtualFileListener implements VirtualFileListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LSPVirtualFileListener.class);

    @Override
    public void contentsChanged(@NotNull VirtualFileEvent event) {
        // The file has been saved
        final VirtualFile file = event.getFile();
        Project project = ProjectLocator.getInstance().guessProjectForFile(file);
        if (project == null) {
            return;
        }
        URI uri = LSPIJUtils.toUri(file);
        //try {

            LanguageServiceAccessor.getInstance(project);
            /*        .getLSWrappers(file, capabilities -> true)
                    .forEach(wrapper -> wrapper.documentSaved(uri, event.getNewModificationStamp()));*/
        /*} catch (IOException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }*/
    }
}
