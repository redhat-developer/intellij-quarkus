/*******************************************************************************
 * Copyright (c) 2023 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.qute.lsp;

import com.intellij.openapi.project.Project;
import com.redhat.devtools.lsp4ij.LanguageServerFactory;
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl;
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider;
import com.redhat.qute.ls.api.QuteLanguageServerAPI;
import org.eclipse.lsp4j.services.LanguageServer;

/**
 * Qute language server factory.
 */
public class QuteLanguageServerFactory implements LanguageServerFactory {

    @Override
    public StreamConnectionProvider createConnectionProvider(Project project) {
        return new QuteServer(project);
    }

    @Override
    public LanguageClientImpl createLanguageClient(Project project) {
        return new QuteLanguageClient(project);
    }

    @Override
    public Class<? extends LanguageServer> getServerInterface() {
        return QuteLanguageServerAPI.class;
    }
}
