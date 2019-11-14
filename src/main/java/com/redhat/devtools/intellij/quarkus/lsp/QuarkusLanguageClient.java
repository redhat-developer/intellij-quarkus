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

import com.github.gtache.lsp.client.LanguageClientImpl;
import com.redhat.devtools.intellij.quarkus.search.PSIQuarkusManager;
import com.redhat.quarkus.commons.QuarkusProjectInfo;
import com.redhat.quarkus.commons.QuarkusProjectInfoParams;
import com.redhat.quarkus.commons.QuarkusPropertyDefinitionParams;
import com.redhat.quarkus.ls.api.QuarkusLanguageClientAPI;
import org.eclipse.lsp4j.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class QuarkusLanguageClient extends LanguageClientImpl implements QuarkusLanguageClientAPI {
  private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusLanguageClient.class);

  @Override
  public CompletableFuture<QuarkusProjectInfo> getQuarkusProjectInfo(QuarkusProjectInfoParams request) {
    LOGGER.info("Project info for:" + request.getUri() + " scope=" + request.getScope());
    QuarkusProjectInfo result = new QuarkusProjectInfo();
    result.setProperties(PSIQuarkusManager.INSTANCE.getConfigItems(request));
    return CompletableFuture.completedFuture(result);
  }

  @Override
  public CompletableFuture<Location> getPropertyDefinition(QuarkusPropertyDefinitionParams quarkusPropertyDefinitionParams) {
    //TODO: implements property definition
    return CompletableFuture.completedFuture(null);
  }
}
