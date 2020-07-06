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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.impl.libraries.LibraryTableBase;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
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
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;


public class QuarkusLanguageClient extends LanguageClientImpl implements MicroProfileLanguageClientAPI, LibraryTable.Listener, BulkFileListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusLanguageClient.class);

  private long lastModification = (-1);

  private final MessageBusConnection connection;

  public QuarkusLanguageClient(Project project) {
    super(project);
    LibraryTablesRegistrar.getInstance().getLibraryTable(project).addListener(this, project);
    connection = ApplicationManager.getApplication().getMessageBus().connect(project);
    connection.subscribe(VirtualFileManager.VFS_CHANGES, this);
  }

  private void sendPropertiesChangeEvent(MicroProfilePropertiesScope scope, Set<String> uris) {
    MicroProfilePropertiesChangeEvent event = new MicroProfilePropertiesChangeEvent();
    event.setType(Collections.singletonList(scope));
    event.setProjectURIs(uris);
    ((MicroProfileLanguageServerAPI)getLanguageServer()).propertiesChanged(event);
  }

  private void handleLibraryUpdate(Library library) {
    if (library.getTable() instanceof LibraryTableBase) {
      long modif = ((LibraryTableBase)library.getTable()).getStateModificationCount();
      if (modif > lastModification) {
        sendPropertiesChangeEvent(MicroProfilePropertiesScope.dependencies, QuarkusModuleUtil.getModulesURIs(getProject()));
        lastModification = modif;
      }
    }
  }


  @Override
  public void afterLibraryAdded(@NotNull Library newLibrary) {
    handleLibraryUpdate(newLibrary);
  }

  @Override
  public void afterLibraryRemoved(@NotNull Library library) {
    handleLibraryUpdate(library);
  }

  @Override
  public void after(@NotNull List<? extends VFileEvent> events) {
    Set<String> uris = new HashSet<>();
    events.forEach(event -> filter(event, uris));
    if (!uris.isEmpty()) {
      sendPropertiesChangeEvent(MicroProfilePropertiesScope.sources, uris);
    }
  }

  private void filter(VFileEvent event, Set<String> uris) {
    VirtualFile file = event.getFile();
    if (file != null && file.exists() && "java".equalsIgnoreCase(file.getExtension())) {
      Module module = ProjectFileIndex.getInstance(getProject()).getModuleForFile(file);
      if (module != null && (event instanceof VFileCreateEvent || event instanceof VFileContentChangeEvent || event instanceof VFileDeleteEvent)) {
        uris.add(PsiUtilsImpl.getProjectURI(module));
      }
    }
  }

  @Override
  public CompletableFuture<MicroProfileProjectInfo> getProjectInfo(MicroProfileProjectInfoParams params) {
    return CompletableFuture.supplyAsync(() -> PropertiesManager.getInstance().getMicroProfileProjectInfo(params, PsiUtilsImpl.getInstance()));
  }

  @Override
  public CompletableFuture<Hover> getJavaHover(MicroProfileJavaHoverParams javaParams) {
    return CompletableFuture.supplyAsync(() -> PropertiesManagerForJava.getInstance().hover(javaParams, PsiUtilsImpl.getInstance()));
  }

  @Override
  public CompletableFuture<List<PublishDiagnosticsParams>> getJavaDiagnostics(MicroProfileJavaDiagnosticsParams javaParams) {
    return CompletableFuture.supplyAsync(() -> PropertiesManagerForJava.getInstance().diagnostics(javaParams, PsiUtilsImpl.getInstance()));
  }

  @Override
  public CompletableFuture<Location> getPropertyDefinition(MicroProfilePropertyDefinitionParams params) {
    return CompletableFuture.supplyAsync(() -> PropertiesManager.getInstance().findPropertyLocation(params, PsiUtilsImpl.getInstance()));
  }

  @Override
  public CompletableFuture<ProjectLabelInfoEntry> getJavaProjectlabels(MicroProfileJavaProjectLabelsParams javaParams) {
    return CompletableFuture.supplyAsync(() -> ProjectLabelManager.getInstance().getProjectLabelInfo(javaParams, PsiUtilsImpl.getInstance()));
  }
}
