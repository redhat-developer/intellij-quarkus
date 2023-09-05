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

import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ex.Settings;
import com.intellij.profile.codeInspection.ui.ErrorsConfigurable;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import com.redhat.devtools.intellij.lsp4ij.ui.components.InspectionHyperlink;
import com.redhat.devtools.intellij.lsp4mp4ij.MicroProfileBundle;
import net.miginfocom.layout.CC;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;

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
