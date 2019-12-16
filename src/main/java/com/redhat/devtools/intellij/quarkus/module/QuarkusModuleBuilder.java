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

import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleNameLocationSettings;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.util.Url;
import com.intellij.util.Urls;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.io.RequestBuilder;
import com.redhat.devtools.intellij.quarkus.QuarkusConstants;
import org.jdom.JDOMException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.zeroturnaround.zip.ZipUtil;

import javax.swing.Icon;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_NAME;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_VALUE;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_QUARKUS_IO_CLIENT_NAME_HEADER_NAME;
import static com.redhat.devtools.intellij.quarkus.QuarkusConstants.CODE_QUARKUS_IO_CLIENT_NAME_HEADER_VALUE;

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

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @Override
    public String getDescription() {
        return "Quarkus module";
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
        processDownload();
        Module module = super.createModule(moduleModel);
        wizardContext.getUserData(QuarkusConstants.WIZARD_TOOL_KEY).processImport(module);
        return module;
    }

    private void processDownload() throws IOException {
        Url url = Urls.newFromEncoded(wizardContext.getUserData(QuarkusConstants.WIZARD_ENDPOINT_URL_KEY) + "/api/download");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("b", wizardContext.getUserData(QuarkusConstants.WIZARD_TOOL_KEY).asParameter());
        parameters.put("g", wizardContext.getUserData(QuarkusConstants.WIZARD_GROUPID_KEY));
        parameters.put("a", wizardContext.getUserData(QuarkusConstants.WIZARD_ARTIFACTID_KEY));
        parameters.put("v", wizardContext.getUserData(QuarkusConstants.WIZARD_VERSION_KEY));
        parameters.put("c", wizardContext.getUserData(QuarkusConstants.WIZARD_CLASSNAME_KEY));
        parameters.put("p", wizardContext.getUserData(QuarkusConstants.WIZARD_PATH_KEY));
        url = url.addParameters(parameters);
        QuarkusModel model = wizardContext.getUserData(QuarkusConstants.WIZARD_MODEL_KEY);
        for(QuarkusCategory category : model.getCategories()) {
            for(QuarkusExtension extension : category.getExtensions()) {
                if (extension.isSelected()) {
                    url = url.addParameters(Collections.singletonMap("e", extension.getId()));
                }
            }
        }
        RequestBuilder builder = HttpRequests.request(url.toString()).tuner(connection -> {
            connection.setRequestProperty(CODE_QUARKUS_IO_CLIENT_NAME_HEADER_NAME, CODE_QUARKUS_IO_CLIENT_NAME_HEADER_VALUE);
            connection.setRequestProperty(CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_NAME, CODE_QUARKUS_IO_CLIENT_CONTACT_EMAIL_HEADER_VALUE);
        });
        File moduleFile = new File(getContentEntryPath());
        try {
            ApplicationManager.getApplication().executeOnPooledThread(() -> builder.connect(request -> {
                ZipUtil.unpack(request.getInputStream(), moduleFile, name -> {
                    int index = name.indexOf('/');
                    return name.substring(index);
                });
                return true;
            })).get();
        } catch (InterruptedException|ExecutionException e) {
            throw new IOException(e);
        }
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(moduleFile);
        RefreshQueue.getInstance().refresh(true, true, (Runnable)null, new VirtualFile[]{vf});
    }

    @Override
    public ModuleWizardStep[] createFinishingSteps(@NotNull WizardContext wizardContext, @NotNull ModulesProvider modulesProvider) {
        this.wizardContext = wizardContext;
        return super.createFinishingSteps(wizardContext, modulesProvider);
    }
}
