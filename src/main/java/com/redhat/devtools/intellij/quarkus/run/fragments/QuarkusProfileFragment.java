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

import com.intellij.execution.ui.SettingsEditorFragment;
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorLabeledComponent;
import com.intellij.openapi.util.Predicates;
import com.intellij.ui.components.JBTextField;
import com.redhat.devtools.intellij.quarkus.run.QuarkusRunConfiguration;

/**
 * Fragment for Quarkus profile configuration in run configuration.
 */
public class QuarkusProfileFragment extends SettingsEditorFragment<QuarkusRunConfiguration, SettingsEditorLabeledComponent<JBTextField>> {

    public QuarkusProfileFragment() {
        super(
                "quarkus.profile",
                "Profile",
                null,
                createProfileComponent(),
                50, // Priority/weight - after module (10) but before program args (100)
                (config, labeledComponent) -> labeledComponent.getComponent().setText(config.getProfile() != null ? config.getProfile() : ""),
                (config, labeledComponent) -> config.setProfile(labeledComponent.getComponent().getText()),
                Predicates.alwaysTrue()
        );
        setRemovable(false);
        setHint("Quarkus profile to activate (e.g., dev, test, prod)");
    }

    private static SettingsEditorLabeledComponent<JBTextField> createProfileComponent() {
        JBTextField textField = new JBTextField();
        textField.setMinimumSize(new java.awt.Dimension(400, textField.getPreferredSize().height));
        return new SettingsEditorLabeledComponent<>("Profile", textField);
    }
}
