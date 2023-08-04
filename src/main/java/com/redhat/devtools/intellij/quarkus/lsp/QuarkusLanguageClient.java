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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.redhat.devtools.intellij.lsp4ij.client.IndexAwareLanguageClient;
import com.redhat.devtools.intellij.lsp4mp4ij.classpath.ClasspathResourceChangedManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.ProjectLabelManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManagerForJava;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.settings.UserDefinedMicroProfileSettings;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4mp.commons.*;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;
import org.eclipse.lsp4mp.commons.utils.JSONUtility;
import org.eclipse.lsp4mp.ls.api.MicroProfileLanguageClientAPI;
import org.eclipse.lsp4mp.ls.api.MicroProfileLanguageServerAPI;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


public class QuarkusLanguageClient extends IndexAwareLanguageClient implements MicroProfileLanguageClientAPI, ClasspathResourceChangedManager.Listener {

    private final MessageBusConnection connection;

    public QuarkusLanguageClient(Project project) {
        super(project);
        connection = project.getMessageBus().connect(project);
        connection.subscribe(ClasspathResourceChangedManager.TOPIC, this);
        // Track MicroProfile settings changed to push them to the language server with LSP didChangeConfiguration.
        UserDefinedMicroProfileSettings.getInstance().addChangeHandler(getDidChangeConfigurationListener());
    }

    @Override
    public void dispose() {
        super.dispose();
        connection.disconnect();
        UserDefinedMicroProfileSettings.getInstance().removeChangeHandler(getDidChangeConfigurationListener());
    }

    @Override
    protected Object createSettings() {
        return UserDefinedMicroProfileSettings.getInstance().toSettingsForMicroProfileLS();
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
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            Runnable task = () -> ProgressManager.getInstance().run(new Task.Backgroundable(getProject(), "Computing deployment jars...") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    System.out.println("Computing deployment jars...");
                    IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
                    try {
                        VirtualFile file = utils.findFile(params.getUri());
                        Module module = utils.getModule(file);
                        long start = System.currentTimeMillis();
                        QuarkusModuleUtil.ensureQuarkusLibrary(module, indicator);
                        long elapsed = System.currentTimeMillis() - start;
                        System.out.println("Ensured QuarkusLibrary in "+ elapsed+ " ms");
                        future.complete(true);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                }
            });
            if (DumbService.getInstance(getProject()).isDumb()) {
                DumbService.getInstance(getProject()).runWhenSmart(task);
            } else {
                task.run();
            }
        });
        return future.thenCompose(Boolean -> {
            String filePath = getFilePath(params.getUri());
            return runAsBackground("Computing MicroProfile properties for '" + filePath + "'.", monitor ->
                    PropertiesManager.getInstance().getMicroProfileProjectInfo(params, PsiUtilsLSImpl.getInstance(getProject()), monitor)
            );
        });
    }

    @Override
    public CompletableFuture<Hover> getJavaHover(MicroProfileJavaHoverParams javaParams) {
        return runAsBackground("Computing MicroProfile Java hover", monitor -> PropertiesManagerForJava.getInstance().hover(javaParams, PsiUtilsLSImpl.getInstance(getProject())));
    }

    @Override
    public CompletableFuture<List<PublishDiagnosticsParams>> getJavaDiagnostics(MicroProfileJavaDiagnosticsParams javaParams) {
        return runAsBackground("Computing MicroProfile Java diagnostics", monitor -> PropertiesManagerForJava.getInstance().diagnostics(javaParams, PsiUtilsLSImpl.getInstance(getProject())));
    }

    @Override
    public CompletableFuture<Location> getPropertyDefinition(MicroProfilePropertyDefinitionParams params) {
        return runAsBackground("Computing property definition", monitor -> PropertiesManager.getInstance().findPropertyLocation(params, PsiUtilsLSImpl.getInstance(getProject())));
    }

    @Override
    public CompletableFuture<ProjectLabelInfoEntry> getJavaProjectLabels(MicroProfileJavaProjectLabelsParams javaParams) {
        return runAsBackground("Computing Java projects labels", monitor -> ProjectLabelManager.getInstance().getProjectLabelInfo(javaParams, PsiUtilsLSImpl.getInstance(getProject())));
    }

    @Override
    public CompletableFuture<List<ProjectLabelInfoEntry>> getAllJavaProjectLabels() {
        return runAsBackground("Computing All Java projects labels", monitor -> ProjectLabelManager.getInstance().getProjectLabelInfo(PsiUtilsLSImpl.getInstance(getProject())));
    }

    @Override
    public CompletableFuture<JavaFileInfo> getJavaFileInfo(MicroProfileJavaFileInfoParams javaParams) {
        return runAsBackground("Computing Java file info", monitor -> PropertiesManagerForJava.getInstance().fileInfo(javaParams, PsiUtilsLSImpl.getInstance(getProject())));
    }

    @Override
    public CompletableFuture<List<MicroProfileDefinition>> getJavaDefinition(MicroProfileJavaDefinitionParams javaParams) {
        return runAsBackground("Computing Java definitions", monitor -> PropertiesManagerForJava.getInstance().definition(javaParams, PsiUtilsLSImpl.getInstance(getProject())));
    }

    @Override
    public CompletableFuture<MicroProfileJavaCompletionResult> getJavaCompletion(MicroProfileJavaCompletionParams javaParams) {
        return runAsBackground("Computing Java completion", monitor -> {
            IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
            CompletionList completionList = PropertiesManagerForJava.getInstance().completion(javaParams, utils);
            JavaCursorContextResult cursorContext = PropertiesManagerForJava.getInstance().javaCursorContext(javaParams, utils);
            return new MicroProfileJavaCompletionResult(completionList, cursorContext);
        });
    }

    @Override
    public CompletableFuture<List<? extends CodeLens>> getJavaCodelens(MicroProfileJavaCodeLensParams javaParams) {
        return runAsBackground("Computing Java codelens", monitor -> PropertiesManagerForJava.getInstance().codeLens(javaParams, PsiUtilsLSImpl.getInstance(getProject()), monitor));
    }

    @Override
    public CompletableFuture<List<CodeAction>> getJavaCodeAction(MicroProfileJavaCodeActionParams javaParams) {
        return runAsBackground("Computing Java code actions", monitor -> (List<CodeAction>) PropertiesManagerForJava.getInstance().codeAction(javaParams, PsiUtilsLSImpl.getInstance(getProject())));
    }

    @Override
    public CompletableFuture<CodeAction> resolveCodeAction(CodeAction unresolved) {
        return runAsBackground("Computing Java resolve code actions", monitor -> {
            CodeActionResolveData data = JSONUtility.toModel(unresolved.getData(), CodeActionResolveData.class);
            unresolved.setData(data);
            return (CodeAction) PropertiesManagerForJava.getInstance().resolveCodeAction(unresolved, PsiUtilsLSImpl.getInstance(getProject()));
        });
    }

    @Override
    public CompletableFuture<JavaCursorContextResult> getJavaCursorContext(MicroProfileJavaCompletionParams params) {
        return runAsBackground("Computing Java Cursor context", monitor -> PropertiesManagerForJava.getInstance().javaCursorContext(params, PsiUtilsLSImpl.getInstance(getProject())));
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
