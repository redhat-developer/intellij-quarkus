/*******************************************************************************
 * Copyright (c) 2022-2025 Red Hat, Inc.
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
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Settings editor for Quarkus run configurations.
 * <p>
 * This class builds the UI programmatically (no .form file)
 * using IntelliJ's FormBuilder for a clean, consistent layout.
 */
public class QuarkusRunSettingsEditor extends SettingsEditor<QuarkusRunConfiguration> {

    private final @NotNull Project project;

    private final @NotNull JPanel root;
    private final @NotNull LabeledComponent<ModulesComboBox> module;
    private final @NotNull LabeledComponent<JTextField> profile;
    private final @NotNull EnvironmentVariablesComponent envVariables;

    public QuarkusRunSettingsEditor(@NotNull Project project) {
        this.project = project;

        // --- Module selection combo box
        ModulesComboBox modulesComboBox = new ModulesComboBox();
        modulesComboBox.fillModules(project);
        module = LabeledComponent.create(modulesComboBox, "Module");

        // --- Quarkus profile input field
        JTextField profileField = new JTextField();
        profile = LabeledComponent.create(profileField, "Profile");

        // --- Environment variables component (standard IntelliJ component)
        envVariables = new EnvironmentVariablesComponent();

        // --- Build main panel layout
        // FormBuilder helps to easily create vertical forms with labels and spacing
        root = FormBuilder.createFormBuilder()
                .addLabeledComponent(module.getLabel(), module.getComponent(), 8, false)
                .addLabeledComponent(profile.getLabel(), profile.getComponent(), 8, false)
                .addComponent(envVariables, 8)
                .addVerticalGap(8)
                .getPanel();
    }

    @Override
    protected void resetEditorFrom(@NotNull QuarkusRunConfiguration configuration) {
        // Load configuration values into the UI components
        profile.getComponent().setText(configuration.getProfile());
        module.getComponent().setSelectedModule(configuration.getModule());
        envVariables.setEnvs(configuration.getEnv());
    }

    @Override
    protected void applyEditorTo(@NotNull QuarkusRunConfiguration configuration) throws ConfigurationException {
        // Save UI component values back into the configuration
        configuration.setProfile(profile.getComponent().getText());
        configuration.setModule(module.getComponent().getSelectedModule());
        configuration.setEnv(envVariables.getEnvs());
    }

    @Override
    protected @NotNull JComponent createEditor() {
        // Return the root UI panel
        return root;
    }
}
