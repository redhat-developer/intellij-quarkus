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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.quarkus.QuarkusProjectService;
import com.redhat.devtools.intellij.quarkus.lsp4ij.IndexAwareLanguageClient;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForJava;
import com.redhat.devtools.intellij.qute.psi.QuteSupportForTemplate;
import com.redhat.devtools.intellij.qute.psi.utils.PsiQuteProjectUtils;
import com.redhat.qute.commons.GenerateMissingJavaMemberParams;
import com.redhat.qute.commons.JavaTypeInfo;
import com.redhat.qute.commons.ProjectInfo;
import com.redhat.qute.commons.QuteJavaCodeLensParams;
import com.redhat.qute.commons.QuteJavaDefinitionParams;
import com.redhat.qute.commons.QuteJavaDiagnosticsParams;
import com.redhat.qute.commons.QuteJavaDocumentLinkParams;
import com.redhat.qute.commons.QuteJavaTypesParams;
import com.redhat.qute.commons.QuteJavadocParams;
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
import org.eclipse.lsp4j.WorkspaceEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class QuteLanguageClient extends IndexAwareLanguageClient implements QuteLanguageClientAPI, QuarkusProjectService.Listener {
  private static final Logger LOGGER = LoggerFactory.getLogger(QuteLanguageClient.class);

  private final MessageBusConnection connection;

  public QuteLanguageClient(Project project) {
    super(project);
    connection = project.getMessageBus().connect(project);
    connection.subscribe(QuarkusProjectService.TOPIC, this);
    QuarkusProjectService.getInstance(project);
  }

  @Override
  public void dispose() {
    super.dispose();
    connection.disconnect();
  }

  /**
   * Send the notification qute/dataModelChanged with the project Uris to
   * refresh data model used in Qute Template.
   *
   * @param uris the project uris where the data model must be refreshed.
   */
  private void notifyDataModelChanged(Set<String> uris) {
    QuteLanguageServerAPI server = (QuteLanguageServerAPI) getLanguageServer();
    if (server != null) {
      JavaDataModelChangeEvent event = new JavaDataModelChangeEvent();
      event.setProjectURIs(uris);
      server.dataModelChanged(event);
    }
  }

  @Override
  public void libraryUpdated(Library library) {
    if (isDisposed()) {
      // The language client has been disposed, ignore the changed of library
      return;
    }
    Set<String> uris = new HashSet<>();
    uris.add(PsiQuteProjectUtils.getProjectURI(getProject()));
    notifyDataModelChanged(uris);
  }

  @Override
  public void sourceUpdated(List<Pair<Module, VirtualFile>> sources) {
    if (isDisposed()) {
      // The language client has been disposed, ignore the changed of Java source files
      return;
    }
    Set<String> uris = sources.stream()
            .map(pair -> pair.getLeft())
            .map(module -> PsiQuteProjectUtils.getProjectURI(module))
            .collect(Collectors.toSet());
    if (!uris.isEmpty()) {
      notifyDataModelChanged(uris);
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

  @Override
  public CompletableFuture<WorkspaceEdit> generateMissingJavaMember(GenerateMissingJavaMemberParams params) {
    return runAsBackground("generateMissingJavaMember", monitor -> ReadAction.compute(() -> QuteSupportForTemplate.getInstance()
            .generateMissingJavaMember(params, PsiUtilsLSImpl.getInstance(getProject()), monitor)));
  }

  @Override
  public CompletableFuture<String> getJavadoc(QuteJavadocParams params) {
    return runAsBackground("getJavadoc", monitor -> QuteSupportForTemplate.getInstance()
            .getJavadoc(params, PsiUtilsLSImpl.getInstance(getProject()), monitor));
  }
}
