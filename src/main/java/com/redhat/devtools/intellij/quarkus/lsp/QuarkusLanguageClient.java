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
package com.redhat.devtools.intellij.quarkus.lsp;

import com.intellij.codeInspection.InspectionProfile;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.profile.ProfileChangeAdapter;
import com.intellij.util.messages.MessageBusConnection;
import com.redhat.devtools.intellij.lsp4mp4ij.classpath.ClasspathResourceChangedManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.ProjectLabelManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManagerForJava;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.settings.MicroProfileInspectionsInfo;
import com.redhat.devtools.intellij.lsp4mp4ij.settings.UserDefinedMicroProfileSettings;
import com.redhat.devtools.intellij.quarkus.QuarkusDeploymentSupport;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.quarkus.QuarkusPluginDisposable;
import com.redhat.devtools.lsp4ij.JSONUtils;
import com.redhat.devtools.lsp4ij.client.CoalesceByKey;
import com.redhat.devtools.lsp4ij.client.IndexAwareLanguageClient;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4mp.commons.*;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;
import org.eclipse.lsp4mp.ls.api.MicroProfileLanguageClientAPI;
import org.eclipse.lsp4mp.ls.api.MicroProfileLanguageServerAPI;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


public class QuarkusLanguageClient extends IndexAwareLanguageClient implements MicroProfileLanguageClientAPI, ClasspathResourceChangedManager.Listener, ProfileChangeAdapter {

    private final MessageBusConnection connection;

    private MicroProfileInspectionsInfo inspectionsInfo;

    public QuarkusLanguageClient(Project project) {
        super(project);
        // Call Quarkus deployment support here to react on library changed (to evict quarkus deployment cache) before
        // sending an LSP microprofile/propertiesChanged notifications
        Disposer.register(QuarkusPluginDisposable.getInstance(project), this);
        QuarkusDeploymentSupport.getInstance(project);
        connection = project.getMessageBus().connect(QuarkusPluginDisposable.getInstance(project));
        connection.subscribe(ClasspathResourceChangedManager.TOPIC, this);
        inspectionsInfo = MicroProfileInspectionsInfo.getMicroProfileInspectionInfo(project);
        connection.subscribe(ProfileChangeAdapter.TOPIC, this);
        // Track MicroProfile settings changed to push them to the language server with LSP didChangeConfiguration.
        UserDefinedMicroProfileSettings.getInstance(project).addChangeHandler(getDidChangeConfigurationListener());
    }

    @Override
    public void dispose() {
        super.dispose();
        connection.disconnect();
        UserDefinedMicroProfileSettings.getInstance(getProject()).removeChangeHandler(getDidChangeConfigurationListener());
    }

    @Override
    protected Object createSettings() {
        return UserDefinedMicroProfileSettings.getInstance(getProject()).toSettingsForMicroProfileLS();
    }

    private void sendPropertiesChangeEvent(List<MicroProfilePropertiesScope> scope, Set<String> uris) {
        MicroProfileLanguageServerAPI server = (MicroProfileLanguageServerAPI) getLanguageServer();
        if (server != null) {
            MicroProfilePropertiesChangeEvent event = new MicroProfilePropertiesChangeEvent();
            event.setType(scope);
            event.setProjectURIs(uris);
            server.propertiesChanged(event);
        }
    }

