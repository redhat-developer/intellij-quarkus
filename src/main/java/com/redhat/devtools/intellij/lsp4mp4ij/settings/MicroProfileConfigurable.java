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
package com.redhat.devtools.intellij.lsp4mp4ij.settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.NamedConfigurable;
import com.intellij.openapi.util.NlsContexts;
import com.redhat.devtools.intellij.lsp4mp4ij.MicroProfileBundle;

import javax.swing.*;

/**
 * MicroProfile configuration.
 */
public class MicroProfileConfigurable extends NamedConfigurable<UserDefinedMicroProfileSettings> {

    private final Project project;
    private MicroProfileView myView;

    public MicroProfileConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public UserDefinedMicroProfileSettings getEditableObject() {
        return null;
    }

    @Override
    public @NlsContexts.DetailedDescription String getBannerSlogan() {
        return null;
    }

    @Override
    public JComponent createOptionsPanel() {
        if (myView == null) {
            myView = new MicroProfileView();
        }
        return myView.getComponent();
    }

    @Override
    public void setDisplayName(String name) {
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return MicroProfileBundle.message("microprofile");
    }


    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
    }
}
