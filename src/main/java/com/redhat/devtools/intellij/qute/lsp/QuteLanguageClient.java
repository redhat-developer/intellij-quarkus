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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import com.redhat.devtools.intellij.lsp4ij.client.IndexAwareLanguageClient;
import com.redhat.devtools.intellij.lsp4mp4ij.classpath.ClasspathResourceChangedManager;
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
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class QuteLanguageClient extends IndexAwareLanguageClient implements QuteLanguageClientAPI, ClasspathResourceChangedManager.Listener {
  private static final Logger LOGGER = LoggerFactory.getLogger(QuteLanguageClient.class);

  private final MessageBusConnection connection;

  public QuteLanguageClient(Project project) {
    super(project);
    connection = project.getMessageBus().connect(project);
    connection.subscribe(ClasspathResourceChangedManager.TOPIC, this);
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
  private void notifyQuteDataModelChanged(Set<String> uris) {
    QuteLanguageServerAPI server = (QuteLanguageServerAPI) getLanguageServer();
    if (server != null) {
      JavaDataModelChangeEvent event = new JavaDataModelChangeEvent();
      event.setProjectURIs(uris);
      server.dataModelChanged(event);
    }
  }

  @Override
  public void librariesChanged() {
    if (isDisposed()) {
      // The language client has been disposed, ignore changes in libraries
      return;
    }
    Set<String> uris = new HashSet<>();
    uris.add(PsiQuteProjectUtils.getProjectURI(getProject()));
    notifyQuteDataModelChanged(uris);
  }

  @Override
  public void sourceFilesChanged(Set<Pair<VirtualFile, Module>> sources) {
    if (isDisposed()) {
      // The language client has been disposed, ignore changes in Java source files
      return;
    }
    Set<String> uris = sources.stream()
            // qute/dataModelChanged must be sent only if there are some Java files which are changed
            .filter(pair -> PsiMicroProfileProjectManager.isJavaFile(pair.getFirst()))
            .map(pair -> pair.getSecond())
            .map(module -> PsiUtilsLSImpl.getProjectURI(module))
            .collect(Collectors.toSet());
    if (!uris.isEmpty()) {
      notifyQuteDataModelChanged(uris);
    }
  }

  @Override
  public CompletableFuture<ProjectInfo> getProjectInfo(QuteProjectParams params) {
    return runAsBackground("getProjectInfo", monitor -> QuteSupportForTemplate.getInstance().getProjectInfo(params, PsiUtilsLSImpl.getInstance(getProject()), monitor));
  }

  @Override
  public CompletableFuture<DataModelProject<DataModelTemplate<DataModelParameter>>> getDataModelProject(
          QuteDataModelProjectParams params) {
    return runAsBackground("getDataModel", monitor -> QuteSupportForTemplate.getInstance().getDataModelProject(params, PsiUtilsLSImpl.getInstance(getProject()),
            monitor));
  }

  @Override
  public CompletableFuture<List<JavaTypeInfo>> getJavaTypes(QuteJavaTypesParams params) {
    return runAsBackground("getJavaTypes", monitor -> QuteSupportForTemplate.getInstance().getJavaTypes(params, PsiUtilsLSImpl.getInstance(getProject()),
            monitor));
  }

  @Override
  public CompletableFuture<ResolvedJavaTypeInfo> getResolvedJavaType(QuteResolvedJavaTypeParams params) {
    return runAsBackground("getResolvedJavaType", monitor -> QuteSupportForTemplate.getInstance().getResolvedJavaType(params, PsiUtilsLSImpl.getInstance(getProject()),
            monitor));
  }

  @Override
  public CompletableFuture<Location> getJavaDefinition(QuteJavaDefinitionParams params) {
    return runAsBackground("getJavaDefinition", monitor -> QuteSupportForTemplate.getInstance().getJavaDefinition(params, PsiUtilsLSImpl.getInstance(getProject()),
            monitor));
  }

  @Override
  public CompletableFuture<List<? extends CodeLens>> getJavaCodelens(QuteJavaCodeLensParams javaParams) {
    return runAsBackground("getJavaCodelens", monitor -> QuteSupportForJava.getInstance().codeLens(javaParams, PsiUtilsLSImpl.getInstance(getProject()),
            monitor));
  }

  @Override
  public CompletableFuture<List<PublishDiagnosticsParams>> getJavaDiagnostics(QuteJavaDiagnosticsParams javaParams) {
    return runAsBackground("getJavaDiagnostics", monitor -> QuteSupportForJava.getInstance().diagnostics(javaParams, PsiUtilsLSImpl.getInstance(getProject()),
            monitor));
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
    return runAsBackground("generateMissingJavaMember", monitor -> QuteSupportForTemplate.getInstance()
            .generateMissingJavaMember(params, PsiUtilsLSImpl.getInstance(getProject()), monitor));
  }

  @Override
  public CompletableFuture<String> getJavadoc(QuteJavadocParams params) {
    return runAsBackground("getJavadoc", monitor -> QuteSupportForTemplate.getInstance()
            .getJavadoc(params, PsiUtilsLSImpl.getInstance(getProject()), monitor));
  }
}
