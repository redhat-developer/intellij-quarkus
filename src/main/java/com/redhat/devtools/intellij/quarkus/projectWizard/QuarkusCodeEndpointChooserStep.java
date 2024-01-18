/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.quarkus.projectWizard;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.TextFieldWithStoredHistory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.redhat.devtools.intellij.quarkus.QuarkusBundle;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.QUARKUS_CODE_URL;
import static com.redhat.devtools.intellij.quarkus.projectWizard.QuarkusModelRegistry.DEFAULT_TIMEOUT_IN_SEC;
import static com.redhat.devtools.intellij.quarkus.projectWizard.RequestHelper.waitFor;

/**
 * Quarkus Code endpoint selection step of the Quarkus project wizard.
 */
public class QuarkusCodeEndpointChooserStep extends ModuleWizardStep implements Disposable {
    private static final String LAST_ENDPOINT_URL = "quarkus.code.endpoint.url.last";
    private static final String ENDPOINT_URL_HISTORY = "quarkus.code.endpoint.url.history";
    private final WizardContext wizardContext;
    private final JBRadioButton defaultRadioButton = new JBRadioButton("Default:", true);
    private final JBRadioButton customRadioButton = new JBRadioButton("Custom:", false);
    private final TextFieldWithStoredHistory endpointURL = new TextFieldWithStoredHistory(ENDPOINT_URL_HISTORY);
    private final ComponentWithBrowseButton<TextFieldWithStoredHistory> customUrlWithBrowseButton;
    private final JPanel component;

    private Future<QuarkusModel> loadingRequest = null;

    private QuarkusModel streams = null;

    private AsyncProcessIcon spinner = new AsyncProcessIcon(QuarkusBundle.message("quarkus.wizard.loading.streams"));

