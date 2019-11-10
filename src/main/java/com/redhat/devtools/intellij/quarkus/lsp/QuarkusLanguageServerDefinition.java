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
import com.github.gtache.lsp.client.languageserver.serverdefinition.ExeLanguageServerDefinition;

public class QuarkusLanguageServerDefinition extends ExeLanguageServerDefinition {
  public QuarkusLanguageServerDefinition(String ext, String path, String[] args) {
    super(ext, path, args);
  }

  @Override
  public LanguageClientImpl createLanguageClient() {
    return new QuarkusLanguageClient();
  }
}
