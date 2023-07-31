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

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.redhat.devtools.intellij.quarkus.QuarkusBundle;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;
import com.redhat.devtools.intellij.quarkus.tool.ToolDelegate;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.redhat.devtools.intellij.quarkus.projectWizard.QuarkusModelRegistry.DEFAULT_TIMEOUT_IN_SEC;
import static com.redhat.devtools.intellij.quarkus.projectWizard.RequestHelper.waitFor;

/**
 * Quarkus module coordinates selection step of the Quarkus project wizard.
 */
public class QuarkusModuleInfoStep extends ModuleWizardStep implements Disposable {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusModuleInfoStep.class);

    private final JBLoadingPanel panel = new JBLoadingPanel(new BorderLayout(), this, 300);

    private ComboBox<QuarkusStream> streamComboBox;

    private ComboBox<ToolDelegate> toolComboBox;

    private JBCheckBox exampleField;

    private JBTextField groupIdField;

    private JBTextField artifactIdField;

    private JBTextField versionField;

    private JBTextField classNameField;

    private JBTextField pathField;

    private AsyncProcessIcon spinner = new AsyncProcessIcon(QuarkusBundle.message("quarkus.wizard.loading.extensions"));

    private final WizardContext context;

    private QuarkusModel model;

    private Future<QuarkusExtensionsModel> extensionsModelRequest;

    private QuarkusExtensionsModel extensionsModel;
    private CollectionComboBoxModel<QuarkusStream> streamModel;
    private EmptyProgressIndicator indicator;

    public QuarkusModuleInfoStep(WizardContext context) {
        Disposer.register(context.getDisposable(), this);
        this.context = context;
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }

    @Override
    public void updateDataModel() {
        context.putUserData(QuarkusConstants.WIZARD_TOOL_KEY, (ToolDelegate)toolComboBox.getModel().getSelectedItem());
        context.putUserData(QuarkusConstants.WIZARD_EXAMPLE_KEY, exampleField.isSelected());
        context.putUserData(QuarkusConstants.WIZARD_GROUPID_KEY, groupIdField.getText());
        context.putUserData(QuarkusConstants.WIZARD_ARTIFACTID_KEY, artifactIdField.getText());
        context.putUserData(QuarkusConstants.WIZARD_VERSION_KEY, versionField.getText());
        context.putUserData(QuarkusConstants.WIZARD_CLASSNAME_KEY, classNameField.getText());
        context.putUserData(QuarkusConstants.WIZARD_PATH_KEY, pathField.getText());
        context.putUserData(QuarkusConstants.WIZARD_EXTENSIONS_MODEL_KEY, extensionsModel);
    }

    @Override
    public void dispose() {
        if (model != null) {
            model.dispose();
        }
        if (extensionsModelRequest != null) {
            extensionsModelRequest.cancel(true);
            extensionsModelRequest = null;
        }
        model = null;
    }

    @Override
    public void _init() {
        panel.setBorder(JBUI.Borders.empty(20));

        indicator = new EmptyProgressIndicator() {
            @Override
            public void setText(String text) {
           SwingUtilities.invokeLater(() -> panel.setLoadingText(text));
            }
        };
        model = context.getUserData(QuarkusConstants.WIZARD_QUARKUS_STREAMS);
        final FormBuilder formBuilder = new FormBuilder();
        streamModel = new CollectionComboBoxModel<>(model.getStreams());
        streamModel.setSelectedItem(model.getStreams().stream().filter(QuarkusStream::isRecommended).findFirst().orElse(model.getStreams().get(0)));
        streamModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                loadExtensionsModel(streamModel, indicator);
            }
        });

        streamComboBox = new ComboBox<>(streamModel);
        streamComboBox.setRenderer(new ColoredListCellRenderer<QuarkusStream>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends QuarkusStream> list, QuarkusStream stream, int index, boolean selected, boolean hasFocus) {
                if (stream.isRecommended()) {
                    this.append(stream.getPlatformVersion(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES, true);
                } else {
                    this.append(stream.getPlatformVersion(), SimpleTextAttributes.REGULAR_ATTRIBUTES, true);
                }
                if (stream.getStatus() != null) {
                    this.append(" ").append(stream.getStatus());
                }
            }
        });
        JComponent streamComponent = new JComponent() {};
        streamComponent.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        streamComponent.add(streamComboBox);
        streamComponent.add(spinner);
        streamComponent.add(Box.createHorizontalGlue());

        formBuilder.addLabeledComponent("Quarkus stream:", streamComponent);

        final CollectionComboBoxModel<ToolDelegate> toolModel = new CollectionComboBoxModel<>(Arrays.asList(ToolDelegate.getDelegates()));
        toolComboBox = new ComboBox<>(toolModel);
        toolComboBox.setRenderer(new ColoredListCellRenderer<ToolDelegate>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends ToolDelegate> list, ToolDelegate toolDelegate, int index, boolean selected, boolean hasFocus) {
                this.append(toolDelegate.getDisplay());
            }
        });
        formBuilder.addLabeledComponent("Tool:", toolComboBox);
        exampleField = new JBCheckBox("If selected, project will contain sample code from extensions that suppport codestarts.", true);
        formBuilder.addLabeledComponent("Example code:", exampleField);
        groupIdField = new JBTextField("org.acme");
        formBuilder.addLabeledComponent("Group:", groupIdField);
        artifactIdField = new JBTextField("code-with-quarkus");
        formBuilder.addLabeledComponent("Artifact:", artifactIdField);
        versionField = new JBTextField("1.0.0-SNAPSHOT");
        formBuilder.addLabeledComponent("Version:", versionField);
        classNameField = new JBTextField("org.acme.ExampleResource");
        formBuilder.addLabeledComponent("Class name:", classNameField);
        pathField = new JBTextField("/hello");
        formBuilder.addLabeledComponent("Path:", pathField);
        panel.add(ScrollPaneFactory.createScrollPane(formBuilder.getPanel(), true), "North");
        hideSpinner();
        extensionsModelRequest = loadExtensionsModel(streamModel, indicator);
    }

    private Future<QuarkusExtensionsModel> loadExtensionsModel(CollectionComboBoxModel<QuarkusStream> streamModel, ProgressIndicator indicator) {
        String key = ((QuarkusStream) streamModel.getSelectedItem()).getKey();
        if (key == null) {
            return null;
        }
        showSpinner();
        ModalityState modalityState = getModalityState();
        return ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                return model.loadExtensionsModel(key, indicator);
            } catch (Exception e) {
                if (getComponent().isShowing()) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(QuarkusBundle.message("quarkus.wizard.error.extensions.loading.message", key, e.getMessage()), QuarkusBundle.message("quarkus.wizard.error.extensions.loading"));
                    },  modalityState);
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

    @Override
    public boolean validate() throws ConfigurationException {
        if (groupIdField.getText().isEmpty()) {
            throw new ConfigurationException("Group must be specified");
        }
        if (artifactIdField.getText().isEmpty()) {
            throw new ConfigurationException("Artifact must be specified");
        }
        if (versionField.getText().isEmpty()) {
            throw new ConfigurationException("Version must be specified");
        }
        try {
            boolean requestComplete = checkRequestComplete();
            if (!requestComplete) {
                return false;
            }
            extensionsModel = extensionsModelRequest.get(DEFAULT_TIMEOUT_IN_SEC, TimeUnit.SECONDS);
            if (extensionsModel == null) {
                //Unlikely to happen, most likely, a parsing failure will be caught in the catch block
                throw new ConfigurationException(QuarkusBundle.message("quarkus.wizard.error.extensions.loading"));
            }
        } catch (TimeoutException e) {
            throw new ConfigurationException(QuarkusBundle.message("quarkus.wizard.error.process.timeout", DEFAULT_TIMEOUT_IN_SEC), QuarkusBundle.message("quarkus.wizard.error.extensions.loading"));
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException(QuarkusBundle.message("quarkus.wizard.error.extensions.loading"), e, QuarkusBundle.message("quarkus.wizard.error.extensions.loading"));
        }
        return true;
    }

    private ModalityState getModalityState() {
        return ModalityState.stateForComponent(getComponent());
    }

    private void showSpinner() {
        spinner.setVisible(true);
        spinner.resume();
    }

    private void hideSpinner() {
        spinner.setVisible(false);
        spinner.suspend();
    }

    private boolean checkRequestComplete() {
        if (extensionsModelRequest == null) {
            extensionsModelRequest = loadExtensionsModel(streamModel, indicator);
        }
        if (extensionsModelRequest.isDone()) {
            return true;
        }
        waitFor(extensionsModelRequest, QuarkusBundle.message("quarkus.wizard.loading.extensions"),
                DEFAULT_TIMEOUT_IN_SEC*10,
                context.getProject());
        return false;
    }
}
