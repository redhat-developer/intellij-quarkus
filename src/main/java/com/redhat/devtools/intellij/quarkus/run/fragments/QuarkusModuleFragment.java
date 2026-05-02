/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.run.fragments;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.execution.ui.SettingsEditorFragment;
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorLabeledComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Predicates;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fragment for Quarkus module selection in run configuration.
 */
public class QuarkusModuleFragment extends SettingsEditorFragment<QuarkusRunConfiguration, SettingsEditorLabeledComponent<ModulesComboBox>> {

    public QuarkusModuleFragment(@NotNull Project project) {
        super(
                "quarkus.module",
                "Module",
                null,
                createLabeledComponent(project),
                10, // Priority/weight - lowest to appear first
                (config, labeledComponent) -> labeledComponent.getComponent().setSelectedModule(config.getModule()),
                (config, labeledComponent) -> config.setModule(labeledComponent.getComponent().getSelectedModule()),
                Predicates.alwaysTrue()
        );
        setRemovable(false);
        setHint("Select the module containing the Quarkus application");
    }

    @Nullable
    public Module getSelectedModule() {
        return component().getComponent().getSelectedModule();
    }

    private static SettingsEditorLabeledComponent<ModulesComboBox> createLabeledComponent(@NotNull Project project) {
        ModulesComboBox comboBox = new ModulesComboBox();
        comboBox.fillModules(project);
        comboBox.setMinimumSize(new java.awt.Dimension(400, comboBox.getPreferredSize().height));
        return new SettingsEditorLabeledComponent<>("Module", comboBox);
    }
}
