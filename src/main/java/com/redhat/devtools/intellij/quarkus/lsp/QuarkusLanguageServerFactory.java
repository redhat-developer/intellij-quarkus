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
package com.redhat.devtools.intellij.quarkus.lsp;

import com.intellij.openapi.project.Project;
import com.redhat.devtools.lsp4ij.LanguageServerFactory;
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl;
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures;
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4mp.ls.api.MicroProfileLanguageServerAPI;
import org.jetbrains.annotations.NotNull;

/**
 * Quarkus language server factory.
 */
public class QuarkusLanguageServerFactory implements LanguageServerFactory {

    @Override
    public StreamConnectionProvider createConnectionProvider(Project project) {
        return new QuarkusServer(project);
    }

    @Override
    public LanguageClientImpl createLanguageClient(Project project) {
        return new QuarkusLanguageClient(project);
    }

    @Override
    public Class<? extends LanguageServer> getServerInterface() {
        return MicroProfileLanguageServerAPI.class;
    }

    @Override
    public @NotNull LSPClientFeatures createClientFeatures() {
        return new MicroProfileClientFeatures();
    }
}
