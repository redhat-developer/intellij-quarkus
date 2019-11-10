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

import com.github.gtache.lsp.client.languageserver.ServerStatus;
import com.github.gtache.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import com.github.gtache.lsp.contributors.icon.LSPDefaultIconProvider;
import com.github.gtache.lsp.contributors.icon.LSPIconProvider;
import com.intellij.openapi.util.IconLoader;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.SymbolKind;
import scala.Tuple2;
import scala.collection.immutable.Map;

import javax.swing.Icon;

public class IconProvider implements LSPIconProvider {
    @Override
    public Icon getCompletionIcon(CompletionItemKind kind) {
        return LSPDefaultIconProvider.getCompletionIcon(kind);
    }

    @Override
    public Map<ServerStatus, Icon> getStatusIcons() {
        Map<ServerStatus, Icon> icons = LSPDefaultIconProvider.getStatusIcons();
        return icons.$plus(new Tuple2<>(ServerStatus.STARTED, IconLoader.findIcon("/quarkus_icon_rgb_32px_default.png", IconProvider.class)));
    }

    @Override
    public Icon getSymbolIcon(SymbolKind kind) {
        return LSPDefaultIconProvider.getSymbolIcon(kind);
    }

    @Override
    public boolean isSpecificFor(LanguageServerDefinition serverDefinition) {
        return serverDefinition instanceof QuarkusLanguageServerDefinition;
    }
}
