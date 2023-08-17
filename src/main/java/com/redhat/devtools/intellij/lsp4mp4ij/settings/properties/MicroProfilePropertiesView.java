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
package com.redhat.devtools.intellij.lsp4mp4ij.settings.properties;

import com.intellij.openapi.Disposable;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import com.redhat.devtools.intellij.lsp4mp4ij.MicroProfileBundle;

import javax.swing.*;
import javax.swing.border.TitledBorder;

/**
 * MicroProfile Properties view.
 */
public class MicroProfilePropertiesView implements Disposable {

    private final JPanel myMainPanel;

    private final JBCheckBox inlayHintCheckBox = new JBCheckBox(MicroProfileBundle.message("microprofile.properties.inlayHint.enabled"));

    public MicroProfilePropertiesView() {
        JComponent descriptionPanel = createDescription(null);
        JPanel settingsPanel = createSettings(descriptionPanel);
        TitledBorder title = IdeBorderFactory.createTitledBorder(MicroProfileBundle.message("microprofile.properties.title"));
        settingsPanel.setBorder(title);
        this.myMainPanel = JBUI.Panels.simplePanel(10, 10)
                .addToLeft(JBUI.Panels.simplePanel())
                .addToCenter(settingsPanel);
    }

    private JPanel createSettings(JComponent description) {
        return FormBuilder.createFormBuilder()
                .addComponent(description, 0)
                .addComponent(inlayHintCheckBox, 5)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    private JComponent createDescription(String description) {
        /**
         * Normally comments are below the controls.
         * Here we want the comments to precede the controls, we therefore create an empty, 0-sized panel.
         */
        JPanel titledComponent = UI.PanelFactory.grid().createPanel();
        titledComponent.setMinimumSize(JBUI.emptySize());
        titledComponent.setPreferredSize(JBUI.emptySize());
        if (description != null && !description.isBlank()) {
            titledComponent = UI.PanelFactory.panel(titledComponent)
                    .withComment(description)
                    .resizeX(true)
                    .resizeY(true)
                    .createPanel();
        }
        return titledComponent;
    }


    public JComponent getComponent() {
        return myMainPanel;
    }


    public boolean isInlayHintEnabled() {
        return inlayHintCheckBox.isSelected();
    }

    public void setInlayHintEnabled(boolean inlayHint) {
        inlayHintCheckBox.setSelected(inlayHint);
    }

    @Override
    public void dispose() {

    }
}
