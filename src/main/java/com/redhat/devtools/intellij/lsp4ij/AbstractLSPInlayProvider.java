/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.lsp4ij;

import com.intellij.codeInsight.hints.*;
import com.intellij.ide.DataManager;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.layout.LCFlags;
import com.intellij.ui.layout.LayoutKt;
import com.redhat.devtools.intellij.lsp4ij.commands.CommandExecutor;
import org.eclipse.lsp4j.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.Component;

public abstract class AbstractLSPInlayProvider implements InlayHintsProvider<NoSettings> {

    private final Key<InlayHintsSink> sinkKey;

    protected AbstractLSPInlayProvider(Key<InlayHintsSink> sinkKey) {
        this.sinkKey = sinkKey;
    }

    private SettingsKey<NoSettings> key = new SettingsKey<>("LSP.hints");

    @Override
    public boolean isVisibleInSettings() {
        return true;
    }

    @NotNull
    @Override
    public SettingsKey<NoSettings> getKey() {
        return key;
    }

    @NotNull
    @Override
    public String getName() {
        return "LSP";
    }

    @Nullable
    @Override
    public String getPreviewText() {
        return "Preview";
    }

    @NotNull
    @Override
    public ImmediateConfigurable createConfigurable(@NotNull NoSettings o) {
        return new ImmediateConfigurable() {
            @NotNull
            @Override
            public JComponent createComponent(@NotNull ChangeListener changeListener) {
                return LayoutKt.panel(new LCFlags[0], "LSP", builder -> {
                    return null;
                });
            }
        };
    }

    @NotNull
    @Override
    public NoSettings createSettings() {
        return new NoSettings();
    }

    @Override
    public boolean isLanguageSupported(@NotNull Language language) {
        return true;
    }

    protected void executeClientCommand(Component source, Command command) {
        if (command != null) {
            AnAction action = ActionManager.getInstance().getAction(command.getCommand());
            if (action != null) {
                DataContext context = SimpleDataContext.getSimpleContext(CommandExecutor.LSP_COMMAND, command, DataManager.getInstance().getDataContext(source));
                action.actionPerformed(new AnActionEvent(null, context,
                        ActionPlaces.UNKNOWN, new Presentation(),
                        ActionManager.getInstance(), 0));
            }
        }
    }

    /**
     * Returns the virtual file where inlay hint must be added and null otherwise.
     *
     * @param psiFile        the psi file.
     * @param editor         the editor.
     * @param inlayHintsSink the inlay hints sink.
     * @return the virtual file where inlay hint must be added and null otherwise.
     */
    protected @Nullable VirtualFile getFile(@NotNull PsiFile psiFile, @NotNull Editor editor, @NotNull InlayHintsSink inlayHintsSink) {
        Project project = psiFile.getProject();
        if (project.isDisposed()) {
            // The project has been closed, don't collect inlay hints.
            return null;
        }
        // Before IJ 2023-3, FactoryInlayHintsCollector#collect(PsiElement element.. is called once time with PsiFile as element.
        // Since IJ 2023-3, FactoryInlayHintsCollector#collect(PsiElement element.. is called several times for each tokens of the PsiFile
        // which causes the problem of codelens/inlay hint which are not displayed because there are too many call of LSP request codelens/inlayhint which are cancelled.
        // With IJ 2023-3 we need to collect LSP CodeLens/InlayHint just for the first call. To implement this idea, we store the instance InlayHintsSink,
        // and we forbid the compute of inlay hint if InlayHintsSink is already filled.
        InlayHintsSink sink = editor.getUserData(sinkKey);
        if (sink == inlayHintsSink) {
            // LSP CodeLens/InlayHint has already be done for teh file, ignore it.
            return null;
        }
        editor.putUserData(sinkKey, inlayHintsSink);
        return LSPIJUtils.getFile(psiFile);
    }
}
