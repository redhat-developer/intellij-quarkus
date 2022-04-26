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
package com.redhat.devtools.intellij.qute.lsp;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.quarkus.QuarkusProjectService;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageClientImpl;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForJava;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForTemplate;
import com.redhat.qute.commons.JavaTypeInfo;
import com.redhat.qute.commons.ProjectInfo;
import com.redhat.qute.commons.QuteJavaCodeLensParams;
import com.redhat.qute.commons.QuteJavaDefinitionParams;
import com.redhat.qute.commons.QuteJavaDiagnosticsParams;
import com.redhat.qute.commons.QuteJavaDocumentLinkParams;
import com.redhat.qute.commons.QuteJavaTypesParams;
import com.redhat.qute.commons.QuteProjectParams;
import com.redhat.qute.commons.QuteResolvedJavaTypeParams;
import com.redhat.qute.commons.ResolvedJavaTypeInfo;
import com.redhat.qute.commons.datamodel.DataModelParameter;
import com.redhat.qute.commons.datamodel.DataModelProject;
import com.redhat.qute.commons.datamodel.DataModelTemplate;
import com.redhat.qute.commons.datamodel.JavaDataModelChangeEvent;
import com.redhat.qute.commons.datamodel.QuteDataModelProjectParams;
import com.redhat.qute.commons.usertags.QuteUserTagParams;
import com.redhat.qute.commons.usertags.UserTagInfo;
import com.redhat.qute.ls.api.QuteLanguageClientAPI;
import com.redhat.qute.ls.api.QuteLanguageServerAPI;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class QuteLanguageClient extends LanguageClientImpl implements QuteLanguageClientAPI, QuarkusProjectService.Listener {
  private static final Logger LOGGER = LoggerFactory.getLogger(QuteLanguageClient.class);

  private final MessageBusConnection connection;

  public QuteLanguageClient(Project project) {
    super(project);
    connection = project.getMessageBus().connect(project);
    connection.subscribe(QuarkusProjectService.TOPIC, this);
    QuarkusProjectService.getInstance(project);
  }

  private void sendPropertiesChangeEvent(Set<String> uris) {
    QuteLanguageServerAPI server = (QuteLanguageServerAPI) getLanguageServer();
    if (server != null) {
      JavaDataModelChangeEvent event = new JavaDataModelChangeEvent();
      event.setProjectURIs(uris);
      server.dataModelChanged(event);
    }
  }

  @Override
  public void libraryUpdated(Library library) {
    sendPropertiesChangeEvent(QuarkusModuleUtil.getModulesURIs(getProject()));
  }

  @Override
  public void sourceUpdated(List<Pair<Module, VirtualFile>> sources) {
    Set<String> uris = sources.stream().map(pair -> pair.getLeft()).
            map(module -> PsiUtilsLSImpl.getProjectURI(module)).
            collect(Collectors.toSet());
    if (!uris.isEmpty()) {
      sendPropertiesChangeEvent(uris);
    }
  }

  <R> CompletableFuture<R> runAsBackground(String title, Function<ProgressIndicator, R> function) {
    CompletableFuture<R> future = new CompletableFuture<>();
    CompletableFuture.runAsync(() -> {
      Runnable task = () -> ProgressManager.getInstance().run(new Task.Backgroundable(getProject(), title) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          runTask(indicator, future, function);
        }

        @Override
        public boolean isHeadless() {
          return true;
        }
      });
      if (DumbService.getInstance(getProject()).isDumb()) {
        DumbService.getInstance(getProject()).runWhenSmart(task);
      } else {
        task.run();
      }
    });
    return future;
  }

  private <R> void runTask(@NotNull ProgressIndicator indicator, CompletableFuture<R> future, Function<ProgressIndicator, R> function) {
    boolean done = false;
    for(int i=0; !done && i < 10;++i) {
      try {
        future.complete(function.apply(indicator));
        done = true;
      } catch (IndexNotReadyException e) {
      } catch (Throwable t) {
        future.completeExceptionally(t);
      }
    }
    if (!done) {
      DumbService.getInstance(getProject()).runWhenSmart(() -> runTask(indicator, future, function));
    }
  }

  @Override
  public CompletableFuture<ProjectInfo> getProjectInfo(QuteProjectParams params) {
    return runAsBackground("getProjectInfo", monitor -> QuteSupportForTemplate.getInstance().getProjectInfo(params, PsiUtilsLSImpl.getInstance(getProject()), monitor));
  }

  @Override
  public CompletableFuture<DataModelProject<DataModelTemplate<DataModelParameter>>> getDataModelProject(
          QuteDataModelProjectParams params) {
    return runAsBackground("getDataModel", monitor -> ReadAction.compute(() -> QuteSupportForTemplate.getInstance().getDataModelProject(params, PsiUtilsLSImpl.getInstance(getProject()),
            monitor)));
  }

  @Override
  public CompletableFuture<List<JavaTypeInfo>> getJavaTypes(QuteJavaTypesParams params) {
    return runAsBackground("getJavaTypes", monitor -> QuteSupportForTemplate.getInstance().getJavaTypes(params, PsiUtilsLSImpl.getInstance(getProject()),
            monitor));
  }

  @Override
  public CompletableFuture<ResolvedJavaTypeInfo> getResolvedJavaType(QuteResolvedJavaTypeParams params) {
    return runAsBackground("getResolvedJavaType", monitor -> ReadAction.compute(() -> QuteSupportForTemplate.getInstance().getResolvedJavaType(params, PsiUtilsLSImpl.getInstance(getProject()),
            monitor)));
  }

  @Override
  public CompletableFuture<Location> getJavaDefinition(QuteJavaDefinitionParams params) {
    return runAsBackground("getJavaDefinition", monitor -> ReadAction.compute(() -> QuteSupportForTemplate.getInstance().getJavaDefinition(params, PsiUtilsLSImpl.getInstance(getProject()),
            monitor)));
  }

  @Override
  public CompletableFuture<List<? extends CodeLens>> getJavaCodelens(QuteJavaCodeLensParams javaParams) {
    return runAsBackground("getJavaCodelens", monitor -> ReadAction.compute(() -> QuteSupportForJava.getInstance().codeLens(javaParams, PsiUtilsLSImpl.getInstance(getProject()),
            monitor)));
  }

  @Override
  public CompletableFuture<List<PublishDiagnosticsParams>> getJavaDiagnostics(QuteJavaDiagnosticsParams javaParams) {
    return runAsBackground("getJavaDiagnostics", monitor -> ReadAction.compute(() -> QuteSupportForJava.getInstance().diagnostics(javaParams, PsiUtilsLSImpl.getInstance(getProject()),
            monitor)));
  }

  @Override
  public CompletableFuture<List<DocumentLink>> getJavaDocumentLink(QuteJavaDocumentLinkParams javaParams) {
    return runAsBackground("getJavaDocumentLink", monitor -> QuteSupportForJava.getInstance().documentLink(javaParams, PsiUtilsLSImpl.getInstance(getProject()),
            monitor));
  }

  @Override
  public CompletableFuture<List<UserTagInfo>> getUserTags(QuteUserTagParams params) {
    return runAsBackground("getUserTags", monitor -> QuteSupportForTemplate.getInstance().getUserTags(params, PsiUtilsLSImpl.getInstance(getProject()),
            monitor));
  }
}
