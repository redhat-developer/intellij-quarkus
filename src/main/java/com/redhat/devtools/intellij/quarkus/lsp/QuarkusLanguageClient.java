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

import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageClientImpl;
import com.redhat.devtools.intellij.quarkus.search.PsiUtils;
import com.redhat.devtools.intellij.quarkus.search.PropertiesManager;
import com.redhat.microprofile.commons.MicroProfileProjectInfo;
import com.redhat.microprofile.commons.MicroProfileProjectInfoParams;
import com.redhat.microprofile.ls.api.MicroProfileLanguageClientAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class QuarkusLanguageClient extends LanguageClientImpl implements MicroProfileLanguageClientAPI {
  private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusLanguageClient.class);

  @Override
  public CompletableFuture<MicroProfileProjectInfo> getProjectInfo(MicroProfileProjectInfoParams params) {
    return CompletableFuture.completedFuture(PropertiesManager.getInstance().getMicroProfileProjectInfo(params, PsiUtils.getInstance()));
  }
}
