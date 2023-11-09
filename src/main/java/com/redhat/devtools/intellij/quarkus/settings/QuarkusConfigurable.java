/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.quarkus.settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.NamedConfigurable;
import com.intellij.openapi.util.NlsContexts;
import com.redhat.devtools.intellij.quarkus.QuarkusBundle;

import javax.swing.*;

/**
 * Quarkus configuration.
 */
public class QuarkusConfigurable extends NamedConfigurable<UserDefinedQuarkusSettings> {

    private final Project project;
    private QuarkusView myView;

    public QuarkusConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public UserDefinedQuarkusSettings getEditableObject() {
        return null;
    }

    @Override
    public @NlsContexts.DetailedDescription String getBannerSlogan() {
        return null;
    }

    @Override
    public JComponent createOptionsPanel() {
        if (myView == null) {
            myView = new QuarkusView();
        }
        return myView.getComponent();
    }

    @Override
    public void setDisplayName(String name) {
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return QuarkusBundle.message("quarkus");
    }


    @Override
    public void reset() {
        if (myView == null) return;
        UserDefinedQuarkusSettings settings = UserDefinedQuarkusSettings.getInstance(project);
        myView.setCreateQuarkusRunConfigurationOnProjectImport(settings.isCreateQuarkusRunConfigurationOnProjectImport());
    }

    @Override
    public boolean isModified() {
        if (myView == null) return false;
        UserDefinedQuarkusSettings settings = UserDefinedQuarkusSettings.getInstance(project);
        return !(myView.isCreateQuarkusRunConfigurationOnProjectImport() == settings.isCreateQuarkusRunConfigurationOnProjectImport());
    }

    @Override
    public void apply() throws ConfigurationException {
        if (myView == null) return;
        UserDefinedQuarkusSettings settings = UserDefinedQuarkusSettings.getInstance(project);
        settings.setCreateQuarkusRunConfigurationOnProjectImport(myView.isCreateQuarkusRunConfigurationOnProjectImport());
        settings.fireStateChanged();
    }
}
