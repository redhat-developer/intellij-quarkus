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
package com.redhat.devtools.intellij.quarkus.run;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class QuarkusRunSettingsEditor extends SettingsEditor<QuarkusRunConfiguration> {
    private LabeledComponent<JTextField> profile;
    private JPanel root;
    private LabeledComponent<ModulesComboBox> module;
    private EnvironmentVariablesComponent envVariables;

    public QuarkusRunSettingsEditor(Project project) {
        module.getComponent().fillModules(project);
    }

    @Override
    protected void resetEditorFrom(@NotNull QuarkusRunConfiguration configuration) {
        profile.getComponent().setText(configuration.getProfile());
        module.getComponent().setSelectedModule(configuration.getModule());
        envVariables.setEnvs(configuration.getEnv());
    }

    @Override
    protected void applyEditorTo(@NotNull QuarkusRunConfiguration configuration) throws ConfigurationException {
        configuration.setProfile(profile.getComponent().getText());
        configuration.setModule(module.getComponent().getSelectedModule());
        configuration.setEnv(envVariables.getEnvs());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return root;
    }

    public void createUIComponents() {
        profile = new LabeledComponent<>();
        profile.setComponent(new JTextField());
    }
}
