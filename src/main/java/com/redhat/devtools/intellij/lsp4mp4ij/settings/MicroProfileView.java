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

import com.intellij.openapi.Disposable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.redhat.devtools.intellij.lsp4ij.ui.components.InspectionHyperlink;

import javax.swing.*;

/**
 * MicroProfile view.
 */
public class MicroProfileView implements Disposable {

    private final JPanel myMainPanel;

    public MicroProfileView() {
        JPanel settingsPanel = createSettings();
        this.myMainPanel = JBUI.Panels.simplePanel(10,10)
                .addToLeft(JBUI.Panels.simplePanel())
                .addToCenter(settingsPanel);
    }

    private JPanel createSettings() {
        return FormBuilder.createFormBuilder()
                .addComponent(new InspectionHyperlink("Configure Microprofile inspections", "MicroProfile"))
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JComponent getComponent() {
        return myMainPanel;
    }
    
    @Override
    public void dispose() {

    }
}
