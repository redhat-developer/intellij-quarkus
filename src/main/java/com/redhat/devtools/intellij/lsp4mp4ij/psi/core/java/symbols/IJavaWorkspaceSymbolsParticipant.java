/*******************************************************************************
 * Copyright (c) 2024 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package com.redhat.devtools.intellij.lsp4mp4ij.psi.core.java.symbols;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.redhat.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.eclipse.lsp4j.SymbolInformation;

import java.util.List;

/**
 * Represents an object that can collect workspace symbols for java projects.
 */
public interface IJavaWorkspaceSymbolsParticipant {

    ExtensionPointName<IJavaWorkspaceSymbolsParticipant> EP_NAME = ExtensionPointName.create("com.redhat.devtools.intellij.quarkus.javaWorkspaceSymbolsParticipant");

    /**
     * Fill in <code>symbols</code> with workspace symbols of the given project.
     *
     * @param project the project to collect workspace symbols from
     * @param utils   the JDT utils
     * @param symbols the list of symbols to add to
     * @param monitor the progress monitor
     */
    void collectSymbols(Module project, IPsiUtils utils, List<SymbolInformation> symbols,
                        ProgressIndicator monitor);

}