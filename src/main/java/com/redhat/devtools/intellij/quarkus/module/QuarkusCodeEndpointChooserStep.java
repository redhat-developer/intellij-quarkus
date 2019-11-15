/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.module;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.TextFieldWithStoredHistory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.QUARKUS_CODE_URL;

public class QuarkusCodeEndpointChooserStep extends ModuleWizardStep {
    private static final String LAST_ENDPOINT_URL = "quarkus.code.endpoint.url.last";
    private static final String ENDPOINT_URL_HISTORY = "quarkus.code.endpoint.url.history";
    private final WizardContext wizardContext;
    private final JBRadioButton defaultRadioButton = new JBRadioButton("Default:", true);
    private final JBRadioButton customRadioButton = new JBRadioButton("Custom:", false);
    private final TextFieldWithStoredHistory endpointURL = new TextFieldWithStoredHistory(ENDPOINT_URL_HISTORY);
    private final ComponentWithBrowseButton<TextFieldWithStoredHistory> customUrlWithBrowseButton;

    QuarkusCodeEndpointChooserStep(WizardContext wizardContext) {
        this.customUrlWithBrowseButton = new ComponentWithBrowseButton(this.endpointURL, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    QuarkusCodeEndpointChooserStep.this.validate();
                    BrowserUtil.browse(QuarkusCodeEndpointChooserStep.this.endpointURL.getText());
                } catch (ConfigurationException var3) {
                    Messages.showErrorDialog(var3.getMessage(), "Cannot Open URL");
                }

            }
        });
        this.wizardContext = wizardContext;
        String lastServiceUrl = PropertiesComponent.getInstance().getValue(LAST_ENDPOINT_URL, QUARKUS_CODE_URL);
        if (!lastServiceUrl.equals(QUARKUS_CODE_URL)) {
            this.endpointURL.setSelectedItem(lastServiceUrl);
            this.defaultRadioButton.setSelected(false);
            this.customRadioButton.setSelected(true);
        } else {
            this.defaultRadioButton.setSelected(true);
        }

        List<String> history = this.endpointURL.getHistory();
        history.remove(QUARKUS_CODE_URL);
        this.endpointURL.setHistory(history);
        this.updateCustomUrl();
    }

    public JComponent getComponent() {
        ButtonGroup group = new ButtonGroup();
        group.add(this.defaultRadioButton);
        group.add(this.customRadioButton);
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                QuarkusCodeEndpointChooserStep.this.updateCustomUrl();
            }
        };
        this.defaultRadioButton.addActionListener(listener);
        this.customRadioButton.addActionListener(listener);
        FormBuilder builder = new FormBuilder();
        builder.addComponent(new JBLabel("Choose Quarkus Code endpoint URL."));
        BorderLayoutPanel defaultPanel = JBUI.Panels.simplePanel(10, 0);
        defaultPanel.addToLeft(this.defaultRadioButton);
        HyperlinkLabel label = new HyperlinkLabel(QUARKUS_CODE_URL);
        label.setHyperlinkTarget(QUARKUS_CODE_URL);
        defaultPanel.addToCenter(label);
        builder.addComponent(defaultPanel);
        BorderLayoutPanel customPanel = JBUI.Panels.simplePanel(10, 0);
        customPanel.addToLeft(this.customRadioButton);
        this.customUrlWithBrowseButton.setButtonIcon(AllIcons.Actions.ShowViewer);
        customPanel.addToCenter(this.customUrlWithBrowseButton);
        builder.addComponent(customPanel);
        builder.addTooltip("Make sure your network connection is active before continuing.");
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(builder.getPanel(), "North");
        return panel;
    }

    private void updateCustomUrl() {
        boolean custom = this.customRadioButton.isSelected();
        this.endpointURL.getTextEditor().setEnabled(custom);
        this.endpointURL.getTextEditor().setEditable(custom);
        this.endpointURL.setEnabled(custom);
        this.customUrlWithBrowseButton.setButtonEnabled(custom);
    }

    public boolean validate() throws ConfigurationException {
        if (this.defaultRadioButton.isSelected()) {
            return true;
        } else {
            String serviceUrl = this.endpointURL.getText();
            if (serviceUrl.isEmpty()) {
                throw new ConfigurationException("Quarkus Code endpoint URL must be set");
            } else if (!serviceUrl.startsWith("http://") && !serviceUrl.startsWith("https://")) {
                throw new ConfigurationException("Invalid custom Quarkus Code endpoint URL");
            } else {
                try {
                    new URL(serviceUrl);
                    return true;
                } catch (MalformedURLException var3) {
                    throw new ConfigurationException("Invalid Custom Quarkus Code endpoint URL");
                }
            }
        }
    }

    public void updateDataModel() {
        String endpointURL = this.customRadioButton.isSelected() ? this.endpointURL.getText() : QUARKUS_CODE_URL;
        if (!Comparing.strEqual(this.wizardContext.getUserData(QuarkusConstants.WIZARD_ENDPOINT_URL_KEY), endpointURL)) {
            this.endpointURL.addCurrentTextToHistory();
            this.wizardContext.putUserData(QuarkusConstants.WIZARD_ENDPOINT_URL_KEY, endpointURL);
            PropertiesComponent.getInstance().setValue(LAST_ENDPOINT_URL, endpointURL);
        }
    }
}
