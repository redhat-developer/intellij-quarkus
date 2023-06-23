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
package com.redhat.devtools.intellij.lsp4mp4ij.settings.java;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.NamedConfigurable;
import com.intellij.openapi.util.NlsContexts;
import com.redhat.devtools.intellij.lsp4mp4ij.MicroProfileBundle;
import com.redhat.devtools.intellij.lsp4mp4ij.settings.UserDefinedMicroProfileSettings;
import com.redhat.devtools.intellij.lsp4mp4ij.settings.properties.MicroProfilePropertiesView;

import javax.swing.*;

/**
 *
 */
public class MicroProfileJavaConfigurable extends NamedConfigurable<UserDefinedMicroProfileSettings> {

    private final UserDefinedMicroProfileSettings myMicroProfileSettings;
    private MicroProfileJavaView myView;
    private String myDisplayName;

    public MicroProfileJavaConfigurable(UserDefinedMicroProfileSettings microProfileSettings) {
        this.myMicroProfileSettings = microProfileSettings;
    }

    @Override
    public UserDefinedMicroProfileSettings getEditableObject() {
        return myMicroProfileSettings;
    }

    @Override
    public @NlsContexts.DetailedDescription String getBannerSlogan() {
        return null;
    }

    @Override
    public JComponent createOptionsPanel() {
        if (myView == null) {
            myView = new MicroProfileJavaView();
        }
        return myView.getComponent();
    }

    @Override
    public void setDisplayName(String name) {
        myDisplayName = name;
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return MicroProfileBundle.message("microprofile.java");
    }

    @Override
    public void reset() {
        if (myView == null) return;
        UserDefinedMicroProfileSettings settings = UserDefinedMicroProfileSettings.getInstance();
        myView.setUrlCodeLensEnabled(settings.isUrlCodeLensEnabled());
    }

    @Override
    public boolean isModified() {
        if (myView == null) return false;
        UserDefinedMicroProfileSettings settings = UserDefinedMicroProfileSettings.getInstance();
        if (settings == null) {
            return true;
        }
        return !(myView.isUrlCodeLensEnabled()== settings.isUrlCodeLensEnabled());
    }

    @Override
    public void apply() throws ConfigurationException {
        if (myView == null) return;
        UserDefinedMicroProfileSettings settings = UserDefinedMicroProfileSettings.getInstance();
        settings.setUrlCodeLensEnabled(myView.isUrlCodeLensEnabled());
        settings.fireStateChanged();
    }
}