    QuarkusCodeEndpointChooserStep(WizardContext wizardContext) {
        Disposer.register(wizardContext.getDisposable(), this);
        this.customUrlWithBrowseButton = new ComponentWithBrowseButton(this.endpointURL, e -> {
            try {
                QuarkusCodeEndpointChooserStep.this.validate();
                BrowserUtil.browse(QuarkusCodeEndpointChooserStep.this.endpointURL.getText());
            } catch (ConfigurationException var3) {
                Messages.showErrorDialog(var3.getMessage(), "Cannot open URL");
            }

        });

        endpointURL.addActionListener(e -> {
            if (customRadioButton.isSelected()) {
                updateCustomUrl();
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
        this.component = createComponent();
    }

    @Override
    public void _init() {
        super._init();
        SwingUtilities.invokeLater(() -> {
            this.updateCustomUrl();
        });
    }

    @Override
    public JComponent getComponent() {
        return component;
    }

    public JPanel createComponent() {
        ButtonGroup group = new ButtonGroup();
        group.add(this.defaultRadioButton);
        group.add(this.customRadioButton);
        ActionListener listener = e -> QuarkusCodeEndpointChooserStep.this.updateCustomUrl();
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
        this.customUrlWithBrowseButton.setButtonIcon(AllIcons.Actions.Preview);
        customPanel.addToCenter(this.customUrlWithBrowseButton);
        builder.addComponent(customPanel);
        builder.addTooltip("Make sure your network connection is active.");
        builder.addComponentFillVertically(spinner, 10);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(builder.getPanel(), "North");
        panel.setBorder(JBUI.Borders.emptyLeft(20));
        return panel;
    }

    private void updateCustomUrl() {
        boolean custom = this.customRadioButton.isSelected();
        this.endpointURL.getTextEditor().setEnabled(custom);
        this.endpointURL.getTextEditor().setEditable(custom);
        this.endpointURL.setEnabled(custom);
        this.customUrlWithBrowseButton.setButtonEnabled(custom);
        if (loadingRequest != null) {
            loadingRequest.cancel(true);
        }
        loadingRequest = loadStreams();
    }

    private Future<QuarkusModel> loadStreams() {
        final String endpoint = getSelectedEndpointUrl();
        if (endpoint == null || endpoint.isBlank()) {
            return null;
        }
        showSpinner();
        ModalityState modalityState = getModalityState();
        return ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                return QuarkusModelRegistry.INSTANCE.loadStreams(endpoint, new EmptyProgressIndicator());
            } catch (Exception e) {
                if (getComponent().isShowing()) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (getComponent().isShowing()) {
                            Messages.showErrorDialog(QuarkusBundle.message("quarkus.wizard.error.streams.loading.message", endpoint, e.getMessage()), QuarkusBundle.message("quarkus.wizard.error.streams.loading"));
                        }
                    }, modalityState);
                }
            } finally {
                if (getComponent().isShowing()) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (getComponent().isShowing()) {
                            hideSpinner();
                        }
                    }, modalityState);
                }
            }
            return null;
        });
    }

    private ModalityState getModalityState() {
       return ModalityState.stateForComponent(getComponent());
    }

    private void showSpinner() {
        spinner.setVisible(true);
        spinner.resume();
    }

    private void hideSpinner() {
        spinner.suspend();
        spinner.setVisible(false);
    }

    @Override
    public boolean validate() throws ConfigurationException {
        if (this.customRadioButton.isSelected()) {
            String serviceUrl = this.endpointURL.getText();
            if (serviceUrl.isEmpty()) {
                throw new ConfigurationException("Quarkus Code endpoint URL must be set");
            } else if (!serviceUrl.matches("^https?://.*")) {
                throw new ConfigurationException("Invalid custom Quarkus Code endpoint URL");
            }
        }
        try {
            boolean requestComplete = checkRequestComplete();
            if (!requestComplete) {
                return false;
            }
            streams = loadingRequest.get(DEFAULT_TIMEOUT_IN_SEC, TimeUnit.SECONDS);
            if (streams == null) {
                //Unlikely to happen, most likely, a parsing failure will be caught in the catch block
                throw new ConfigurationException(QuarkusBundle.message("quarkus.wizard.error.streams.loading"));
            }
        } catch (TimeoutException e) {
            throw new ConfigurationException(QuarkusBundle.message("quarkus.wizard.error.process.timeout", DEFAULT_TIMEOUT_IN_SEC), QuarkusBundle.message("quarkus.wizard.error.streams.loading"));
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException(QuarkusBundle.message("quarkus.wizard.error.streams.loading"), e, QuarkusBundle.message("quarkus.wizard.error.streams.loading"));
        }
        return true;
    }

    private boolean checkRequestComplete() throws ExecutionException, InterruptedException {
        if (loadingRequest == null) {
            loadingRequest = loadStreams();
        }
        if (loadingRequest.isDone()){
            return true;
        }
        waitFor(loadingRequest, QuarkusBundle.message("quarkus.wizard.loading.streams"),
                DEFAULT_TIMEOUT_IN_SEC*10,
                wizardContext.getProject());
        return false;
    }

    @Override
    public void updateDataModel() {
        String endpointURL = getSelectedEndpointUrl();

        if (!Comparing.strEqual(this.wizardContext.getUserData(QuarkusConstants.WIZARD_ENDPOINT_URL_KEY), endpointURL)) {
            this.endpointURL.addCurrentTextToHistory();
            this.wizardContext.putUserData(QuarkusConstants.WIZARD_ENDPOINT_URL_KEY, endpointURL);
            this.wizardContext.putUserData(QuarkusConstants.WIZARD_QUARKUS_STREAMS, streams);
            PropertiesComponent.getInstance().setValue(LAST_ENDPOINT_URL, endpointURL);
        }
    }

    private String getSelectedEndpointUrl() {
        return this.customRadioButton.isSelected() ? this.endpointURL.getText() : QUARKUS_CODE_URL;
    }

    @Override
    public void dispose() {
        if (loadingRequest != null) {
            loadingRequest.cancel(true);
            loadingRequest = null;
        }
        streams = null;
    }
}
