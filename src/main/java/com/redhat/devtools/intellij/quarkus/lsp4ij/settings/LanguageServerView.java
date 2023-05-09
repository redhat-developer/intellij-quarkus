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
package com.redhat.devtools.intellij.quarkus.lsp4ij.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UI;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageServerBundle;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageServersRegistry;

import javax.swing.*;

/**
 * UI settings view to configure a given language server:
 *
 * <ul>
 *     <li>Debug port</li>
 *     <li>Suspend and wait for a debugger?</li>
 * </ul>
 */
public class LanguageServerView implements Disposable {

    private final JPanel myMainPanel;
    private JBTextField debugPortField = new JBTextField();

    private JBCheckBox debugSuspendCheckBox = new JBCheckBox(LanguageServerBundle.message("language.server.debug.suspend"));

    private ComboBox<ServerTrace> serverTraceComboBox = new ComboBox<>(new DefaultComboBoxModel<>(ServerTrace.values()));

    public LanguageServerView(LanguageServersRegistry.LanguageServerDefinition languageServerDefinition) {
        this.myMainPanel = FormBuilder.createFormBuilder()
                .setFormLeftIndent(10)
                .addComponent(createTitleComponent(languageServerDefinition), 1)
                .addLabeledComponent(LanguageServerBundle.message("language.server.debug.port"), debugPortField, 1)
                .addComponent(debugSuspendCheckBox, 1)
                .addLabeledComponent(LanguageServerBundle.message("language.server.trace"), serverTraceComboBox, 1)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    private JComponent createTitleComponent(LanguageServersRegistry.LanguageServerDefinition languageServerDefinition) {
        JLabel title = new JLabel(languageServerDefinition.getDisplayName());
        String description = languageServerDefinition.description;
        if (description != null && description.length() > 0) {
            // @See com.intellij.internal.ui.ComponentPanelTestAction for more details on how to create comment panels
            return UI.PanelFactory.panel(title)
                    .withComment(description)
                    .createPanel();
        }
        return title;
    }

    public JComponent getComponent() {
        return myMainPanel;
    }

    public String getDebugPort() {
        return debugPortField.getText();
    }

    public void setDebugPort(String debugPort) {
        debugPortField.setText(debugPort);
    }

    public boolean isDebugSuspend() {
        return debugSuspendCheckBox.isSelected();
    }

    public void setDebugSuspend(boolean debugSuspend) {
        debugSuspendCheckBox.setSelected(debugSuspend);
    }

    public ServerTrace getServerTrace() {
        return (ServerTrace) serverTraceComboBox.getSelectedItem();
    }

    public void setServerTrace(ServerTrace serverTrace) {
        serverTraceComboBox.setSelectedItem(serverTrace);
    }

    @Override
    public void dispose() {

    }

}
