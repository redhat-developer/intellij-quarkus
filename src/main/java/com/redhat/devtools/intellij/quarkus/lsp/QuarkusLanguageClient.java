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
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.ProjectLabelManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManagerForJava;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.IConfigSource;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.IConfigSourceProvider;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.quarkus.QuarkusProjectService;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageClientImpl;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4mp.commons.JavaFileInfo;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeLensParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCompletionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaFileInfoParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaHoverParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaProjectLabelsParams;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfoParams;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesChangeEvent;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.eclipse.lsp4mp.commons.MicroProfilePropertyDefinitionParams;
import org.eclipse.lsp4mp.commons.ProjectLabelInfoEntry;
import org.eclipse.lsp4mp.ls.api.MicroProfileLanguageClientAPI;
import org.eclipse.lsp4mp.ls.api.MicroProfileLanguageServerAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class QuarkusLanguageClient extends LanguageClientImpl implements MicroProfileLanguageClientAPI, QuarkusProjectService.Listener {
  private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusLanguageClient.class);
  private static final String JAVA_FILE_EXTENSION = "java";

  private final MessageBusConnection connection;

  public QuarkusLanguageClient(Project project) {
    super(project);
    connection = project.getMessageBus().connect(project);
    connection.subscribe(QuarkusProjectService.TOPIC, this);
    QuarkusProjectService.getInstance(project);
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
  public void libraryUpdated(Library library) {
    sendPropertiesChangeEvent(Collections.singletonList(MicroProfilePropertiesScope.dependencies), QuarkusModuleUtil.getModulesURIs(getProject()));
  }

  @Override
  public void sourceUpdated(List<Pair<Module, VirtualFile>> sources) {
    List<Pair<String,MicroProfilePropertiesScope>> info = sources.stream().
            filter(pair -> isJavaFile(pair.getRight()) || isConfigSource(pair.getRight(), pair.getLeft())).
            map(pair -> Pair.of(PsiUtilsLSImpl.getProjectURI(pair.getLeft()), getScope(pair.getRight()))).
            collect(Collectors.toList());
    if (!info.isEmpty()) {
      sendPropertiesChangeEvent(info.stream().map(Pair::getRight).collect(Collectors.toList()), info.stream().map(Pair::getLeft).collect(Collectors.toSet()));
    }
  }

  private MicroProfilePropertiesScope getScope(VirtualFile file) {
    return isJavaFile(file)?MicroProfilePropertiesScope.sources:MicroProfilePropertiesScope.configfiles;
  }

  private boolean isJavaFile(VirtualFile file) {
    return JAVA_FILE_EXTENSION.equals(file.getExtension());
  }

  private boolean isConfigSource(VirtualFile file, Module project) {
    return PsiMicroProfileProjectManager.getInstance(project.getProject()).isConfigSource(file);
  }


  <R> CompletableFuture<R> runAsBackground(String title, Supplier<R> supplier) {
    CompletableFuture<R> future = new CompletableFuture<>();
    CompletableFuture.runAsync(() -> {
      Runnable task = () -> ProgressManager.getInstance().runProcess(() -> {
        try {
          future.complete(supplier.get());
        } catch (Throwable t) {
          future.completeExceptionally(t);
        }
      }, new EmptyProgressIndicator());
      if (DumbService.getInstance(getProject()).isDumb()) {
        DumbService.getInstance(getProject()).runWhenSmart(task);
      } else {
        task.run();
      }
    });
    return future;
  }

  @Override
  public CompletableFuture<MicroProfileProjectInfo> getProjectInfo(MicroProfileProjectInfoParams params) {
    return runAsBackground("Computing project information", () -> PropertiesManager.getInstance().getMicroProfileProjectInfo(params, PsiUtilsLSImpl.getInstance(getProject())));
  }

  @Override
  public CompletableFuture<Hover> getJavaHover(MicroProfileJavaHoverParams javaParams) {
    return runAsBackground("Computing Java hover", () -> PropertiesManagerForJava.getInstance().hover(javaParams, PsiUtilsLSImpl.getInstance(getProject())));
  }

  @Override
  public CompletableFuture<List<PublishDiagnosticsParams>> getJavaDiagnostics(MicroProfileJavaDiagnosticsParams javaParams) {
    return runAsBackground("Computing Java diagnostics", () -> PropertiesManagerForJava.getInstance().diagnostics(javaParams, PsiUtilsLSImpl.getInstance(getProject())));
  }

  @Override
  public CompletableFuture<Location> getPropertyDefinition(MicroProfilePropertyDefinitionParams params) {
    return runAsBackground("Computing property definition", () -> PropertiesManager.getInstance().findPropertyLocation(params, PsiUtilsLSImpl.getInstance(getProject())));
  }

  @Override
  public CompletableFuture<ProjectLabelInfoEntry> getJavaProjectlabels(MicroProfileJavaProjectLabelsParams javaParams) {
    return runAsBackground("Computing Java projects labels", () -> ProjectLabelManager.getInstance().getProjectLabelInfo(javaParams, PsiUtilsLSImpl.getInstance(getProject())));
  }

  @Override
  public CompletableFuture<JavaFileInfo> getJavaFileInfo(MicroProfileJavaFileInfoParams javaParams) {
    return runAsBackground("Computing Java file info", () -> PropertiesManagerForJava.getInstance().fileInfo(javaParams, PsiUtilsLSImpl.getInstance(getProject())));
  }

  @Override
  public CompletableFuture<CompletionList> getJavaCompletion(MicroProfileJavaCompletionParams javaParams) {
    return runAsBackground("Computing Java completion", () -> PropertiesManagerForJava.getInstance().completion(javaParams, PsiUtilsLSImpl.getInstance(getProject())));
  }

  @Override
  public CompletableFuture<List<? extends CodeLens>> getJavaCodelens(MicroProfileJavaCodeLensParams javaParams) {
    return runAsBackground("Computing Java codelens", () -> PropertiesManagerForJava.getInstance().codeLens(javaParams, PsiUtilsLSImpl.getInstance(getProject())));
  }

  @Override
  public CompletableFuture<List<CodeAction>> getJavaCodeAction(MicroProfileJavaCodeActionParams javaParams) {
    return runAsBackground("Computing Java code actions", () -> (List<CodeAction>) PropertiesManagerForJava.getInstance().codeAction(javaParams, PsiUtilsLSImpl.getInstance(getProject())));
  }
}
