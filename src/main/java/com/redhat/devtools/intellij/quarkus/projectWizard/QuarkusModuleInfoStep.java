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
import com.intellij.openapi.projectRoots.impl.SdkVersionUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.*;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.redhat.devtools.intellij.quarkus.QuarkusBundle;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;
import com.redhat.devtools.intellij.quarkus.buildtool.BuildToolDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JdkVersionDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static com.intellij.ide.starters.shared.ValidationFunctions.*;
import static com.intellij.ui.SimpleTextAttributes.*;
import static com.redhat.devtools.intellij.quarkus.projectWizard.QuarkusModelRegistry.DEFAULT_TIMEOUT_IN_SEC;
import static com.redhat.devtools.intellij.quarkus.projectWizard.QuarkusValidationFunctions.CHECK_CLASS_NAME;
import static com.redhat.devtools.intellij.quarkus.projectWizard.RequestHelper.waitFor;

/**
 * Quarkus module coordinates selection step of the Quarkus project wizard.
 */
public class QuarkusModuleInfoStep extends ModuleWizardStep implements Disposable {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusModuleInfoStep.class);

    private final JBLoadingPanel panel = new JBLoadingPanel(new BorderLayout(), this, 300);

    private ComboBox<QuarkusStream> streamComboBox;

    private ComboBox<BuildToolDelegate> toolComboBox;

    private ComboBox<String> javaVersionsComboBox;

    private JBCheckBox exampleField;

    private JBTextField groupIdField;

    private JBTextField artifactIdField;

    private JBTextField versionField;

    private JBTextField classNameField;

    private JBTextField pathField;

    private final AsyncProcessIcon spinner = new AsyncProcessIcon(QuarkusBundle.message("quarkus.wizard.loading.extensions"));

    private final WizardContext context;

    private QuarkusModel model;

    private Future<QuarkusExtensionsModel> extensionsModelRequest;

    private QuarkusExtensionsModel extensionsModel;
    private CollectionComboBoxModel<QuarkusStream> streamModel;
    private EmptyProgressIndicator indicator;

    private boolean isInitialized = false;
    private JdkVersionDetector.JdkVersionInfo jdkVersionInfo;
    private final List<JComponent> componentsToValidate = new ArrayList<>();

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
        context.putUserData(QuarkusConstants.WIZARD_TOOL_KEY, (BuildToolDelegate)toolComboBox.getModel().getSelectedItem());
        context.putUserData(QuarkusConstants.WIZARD_EXAMPLE_KEY, exampleField.isSelected());
        context.putUserData(QuarkusConstants.WIZARD_GROUPID_KEY, groupIdField.getText());
        context.putUserData(QuarkusConstants.WIZARD_ARTIFACTID_KEY, artifactIdField.getText());
        context.putUserData(QuarkusConstants.WIZARD_VERSION_KEY, versionField.getText());
        context.putUserData(QuarkusConstants.WIZARD_CLASSNAME_KEY, classNameField.getText());
        context.putUserData(QuarkusConstants.WIZARD_PATH_KEY, pathField.getText());
        context.putUserData(QuarkusConstants.WIZARD_EXTENSIONS_MODEL_KEY, extensionsModel);
        String selectedJava = (String) javaVersionsComboBox.getModel().getSelectedItem();
        Integer javaVersion = selectedJava == null? null: Integer.valueOf(selectedJava);
        context.putUserData(QuarkusConstants.WIZARD_JAVA_VERSION_KEY, javaVersion);
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
        jdkVersionInfo = SdkVersionUtil.getJdkVersionInfo(context.getProjectJdk().getHomePath());
        if (isInitialized) {
            return;
        }
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
                updateJavaVersions();
                loadExtensionsModel(streamModel, indicator);
            }
        });

        streamComboBox = new ComboBox<>(streamModel);
        streamComboBox.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends QuarkusStream> list, QuarkusStream stream, int index, boolean selected, boolean hasFocus) {
                SimpleTextAttributes textAttributes = getComboItemStyle(stream.isRecommended(), false);
                this.append(stream.getPlatformVersion(), textAttributes, true);
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

        final CollectionComboBoxModel<BuildToolDelegate> toolModel = new CollectionComboBoxModel<>(Arrays.asList(BuildToolDelegate.getDelegates()));
        toolComboBox = new ComboBox<>(toolModel);
        toolComboBox.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends BuildToolDelegate> list, BuildToolDelegate toolDelegate, int index, boolean selected, boolean hasFocus) {
                this.append(toolDelegate.getDisplay());
            }
        });
        formBuilder.addLabeledComponent("Tool:", toolComboBox);

        javaVersionsComboBox = new ComboBox<>();
        javaVersionsComboBox.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends String> list, String version, int index, boolean selected, boolean hasFocus) {
                QuarkusStream stream = getSelectedQuarkusStream();
                if (stream != null) {
                    String recommendedVersion = stream.getJavaCompatibility().recommended();
                    SimpleTextAttributes textAttribute = getComboItemStyle(Objects.equals(version, recommendedVersion), !isValidJava(version, jdkVersionInfo));
                    this.append(version, textAttribute, true);
                }
            }
        });
        formBuilder.addLabeledComponent("Java version:", javaVersionsComboBox);
        addValidator(javaVersionsComboBox, () -> {
            if (!isValidJava(javaVersionsComboBox.getModel().getSelectedItem().toString(), jdkVersionInfo)) {
                return new ValidationInfo(QuarkusBundle.message("quarkus.wizard.error.incompatible.jdk", jdkVersionInfo.displayVersionString()), javaVersionsComboBox);
            }
            return null;
        });

        exampleField = new JBCheckBox("If selected, project will contain sample code from extensions that support codestarts.", true);
        formBuilder.addLabeledComponent("Example code:", exampleField);

        groupIdField = new JBTextField("org.acme");
        formBuilder.addLabeledComponent("Group:", groupIdField);
        TextFieldValidator groupIdValidator = new TextFieldValidator(groupIdField, CHECK_NOT_EMPTY, CHECK_NO_WHITESPACES, CHECK_GROUP_FORMAT, CHECK_NO_RESERVED_WORDS);
        addValidator(groupIdField, groupIdValidator::validate);

        artifactIdField = new JBTextField("code-with-quarkus");
        formBuilder.addLabeledComponent("Artifact:", artifactIdField);
        TextFieldValidator artifactIdValidator = new TextFieldValidator(artifactIdField, CHECK_NOT_EMPTY, CHECK_NO_WHITESPACES, CHECK_ARTIFACT_SIMPLE_FORMAT, CHECK_NO_RESERVED_WORDS);
        addValidator(artifactIdField, artifactIdValidator::validate);

        versionField = new JBTextField("1.0.0-SNAPSHOT");
        formBuilder.addLabeledComponent("Version:", versionField);
        TextFieldValidator versionValidator = new TextFieldValidator(versionField, CHECK_NOT_EMPTY, CHECK_NO_WHITESPACES);
        addValidator(versionField, versionValidator::validate);

        classNameField = new JBTextField("org.acme.ExampleResource");
        formBuilder.addLabeledComponent("Class name:", classNameField);
        TextFieldValidator classNameValidator = new TextFieldValidator(classNameField, CHECK_NO_WHITESPACES, CHECK_CLASS_NAME);
        addValidator(classNameField, classNameValidator::validate);

        pathField = new JBTextField("/hello");
        formBuilder.addLabeledComponent("Path:", pathField);
        TextFieldValidator pathValidator = new TextFieldValidator(pathField, CHECK_NO_WHITESPACES);
        addValidator(pathField, pathValidator::validate);
        panel.add(ScrollPaneFactory.createScrollPane(formBuilder.getPanel(), true), "North");
        hideSpinner();
        extensionsModelRequest = loadExtensionsModel(streamModel, indicator);
        updateJavaVersions();
        isInitialized = true;
    }

    void addValidator(JComponent component, Supplier<ValidationInfo> validator) {
        new ComponentValidator(context.getDisposable())
                .withValidator(validator)
                .installOn(component);
        componentsToValidate.add(component);
        if (component instanceof JBTextField textField) {
            textField.getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(@NotNull DocumentEvent e) {
                    validate(textField);
                }
            });
        } else if (component instanceof ComboBox<?> combo) {
            combo.addItemListener(e -> {
                validate(combo);
            });
        }

    }

    private List<ValidationInfo> validateComponents(boolean requestFocus) {
        List<ValidationInfo> validations = new ArrayList<>(componentsToValidate.size());
        componentsToValidate.forEach(c -> validate(c).ifPresent(validations::add));
        if (requestFocus && !validations.isEmpty()) {
            var firstComponent = validations.get(0).component;
            if (firstComponent != null) {
                firstComponent.requestFocusInWindow();
            }
        }
        return validations;
    }

    private Optional<ValidationInfo> validate(@NotNull JComponent component) {
       return ComponentValidator.getInstance(component).map(v -> {
            v.revalidate();
            return v.getValidationInfo();
        });
    }

    /**
     * Checks is the Java version is compatible with the selected SDK version
     *
     * @param version       the Java version
     * @param jdkVersionInfo the version information of the selected JDK
     * @return <code>true</code> if the Java version is compatible with the selected SDK version, <code>false</code> otherwise.
     */
    private boolean isValidJava(String version, JdkVersionDetector.JdkVersionInfo jdkVersionInfo) {
        return jdkVersionInfo.version.isAtLeast(Integer.valueOf(version));
    }

    private QuarkusStream getSelectedQuarkusStream() {
        return (QuarkusStream)streamComboBox.getModel().getSelectedItem();
    }

    SimpleTextAttributes getComboItemStyle(boolean recommended, boolean invalid) {
        var style = recommended ? REGULAR_BOLD_ATTRIBUTES : REGULAR_ATTRIBUTES;
        if (invalid) {
            style = SimpleTextAttributes.merge(ERROR_ATTRIBUTES, style);
        }
        return style;
    }

    private static final List<String> defaultJavaVersions = List.of("17", "11");

    private void updateJavaVersions() {
        QuarkusStream stream = getSelectedQuarkusStream();
        QuarkusStream.JavaCompatibility javaCompatibility = stream.getJavaCompatibility();
        List<String> javaVersions;
        String recommended;
        if (javaCompatibility != null) {
            javaVersions = Arrays.stream(javaCompatibility.versions()).sorted(Comparator.reverseOrder()).toList();
            recommended = javaCompatibility.recommended();
        } else {
            javaVersions = defaultJavaVersions;
            recommended = defaultJavaVersions.get(0);
        }
        ComboBoxModel<String> javaVersionsModel = new CollectionComboBoxModel<>(javaVersions);
        javaVersionsComboBox.setModel(javaVersionsModel);
        javaVersionsModel.setSelectedItem(recommended);
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        var errors = validateComponents(false);
        if (errors.isEmpty()) {
            return toolComboBox;
        }
        return errors.get(0).component;
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
                    ApplicationManager.getApplication().invokeLater(() ->
                        Messages.showErrorDialog(QuarkusBundle.message("quarkus.wizard.error.extensions.loading.message", key, e.getMessage()), QuarkusBundle.message("quarkus.wizard.error.extensions.loading"))
                    ,  modalityState);
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
        var validations = validateComponents(true);
        if (!validations.isEmpty()) {
            return false;
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
