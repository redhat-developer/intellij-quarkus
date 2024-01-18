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

import com.intellij.ide.util.projectWizard.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;
import com.redhat.devtools.intellij.quarkus.TelemetryService;
import com.redhat.devtools.intellij.telemetry.core.service.TelemetryMessageBuilder;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class QuarkusModuleBuilder extends JavaModuleBuilder {

    private WizardContext wizardContext;

    public QuarkusModuleBuilder() {
        super();
    }

    @Nullable
    @Override
    public String getBuilderId() {
        return "quarkus";
    }

    @Override
    public String getDescription() {
        return "Create <b>Quarkus</b> projects using code.quarkus.io provided by Quarkus Tools";
    }

    @Override
    public String getPresentableName() {
        return "Quarkus";
    }

    @Override
    public Icon getNodeIcon() {
        return IconLoader.getIcon("/quarkus_icon_rgb_16px_default.png", QuarkusModuleBuilder.class);
    }

    @Nullable
    @Override
    public ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
        return new QuarkusCodeEndpointChooserStep(context);
    }

    @Override
    public ModuleWizardStep[] createWizardSteps(@NotNull WizardContext wizardContext, @NotNull ModulesProvider modulesProvider) {
        return new ModuleWizardStep[]{new QuarkusModuleInfoStep(wizardContext), new QuarkusExtensionsStep(wizardContext)};
    }

    @Nullable
    @Override
    public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
        ModuleNameLocationSettings moduleNameLocationSettings = settingsStep.getModuleNameLocationSettings();
        if (moduleNameLocationSettings != null) {
            moduleNameLocationSettings.setModuleName(settingsStep.getContext().getUserData(QuarkusConstants.WIZARD_ARTIFACTID_KEY));
        }
        return super.modifySettingsStep(settingsStep);
    }

    @NotNull
    @Override
    public Module createModule(@NotNull ModifiableModuleModel moduleModel) throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {
        TelemetryMessageBuilder.ActionMessage telemetry = TelemetryService.instance().action(TelemetryService.UI_PREFIX + "wizard");
        try {
            processDownload();
            Module module = super.createModule(moduleModel);
            wizardContext.getUserData(QuarkusConstants.WIZARD_TOOL_KEY).processImport(module);
            telemetry.send();
            return module;
        } catch (IOException | InvalidDataException | ModuleWithNameAlreadyExists | JDOMException | ConfigurationException e) {
            telemetry.error(e).send();
            throw e;
        }
    }

    private void processDownload() throws IOException {
        File moduleFile = new File(getContentEntryPath());
        var createQuarkusProjectRequest = new QuarkusModelRegistry.CreateQuarkusProjectRequest();
        createQuarkusProjectRequest.endpoint = wizardContext.getUserData(QuarkusConstants.WIZARD_ENDPOINT_URL_KEY);
        createQuarkusProjectRequest.tool = wizardContext.getUserData(QuarkusConstants.WIZARD_TOOL_KEY).asParameter();
        createQuarkusProjectRequest.groupId = wizardContext.getUserData(QuarkusConstants.WIZARD_GROUPID_KEY);
        createQuarkusProjectRequest.artifactId = wizardContext.getUserData(QuarkusConstants.WIZARD_ARTIFACTID_KEY);
        createQuarkusProjectRequest.version = wizardContext.getUserData(QuarkusConstants.WIZARD_VERSION_KEY);
        createQuarkusProjectRequest.className = wizardContext.getUserData(QuarkusConstants.WIZARD_CLASSNAME_KEY);
        createQuarkusProjectRequest.path = wizardContext.getUserData(QuarkusConstants.WIZARD_PATH_KEY);
        createQuarkusProjectRequest.model = wizardContext.getUserData(QuarkusConstants.WIZARD_EXTENSIONS_MODEL_KEY);
        createQuarkusProjectRequest.javaVersion = wizardContext.getUserData(QuarkusConstants.WIZARD_JAVA_VERSION_KEY);
        createQuarkusProjectRequest.output = moduleFile;
        createQuarkusProjectRequest.codeStarts = wizardContext.getUserData(QuarkusConstants.WIZARD_EXAMPLE_KEY);
        QuarkusModelRegistry.zip(createQuarkusProjectRequest);
        updateWrapperPermissions(moduleFile);
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(moduleFile);
        RefreshQueue.getInstance().refresh(true, true, (Runnable) null, new VirtualFile[]{vf});
    }

    private void updateWrapperPermissions(File moduleFile) {
        File f = new File(moduleFile, "mvnw");
        if (f.exists()) {
            f.setExecutable(true, false);
        }
        f = new File(moduleFile, "gradlew");
        if (f.exists()) {
            f.setExecutable(true, false);
        }
    }

    @Override
    public ModuleWizardStep[] createFinishingSteps(@NotNull WizardContext wizardContext, @NotNull ModulesProvider modulesProvider) {
        this.wizardContext = wizardContext;
        return super.createFinishingSteps(wizardContext, modulesProvider);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
