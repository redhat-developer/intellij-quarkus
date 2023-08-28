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
package com.redhat.devtools.intellij.qute.settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.NamedConfigurable;
import com.intellij.openapi.util.NlsContexts;
import com.redhat.devtools.intellij.qute.QuteBundle;

import javax.swing.*;

/**
 * Qute configuration.
 */
public class QuteConfigurable extends NamedConfigurable<UserDefinedQuteSettings> {

    private final Project project;
    private QuteView myView;

    public QuteConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public UserDefinedQuteSettings getEditableObject() {
        return UserDefinedQuteSettings.getInstance(project);
    }

    @Override
    public @NlsContexts.DetailedDescription String getBannerSlogan() {
        return null;
    }

    @Override
    public JComponent createOptionsPanel() {
        if (myView == null) {
            myView = new QuteView();
        }
        return myView.getComponent();
    }

    @Override
    public void setDisplayName(String name) {
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return QuteBundle.message("qute.settings.title");
    }


    @Override
    public void reset() {
        if (myView == null) return;
        UserDefinedQuteSettings settings = UserDefinedQuteSettings.getInstance(project);
        myView.setValidationEnabled(settings.isValidationEnabled());
        myView.setNativeModeSupportEnabled(settings.isNativeModeSupportEnabled());
    }

    @Override
    public boolean isModified() {
        if (myView == null) return false;
        UserDefinedQuteSettings settings = UserDefinedQuteSettings.getInstance(project);
        return myView.isValidationEnabled() != settings.isValidationEnabled() ||
                myView.isNativeModeSupportEnabled() != settings.isNativeModeSupportEnabled();
    }

    @Override
    public void apply() throws ConfigurationException {
        if (myView == null) return;
        UserDefinedQuteSettings settings = UserDefinedQuteSettings.getInstance(project);
        settings.setValidationEnabled(myView.isValidationEnabled());
        settings.setNativeModeSupportEnabled(myView.isNativeModeSupportEnabled());
        settings.fireStateChanged();
    }
}
