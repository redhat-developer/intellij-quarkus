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
package com.redhat.devtools.intellij.quarkus.lsp;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.quarkus.QuarkusProjectService;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageClientImpl;
import com.redhat.devtools.intellij.quarkus.search.ProjectLabelManager;
import com.redhat.devtools.intellij.quarkus.search.PropertiesManager;
import com.redhat.devtools.intellij.quarkus.search.PsiUtilsImpl;
import com.redhat.devtools.intellij.quarkus.search.core.PropertiesManagerForJava;
import com.redhat.microprofile.commons.MicroProfileJavaDiagnosticsParams;
import com.redhat.microprofile.commons.MicroProfileJavaHoverParams;
import com.redhat.microprofile.commons.MicroProfileJavaProjectLabelsParams;
import com.redhat.microprofile.commons.MicroProfileProjectInfo;
import com.redhat.microprofile.commons.MicroProfileProjectInfoParams;
import com.redhat.microprofile.commons.MicroProfilePropertiesChangeEvent;
import com.redhat.microprofile.commons.MicroProfilePropertiesScope;
import com.redhat.microprofile.commons.MicroProfilePropertyDefinitionParams;
import com.redhat.microprofile.commons.ProjectLabelInfoEntry;
import com.redhat.microprofile.ls.api.MicroProfileLanguageClientAPI;
import com.redhat.microprofile.ls.api.MicroProfileLanguageServerAPI;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
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
    MicroProfilePropertiesChangeEvent event = new MicroProfilePropertiesChangeEvent();
    event.setType(Collections.singletonList(scope));
    event.setProjectURIs(uris);
    ((MicroProfileLanguageServerAPI)getLanguageServer()).propertiesChanged(event);
  }

  @Override
  public void libraryUpdated(Library library) {
    sendPropertiesChangeEvent(MicroProfilePropertiesScope.dependencies, QuarkusModuleUtil.getModulesURIs(getProject()));
  }

  @Override
  public void sourceUpdated(List<Pair<Module, VirtualFile>> sources) {
    Set<String> uris = sources.stream().map(pair -> pair.getLeft()).
            map(module -> PsiUtilsImpl.getProjectURI(module)).
            collect(Collectors.toSet());
    if (!uris.isEmpty()) {
      sendPropertiesChangeEvent(MicroProfilePropertiesScope.sources, uris);
    }
  }

  <R> CompletableFuture<R> runAsBackground(String title, Supplier<R> supplier) {
    CompletableFuture<R> future = new CompletableFuture<>();
    ProgressManager.getInstance().run(new Task.Backgroundable(getProject(), title) {
      @Override
      public void run(ProgressIndicator indicator) {
        try {
          future.complete(supplier.get());
        } catch (Throwable t) {
          future.completeExceptionally(t);
        }
      }
    });
    return future;
  }

  @Override
  public CompletableFuture<MicroProfileProjectInfo> getProjectInfo(MicroProfileProjectInfoParams params) {
    return runAsBackground("Computing project information", () -> PropertiesManager.getInstance().getMicroProfileProjectInfo(params, PsiUtilsImpl.getInstance()));
  }

  @Override
  public CompletableFuture<Hover> getJavaHover(MicroProfileJavaHoverParams javaParams) {
    return runAsBackground("Computing Java hover", () -> PropertiesManagerForJava.getInstance().hover(javaParams, PsiUtilsImpl.getInstance()));
  }

  @Override
  public CompletableFuture<List<PublishDiagnosticsParams>> getJavaDiagnostics(MicroProfileJavaDiagnosticsParams javaParams) {
    return runAsBackground("Computing Java diagnostics", () -> PropertiesManagerForJava.getInstance().diagnostics(javaParams, PsiUtilsImpl.getInstance()));
  }

  @Override
  public CompletableFuture<Location> getPropertyDefinition(MicroProfilePropertyDefinitionParams params) {
    return runAsBackground("Computing property definition", () -> PropertiesManager.getInstance().findPropertyLocation(params, PsiUtilsImpl.getInstance()));
  }

  @Override
  public CompletableFuture<ProjectLabelInfoEntry> getJavaProjectlabels(MicroProfileJavaProjectLabelsParams javaParams) {
    return runAsBackground("Computing Java projects labels", () -> ProjectLabelManager.getInstance().getProjectLabelInfo(javaParams, PsiUtilsImpl.getInstance()));
  }
}
