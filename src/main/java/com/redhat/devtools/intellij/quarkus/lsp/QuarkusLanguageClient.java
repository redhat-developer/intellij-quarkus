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
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.quarkus.QuarkusProjectService;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageClientImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.ProjectLabelManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.PropertiesManagerForJava;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4mp.commons.JavaFileInfo;
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

  private final MessageBusConnection connection;

  public QuarkusLanguageClient(Project project) {
    super(project);
    connection = project.getMessageBus().connect(project);
    connection.subscribe(QuarkusProjectService.TOPIC, this);
    QuarkusProjectService.getInstance(project);
  }

  private void sendPropertiesChangeEvent(MicroProfilePropertiesScope scope, Set<String> uris) {
    MicroProfileLanguageServerAPI server = (MicroProfileLanguageServerAPI) getLanguageServer();
    if (server != null) {
      MicroProfilePropertiesChangeEvent event = new MicroProfilePropertiesChangeEvent();
      event.setType(Collections.singletonList(scope));
      event.setProjectURIs(uris);
      server.propertiesChanged(event);
    }
  }

  @Override
  public void libraryUpdated(Library library) {
    sendPropertiesChangeEvent(MicroProfilePropertiesScope.dependencies, QuarkusModuleUtil.getModulesURIs(getProject()));
  }

  @Override
  public void sourceUpdated(List<Pair<Module, VirtualFile>> sources) {
    Set<String> uris = sources.stream().map(pair -> pair.getLeft()).
            map(module -> PsiUtilsLSImpl.getProjectURI(module)).
            collect(Collectors.toSet());
    if (!uris.isEmpty()) {
      sendPropertiesChangeEvent(MicroProfilePropertiesScope.sources, uris);
    }
  }

  <R> CompletableFuture<R> runAsBackground(String title, Supplier<R> supplier) {
    CompletableFuture<R> future = new CompletableFuture<>();
    CompletableFuture.runAsync(() -> {
      Runnable task = () -> {
        try {
          future.complete(supplier.get());
        } catch (Throwable t) {
          future.completeExceptionally(t);
        }
      };
      if (DumbService.getInstance(getProject()).isDumb()) {
        DumbService.getInstance(getProject()).runWhenSmart(task);
      } else if (ProgressManager.getInstance().hasModalProgressIndicator()) {
        task.run();
      } else {
        ProgressManager.getInstance().run(new Task.Backgroundable(getProject(), title) {
          @Override
          public void run(ProgressIndicator indicator) {
            task.run();
          }
        });
      }
    });
    return future;
  }

  @Override
  public CompletableFuture<MicroProfileProjectInfo> getProjectInfo(MicroProfileProjectInfoParams params) {
    return runAsBackground("Computing project information", () -> PropertiesManager.getInstance().getMicroProfileProjectInfo(params, PsiUtilsLSImpl.getInstance()));
  }

  @Override
  public CompletableFuture<Hover> getJavaHover(MicroProfileJavaHoverParams javaParams) {
    return runAsBackground("Computing Java hover", () -> PropertiesManagerForJava.getInstance().hover(javaParams, PsiUtilsLSImpl.getInstance()));
  }

  @Override
  public CompletableFuture<List<PublishDiagnosticsParams>> getJavaDiagnostics(MicroProfileJavaDiagnosticsParams javaParams) {
    return runAsBackground("Computing Java diagnostics", () -> PropertiesManagerForJava.getInstance().diagnostics(javaParams, PsiUtilsLSImpl.getInstance()));
  }

  @Override
  public CompletableFuture<Location> getPropertyDefinition(MicroProfilePropertyDefinitionParams params) {
    return runAsBackground("Computing property definition", () -> PropertiesManager.getInstance().findPropertyLocation(params, PsiUtilsLSImpl.getInstance()));
  }

  @Override
  public CompletableFuture<ProjectLabelInfoEntry> getJavaProjectlabels(MicroProfileJavaProjectLabelsParams javaParams) {
    return runAsBackground("Computing Java projects labels", () -> ProjectLabelManager.getInstance().getProjectLabelInfo(javaParams, PsiUtilsLSImpl.getInstance()));
  }

  @Override
  public CompletableFuture<JavaFileInfo> getJavaFileInfo(MicroProfileJavaFileInfoParams javaParams) {
    return runAsBackground("Computing Java file info", () -> PropertiesManagerForJava.getInstance().fileInfo(javaParams, PsiUtilsLSImpl.getInstance()));
  }

  @Override
  public CompletableFuture<CompletionList> getJavaCompletion(MicroProfileJavaCompletionParams javaParams) {
    return runAsBackground("Computing Java completion", () -> PropertiesManagerForJava.getInstance().completion(javaParams, PsiUtilsLSImpl.getInstance()));
  }

  @Override
  public CompletableFuture<List<? extends CodeLens>> getJavaCodelens(MicroProfileJavaCodeLensParams javaParams) {
    return runAsBackground("Computing Java codelens", () -> PropertiesManagerForJava.getInstance().codeLens(javaParams, PsiUtilsLSImpl.getInstance()));
  }
}