    @Override
    public void profileChanged(@NotNull InspectionProfile profile) {
        // Track MicroProfile inspections settings (declared in Editor/Inspection/MicroProfile UI settings) changed,
        // convert them to matching LSP4MP configuration and push them via 'workspace/didChangeConfiguration'.
        MicroProfileInspectionsInfo newInspectionState = MicroProfileInspectionsInfo.getMicroProfileInspectionInfo(getProject());
        if (!Objects.equals(newInspectionState, inspectionsInfo)) {
            inspectionsInfo = newInspectionState;
            ApplicationManager.getApplication().invokeLater(() -> {
                new Task.Backgroundable(getProject(), "Updating LSP4MP configuration...", true) {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        triggerChangeConfiguration();
                    }
                }.queue();
            }, ModalityState.defaultModalityState(), getProject().getDisposed());
        }
    }

    @Override
    public void librariesChanged() {
        if (isDisposed()) {
            // The language client has been disposed, ignore changes in libraries
            return;
        }
        sendPropertiesChangeEvent(Collections.singletonList(MicroProfilePropertiesScope.dependencies), QuarkusModuleUtil.getModulesURIs(getProject()));
    }

    @Override
    public void sourceFilesChanged(Set<Pair<VirtualFile, Module>> sources) {
        if (isDisposed()) {
            // The language client has been disposed, ignore changes in Java source / microprofile-config.properties files
            return;
        }
        List<Pair<String, MicroProfilePropertiesScope>> info = sources.stream()
                .filter(pair -> isJavaFile(pair.getFirst()) || isConfigSource(pair.getFirst()))
                .map(pair -> Pair.pair(PsiUtilsLSImpl.getProjectURI(pair.getSecond()), getScope(pair.getFirst()))).
                collect(Collectors.toList());
        if (!info.isEmpty()) {
            sendPropertiesChangeEvent(info.stream().map(p -> p.getSecond()).collect(Collectors.toList()),
                    info.stream().map(p -> p.getFirst()).collect(Collectors.toSet()));
        }
    }

    private MicroProfilePropertiesScope getScope(VirtualFile file) {
        return isJavaFile(file) ? MicroProfilePropertiesScope.sources : MicroProfilePropertiesScope.configfiles;
    }

    private boolean isJavaFile(VirtualFile file) {
        return PsiMicroProfileProjectManager.isJavaFile(file);
    }

    private boolean isConfigSource(VirtualFile file) {
        return PsiMicroProfileProjectManager.isConfigSource(file);
    }

    @Override
    public CompletableFuture<MicroProfileProjectInfo> getProjectInfo(MicroProfileProjectInfoParams params) {
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
        VirtualFile file = null;
        try {
            file = utils.findFile(params.getUri());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Module module = utils.getModule(file);
        if (module == null) {
            throw new RuntimeException();
        }
        CompletableFuture<Void> quarkusDeploymentSupport = QuarkusDeploymentSupport.getInstance(getProject()).updateClasspathWithQuarkusDeploymentAsync(module);
        if (quarkusDeploymentSupport.isDone()) {
            return internalGetProjectInfo(params);
        }
        return quarkusDeploymentSupport
                .thenCompose(unused -> internalGetProjectInfo(params));
    }

    private CompletableFuture<MicroProfileProjectInfo> internalGetProjectInfo(MicroProfileProjectInfoParams params) {
        var coalesceBy = new CoalesceByKey("microprofile/projectInfo", params.getUri(), params.getScopes());
        String filePath = getFilePath(params.getUri());
        return runAsBackground("Computing MicroProfile properties for '" + filePath + "'.", monitor ->
                        PropertiesManager.getInstance().getMicroProfileProjectInfo(params, PsiUtilsLSImpl.getInstance(getProject()), monitor)
                , coalesceBy);
    }

    @Override
    public CompletableFuture<Hover> getJavaHover(MicroProfileJavaHoverParams javaParams) {
        var coalesceBy = new CoalesceByKey("microprofile/java/hover", javaParams.getUri(), javaParams.getPosition());
        return runAsBackground("Computing MicroProfile Java hover", monitor -> PropertiesManagerForJava.getInstance().hover(javaParams, PsiUtilsLSImpl.getInstance(getProject())), coalesceBy);
    }

    @Override
    public CompletableFuture<List<PublishDiagnosticsParams>> getJavaDiagnostics(MicroProfileJavaDiagnosticsParams javaParams) {
        // When project is indexing and user types a lot of characters in the Java editor, the MicroProfile language server
        // validates the Java document and consumes a 'microprofile/java/diagnostics' for each typed character.
        // The response of 'microprofile/java/diagnostics' are blocked (or takes some times) and we have
        // "Too many non-blocking read actions submitted at once in". To avoid having this error, we create a coalesceBy key
        // managed by IJ ReadAction.nonBlocking() to cancel the previous request.
        var coalesceBy = new CoalesceByKey("microprofile/java/diagnostics", javaParams.getUris());
        return runAsBackground("Computing MicroProfile Java diagnostics", monitor -> PropertiesManagerForJava.getInstance().diagnostics(javaParams, PsiUtilsLSImpl.getInstance(getProject())), coalesceBy);
    }

    @Override
    public CompletableFuture<Location> getPropertyDefinition(MicroProfilePropertyDefinitionParams params) {
        var coalesceBy = new CoalesceByKey("microprofile/propertyDefinition", params.getUri(), params.getSourceType(), params.getSourceField(), params.getSourceMethod());
        return runAsBackground("Computing property definition", monitor -> PropertiesManager.getInstance().findPropertyLocation(params, PsiUtilsLSImpl.getInstance(getProject())), coalesceBy);
    }

    @Override
    public CompletableFuture<ProjectLabelInfoEntry> getJavaProjectLabels(MicroProfileJavaProjectLabelsParams javaParams) {
        var coalesceBy = new CoalesceByKey("microprofile/java/projectLabels", javaParams.getUri(), javaParams.getTypes());
        return runAsBackground("Computing Java projects labels", monitor -> ProjectLabelManager.getInstance().getProjectLabelInfo(javaParams, PsiUtilsLSImpl.getInstance(getProject())), coalesceBy);
    }

    @Override
    public CompletableFuture<List<ProjectLabelInfoEntry>> getAllJavaProjectLabels() {
        var coalesceBy = new CoalesceByKey("microprofile/java/workspaceLabels");
        return runAsBackground("Computing All Java projects labels", monitor -> ProjectLabelManager.getInstance().getProjectLabelInfo(PsiUtilsLSImpl.getInstance(getProject())), coalesceBy);
    }

    @Override
    public CompletableFuture<JavaFileInfo> getJavaFileInfo(MicroProfileJavaFileInfoParams javaParams) {
        var coalesceBy = new CoalesceByKey("microprofile/java/fileInfo", javaParams.getUri());
        return runAsBackground("Computing Java file info", monitor -> PropertiesManagerForJava.getInstance().fileInfo(javaParams, PsiUtilsLSImpl.getInstance(getProject())), coalesceBy);
    }

    @Override
    public CompletableFuture<List<MicroProfileDefinition>> getJavaDefinition(MicroProfileJavaDefinitionParams javaParams) {
        var coalesceBy = new CoalesceByKey("microprofile/java/definition", javaParams.getUri(), javaParams.getPosition());
        return runAsBackground("Computing Java definitions", monitor -> PropertiesManagerForJava.getInstance().definition(javaParams, PsiUtilsLSImpl.getInstance(getProject())), coalesceBy);
    }

    @Override
    public CompletableFuture<MicroProfileJavaCompletionResult> getJavaCompletion(MicroProfileJavaCompletionParams javaParams) {
        var coalesceBy = new CoalesceByKey("microprofile/java/completion", javaParams.getUri(), javaParams.getPosition());
        return runAsBackground("Computing Java completion", monitor -> {
            IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
            CompletionList completionList = PropertiesManagerForJava.getInstance().completion(javaParams, utils);
            JavaCursorContextResult cursorContext = PropertiesManagerForJava.getInstance().javaCursorContext(javaParams, utils);
            return new MicroProfileJavaCompletionResult(completionList, cursorContext);
        }, coalesceBy);
    }

    @Override
    public CompletableFuture<List<? extends CodeLens>> getJavaCodelens(MicroProfileJavaCodeLensParams javaParams) {
        var coalesceBy = new CoalesceByKey("microprofile/java/codeLens", javaParams.getUri());
        return runAsBackground("Computing Java codelens", monitor -> PropertiesManagerForJava.getInstance().codeLens(javaParams, PsiUtilsLSImpl.getInstance(getProject()), monitor), coalesceBy);
    }

    @Override
    public CompletableFuture<List<CodeAction>> getJavaCodeAction(MicroProfileJavaCodeActionParams javaParams) {
        var coalesceBy = new CoalesceByKey("microprofile/java/codeAction", javaParams.getUri());
        return runAsBackground("Computing Java code actions", monitor -> (List<CodeAction>) PropertiesManagerForJava.getInstance().codeAction(javaParams, PsiUtilsLSImpl.getInstance(getProject())), coalesceBy);
    }

    @Override
    public CompletableFuture<CodeAction> resolveCodeAction(CodeAction unresolved) {
        var coalesceBy = new CoalesceByKey("microprofile/java/resolveCodeAction");
        return runAsBackground("Computing Java resolve code actions", monitor -> {
            CodeActionResolveData data = JSONUtils.toModel(unresolved.getData(), CodeActionResolveData.class);
            unresolved.setData(data);
            return (CodeAction) PropertiesManagerForJava.getInstance().resolveCodeAction(unresolved, PsiUtilsLSImpl.getInstance(getProject()));
        }, coalesceBy);
    }

    @Override
    public CompletableFuture<JavaCursorContextResult> getJavaCursorContext(MicroProfileJavaCompletionParams params) {
        var coalesceBy = new CoalesceByKey("microprofile/java/javaCursorContext", params.getUri(), params.getPosition());
        return runAsBackground("Computing Java Cursor context", monitor -> PropertiesManagerForJava.getInstance().javaCursorContext(params, PsiUtilsLSImpl.getInstance(getProject())), coalesceBy);
    }

    @Override
    public CompletableFuture<List<SymbolInformation>> getJavaWorkspaceSymbols(String projectUri) {
        //Workspace symbols not supported yet https://github.com/redhat-developer/intellij-quarkus/issues/808
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<String> getPropertyDocumentation(MicroProfilePropertyDocumentationParams params) {
        // Requires porting https://github.com/eclipse/lsp4mp/issues/321 / https://github.com/eclipse/lsp4mp/pull/329
        return CompletableFuture.completedFuture(null);
    }
}
