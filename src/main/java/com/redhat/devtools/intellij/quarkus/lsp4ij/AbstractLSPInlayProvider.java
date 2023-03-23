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
package com.redhat.devtools.intellij.quarkus.lsp4ij;

import com.intellij.codeInsight.hints.ChangeListener;
import com.intellij.codeInsight.hints.ImmediateConfigurable;
import com.intellij.codeInsight.hints.InlayHintsProvider;
import com.intellij.codeInsight.hints.NoSettings;
import com.intellij.codeInsight.hints.SettingsKey;
import com.intellij.ide.DataManager;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.ui.DialogPanel;
import com.intellij.util.ui.JBUI;
import org.eclipse.lsp4j.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public abstract class AbstractLSPInlayProvider implements InlayHintsProvider<NoSettings> {
    public static final DataKey<Command> LSP_COMMAND = DataKey.create("com.redhat.devtools.intellij.quarkus.lsp4ij.command");

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
                JLabel label = new JLabel();
                GridBagConstraints constraints = new GridBagConstraints(
                        0,
                        GridBagConstraints.RELATIVE,
                        1,
                        1,
                        0,
                        0,
                        GridBagConstraints.NORTHWEST,
                        GridBagConstraints.NONE,
                        JBUI.emptyInsets(),
                        0,
                        0);
                label.setText("LSP");
                DialogPanel panel = new DialogPanel(new GridBagLayout());
                panel.add(label, constraints);
                return panel;
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
                DataContext context = SimpleDataContext.getSimpleContext(DataKey.create(LSP_COMMAND.getName()), command, DataManager.getInstance().getDataContext(source));
                action.actionPerformed(new AnActionEvent(null, context,
                        ActionPlaces.UNKNOWN, new Presentation(),
                        ActionManager.getInstance(), 0));
            }
        }
    }
}
