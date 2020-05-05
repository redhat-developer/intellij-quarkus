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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.ProjectRootUtil;
import com.intellij.openapi.roots.impl.libraries.LibraryTableBase;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.redhat.devtools.intellij.quarkus.QuarkusModuleUtil;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageClientImpl;
import com.redhat.devtools.intellij.quarkus.search.PsiUtilsImpl;
import com.redhat.devtools.intellij.quarkus.search.PropertiesManager;
import com.redhat.devtools.intellij.quarkus.search.core.PropertiesManagerForJava;
import com.redhat.microprofile.commons.MicroProfileJavaDiagnosticsParams;
import com.redhat.microprofile.commons.MicroProfileJavaHoverParams;
import com.redhat.microprofile.commons.MicroProfileProjectInfo;
import com.redhat.microprofile.commons.MicroProfileProjectInfoParams;
import com.redhat.microprofile.commons.MicroProfilePropertiesChangeEvent;
import com.redhat.microprofile.commons.MicroProfilePropertiesScope;
import com.redhat.microprofile.ls.api.MicroProfileLanguageClientAPI;
import com.redhat.microprofile.ls.api.MicroProfileLanguageServerAPI;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;


public class QuarkusLanguageClient extends LanguageClientImpl implements MicroProfileLanguageClientAPI, LibraryTable.Listener {
  private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusLanguageClient.class);

  private long lastModification = (-1);

  public QuarkusLanguageClient(Project project) {
    super(project);
    LibraryTablesRegistrar.getInstance().getLibraryTable(project).addListener(this, project);
  }

  private void handleLibraryUpdate(Library library) {
    System.out.println(library + " table:" + library.getTable().getClass() + " class:" + library.getClass());
    if (library.getTable() instanceof LibraryTableBase) {
      long modif = ((LibraryTableBase)library.getTable()).getStateModificationCount();
      if (modif > lastModification) {
        MicroProfilePropertiesChangeEvent event = new MicroProfilePropertiesChangeEvent();
        event.setType(Collections.singletonList(MicroProfilePropertiesScope.dependencies));
        event.setProjectURIs(QuarkusModuleUtil.getModulesURIs(getProject()));
        ((MicroProfileLanguageServerAPI)getLanguageServer()).propertiesChanged(event);
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
  public CompletableFuture<MicroProfileProjectInfo> getProjectInfo(MicroProfileProjectInfoParams params) {
    return CompletableFuture.completedFuture(PropertiesManager.getInstance().getMicroProfileProjectInfo(params, PsiUtilsImpl.getInstance()));
  }

  @Override
  public CompletableFuture<Hover> getJavaHover(MicroProfileJavaHoverParams javaParams) {
    return CompletableFuture.completedFuture(PropertiesManagerForJava.getInstance().hover(javaParams, PsiUtilsImpl.getInstance()));
  }

  @Override
  public CompletableFuture<List<PublishDiagnosticsParams>> getJavaDiagnostics(MicroProfileJavaDiagnosticsParams javaParams) {
    return CompletableFuture.completedFuture(PropertiesManagerForJava.getInstance().diagnostics(javaParams, PsiUtilsImpl.getInstance()));
  }
}
