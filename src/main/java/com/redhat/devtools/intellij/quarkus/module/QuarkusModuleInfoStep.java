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

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.io.IOException;

public class QuarkusModuleInfoStep extends ModuleWizardStep implements Disposable {
    private final JBLoadingPanel panel = new JBLoadingPanel(new BorderLayout(), this, 300);

    private JBTextField groupIdField;

    private JBTextField artifactIdField;

    private JBTextField versionField;

    private JBTextField classNameField;

    private JBTextField pathField;

    private final WizardContext context;

    public QuarkusModuleInfoStep(WizardContext context) {
        this.context = context;
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }

    @Override
    public void updateDataModel() {
        context.putUserData(QuarkusConstants.WIZARD_GROUPID_KEY, groupIdField.getText());
        context.putUserData(QuarkusConstants.WIZARD_ARTIFACTID_KEY, artifactIdField.getText());
        context.putUserData(QuarkusConstants.WIZARD_VERSION_KEY, versionField.getText());
        context.putUserData(QuarkusConstants.WIZARD_CLASSNAME_KEY, classNameField.getText());
        context.putUserData(QuarkusConstants.WIZARD_PATH_KEY, pathField.getText());
    }

    @Override
    public void dispose() {

    }

    @Override
    public void _init() {
        ProgressIndicator indicator = new EmptyProgressIndicator() {
            @Override
            public void setText(String text) {
                SwingUtilities.invokeLater(() -> panel.setLoadingText(text));
            }
        };
        try {
            QuarkusModel model = QuarkusModelRegistry.INSTANCE.load(context.getUserData(QuarkusConstants.WIZARD_ENDPOINT_URL_KEY), indicator);
            context.putUserData(QuarkusConstants.WIZARD_MODEL_KEY, model);
            final FormBuilder formBuilder = new FormBuilder();
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
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        return true;
    }
}
